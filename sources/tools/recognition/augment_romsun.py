#!/usr/bin/env python3
"""Build a deterministic ROMSUN blood-pressure image augmentation set.

This tool expands capture-condition diversity while keeping the three reading
labels unchanged.  It deliberately does *not* synthesize unseen digits.  The
eight known source photos contain only two readings and their measurement
digits cover 0, 1, 2, 3, 7 and 8.  Reconstructing 4, 5, 6 or 9 by erasing
segments from an observed 8 would require verified digit boxes, a rectified
screen, and a trustworthy background model.  Without those annotations it can
create visually plausible but device-inaccurate training data, so it is not
appropriate for a 99% accuracy target.

Requirements:
    Python 3.10+, numpy, opencv-python

Examples:
    python augment_romsun.py --output out --input photo1.jpg --input photo2.jpg

    python augment_romsun.py --output out \
        --sample "photo.jpg|123|80|82" --variants-per-input 64

    python augment_romsun.py --output out --input-list samples.json

Input-list formats:
    JSON:  [{"path": "photo.jpg", "systolic": 123,
             "diastolic": 80, "heart_rate": 82}]
    JSONL: one JSON object per line
    CSV:   path,systolic,diastolic,heart_rate[,screen_quad]
    TXT:   path|systolic|diastolic|heart_rate, one sample per line

The optional screen_quad is [[x, y], ...] in source-image coordinates ordered
clockwise.  When present it is transformed along with the image and written to
manifest.jsonl.  All derivatives of one source share group_id; downstream
splits must group on that field to avoid train/evaluation leakage.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable, Sequence

try:
    import cv2
    import numpy as np
except ImportError as exc:  # pragma: no cover - exercised only on missing setup
    raise SystemExit(
        "Missing dependency. Install numpy and opencv-python before running "
        "augment_romsun.py."
    ) from exc


TOOL_VERSION = "1.0.0"
DEFAULT_SEED = 20260713
PROFILES = (
    "rotation",
    "perspective",
    "scale",
    "blur",
    "low_light",
    "glare",
    "noise",
    "mixed",
)

# Labels were manually verified from the eight user-provided ROMSUN photos.
# --input is intentionally restricted to these names; any other photo must
# provide explicit labels through --sample or --input-list.
KNOWN_LABELS: dict[str, tuple[int, int, int]] = {
    "7787f65317aa328fad0e9cc0ac51d9d.jpg": (123, 78, 81),
    "66bbc9225f38ef9f158fa7d59fed79b.jpg": (123, 78, 81),
    "4b7960f5ea5efe8cba4d03d5250bcac.jpg": (123, 80, 82),
    "d504344a68d4e71e95ed9a64b7dc4a8.jpg": (123, 80, 82),
    "db983b5c705d43d7161a8b4f66949bd.jpg": (123, 80, 82),
    "6c9386f276c0618907c102f41cab0cc.jpg": (123, 80, 82),
    "437916e534e6d6666bbc60619b476e5.jpg": (123, 80, 82),
    "4587b109aac418ad4623f7d8513264f.jpg": (123, 80, 82),
}


@dataclass(frozen=True)
class SourceSample:
    path: Path
    systolic: int
    diastolic: int
    heart_rate: int
    screen_quad: tuple[tuple[float, float], ...] | None = None

    @property
    def labels(self) -> dict[str, int]:
        return {
            "systolic": self.systolic,
            "diastolic": self.diastolic,
            "heart_rate": self.heart_rate,
        }


def _parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Create deterministic ROMSUN image variants and a leakage-safe "
            "manifest. Labels are never changed."
        )
    )
    parser.add_argument(
        "--input",
        action="append",
        default=[],
        metavar="PATH",
        help="Known one of the eight source photos; label is inferred by filename.",
    )
    parser.add_argument(
        "--sample",
        action="append",
        default=[],
        metavar="PATH|SYS|DIA|HR",
        help="Explicitly labelled source photo. May be repeated.",
    )
    parser.add_argument(
        "--input-list",
        action="append",
        default=[],
        type=Path,
        metavar="PATH",
        help="JSON, JSONL, CSV or TXT source list. May be repeated.",
    )
    parser.add_argument("--output", required=True, type=Path, help="Output directory.")
    parser.add_argument(
        "--variants-per-input",
        type=int,
        default=64,
        help="Augmented variants per source, excluding the reference image (default: 64).",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=DEFAULT_SEED,
        help=f"Global deterministic seed (default: {DEFAULT_SEED}).",
    )
    parser.add_argument(
        "--max-side",
        type=int,
        default=1280,
        help="Resize longest side before augmentation; 0 keeps source size (default: 1280).",
    )
    parser.add_argument(
        "--jpeg-quality",
        type=int,
        default=94,
        help="JPEG quality from 70 to 100 (default: 94).",
    )
    parser.add_argument(
        "--without-reference",
        action="store_true",
        help="Do not emit the unaugmented, size-normalized reference image.",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Overwrite matching outputs in a non-empty output directory.",
    )
    args = parser.parse_args(argv)

    if args.variants_per_input < 1:
        parser.error("--variants-per-input must be at least 1")
    if args.max_side != 0 and args.max_side < 256:
        parser.error("--max-side must be 0 or at least 256")
    if not 70 <= args.jpeg_quality <= 100:
        parser.error("--jpeg-quality must be between 70 and 100")
    if not (args.input or args.sample or args.input_list):
        parser.error("provide at least one --input, --sample or --input-list")
    return args


def _resolve_path(raw_path: str, base_dir: Path) -> Path:
    path = Path(os.path.expandvars(raw_path)).expanduser()
    if not path.is_absolute():
        path = base_dir / path
    return path.resolve()


def _parse_quad(value: Any) -> tuple[tuple[float, float], ...] | None:
    if value in (None, ""):
        return None
    if isinstance(value, str):
        try:
            value = json.loads(value)
        except json.JSONDecodeError as exc:
            raise ValueError("screen_quad must be valid JSON") from exc
    if not isinstance(value, list) or len(value) != 4:
        raise ValueError("screen_quad must contain four [x, y] points")
    points: list[tuple[float, float]] = []
    for point in value:
        if not isinstance(point, (list, tuple)) or len(point) != 2:
            raise ValueError("each screen_quad point must be [x, y]")
        x, y = float(point[0]), float(point[1])
        if not math.isfinite(x) or not math.isfinite(y):
            raise ValueError("screen_quad coordinates must be finite")
        points.append((x, y))
    return tuple(points)


def _validate_labels(systolic: int, diastolic: int, heart_rate: int) -> None:
    if systolic not in range(60, 251):
        raise ValueError(f"systolic label out of range: {systolic}")
    if diastolic not in range(40, 151):
        raise ValueError(f"diastolic label out of range: {diastolic}")
    if heart_rate not in range(30, 201):
        raise ValueError(f"heart_rate label out of range: {heart_rate}")
    if systolic <= diastolic:
        raise ValueError("systolic label must be greater than diastolic")


def _sample_from_values(
    raw_path: str,
    systolic: Any,
    diastolic: Any,
    heart_rate: Any,
    base_dir: Path,
    screen_quad: Any = None,
) -> SourceSample:
    try:
        sys_value = int(systolic)
        dia_value = int(diastolic)
        hr_value = int(heart_rate)
    except (TypeError, ValueError) as exc:
        raise ValueError(f"invalid labels for {raw_path}") from exc
    _validate_labels(sys_value, dia_value, hr_value)
    path = _resolve_path(raw_path, base_dir)
    if not path.is_file():
        raise ValueError(f"input image does not exist: {path}")
    return SourceSample(
        path=path,
        systolic=sys_value,
        diastolic=dia_value,
        heart_rate=hr_value,
        screen_quad=_parse_quad(screen_quad),
    )


def _known_sample(raw_path: str, base_dir: Path) -> SourceSample:
    path = _resolve_path(raw_path, base_dir)
    labels = KNOWN_LABELS.get(path.name.lower())
    if labels is None:
        raise ValueError(
            f"no built-in label for {path.name}; use "
            f"--sample \"{path}|SYS|DIA|HR\" or --input-list"
        )
    return _sample_from_values(str(path), *labels, base_dir=Path.cwd())


def _mapping_to_sample(record: dict[str, Any], base_dir: Path) -> SourceSample:
    raw_path = record.get("path") or record.get("image")
    if not raw_path:
        raise ValueError("input-list record is missing path")
    labels = record.get("labels")
    if isinstance(labels, dict):
        values = labels
    else:
        values = record
    return _sample_from_values(
        str(raw_path),
        values.get("systolic"),
        values.get("diastolic"),
        values.get("heart_rate", values.get("heartRate")),
        base_dir,
        record.get("screen_quad", record.get("screenQuad")),
    )


def _load_input_list(path: Path) -> list[SourceSample]:
    path = path.resolve()
    if not path.is_file():
        raise ValueError(f"input list does not exist: {path}")
    suffix = path.suffix.lower()
    base_dir = path.parent
    samples: list[SourceSample] = []

    if suffix == ".json":
        payload = json.loads(path.read_text(encoding="utf-8-sig"))
        if isinstance(payload, dict):
            payload = payload.get("samples")
        if not isinstance(payload, list):
            raise ValueError(f"JSON input list must be an array: {path}")
        samples.extend(_mapping_to_sample(record, base_dir) for record in payload)
    elif suffix == ".jsonl":
        for line_number, line in enumerate(
            path.read_text(encoding="utf-8-sig").splitlines(), start=1
        ):
            if not line.strip():
                continue
            try:
                record = json.loads(line)
                samples.append(_mapping_to_sample(record, base_dir))
            except (json.JSONDecodeError, ValueError) as exc:
                raise ValueError(f"{path}:{line_number}: {exc}") from exc
    elif suffix == ".csv":
        with path.open("r", encoding="utf-8-sig", newline="") as handle:
            reader = csv.DictReader(handle)
            for line_number, record in enumerate(reader, start=2):
                try:
                    samples.append(_mapping_to_sample(dict(record), base_dir))
                except ValueError as exc:
                    raise ValueError(f"{path}:{line_number}: {exc}") from exc
    elif suffix in {".txt", ".list"}:
        for line_number, line in enumerate(
            path.read_text(encoding="utf-8-sig").splitlines(), start=1
        ):
            value = line.strip()
            if not value or value.startswith("#"):
                continue
            try:
                if "|" in value:
                    raw_path, sys_value, dia_value, hr_value = value.rsplit("|", 3)
                    samples.append(
                        _sample_from_values(
                            raw_path, sys_value, dia_value, hr_value, base_dir
                        )
                    )
                else:
                    samples.append(_known_sample(value, base_dir))
            except ValueError as exc:
                raise ValueError(f"{path}:{line_number}: {exc}") from exc
    else:
        raise ValueError(f"unsupported input-list format: {path.suffix}")
    return samples


def _collect_samples(args: argparse.Namespace) -> list[SourceSample]:
    samples: list[SourceSample] = []
    samples.extend(_known_sample(value, Path.cwd()) for value in args.input)
    for value in args.sample:
        try:
            raw_path, sys_value, dia_value, hr_value = value.rsplit("|", 3)
        except ValueError as exc:
            raise ValueError(
                f"invalid --sample {value!r}; expected PATH|SYS|DIA|HR"
            ) from exc
        samples.append(
            _sample_from_values(
                raw_path, sys_value, dia_value, hr_value, Path.cwd()
            )
        )
    for list_path in args.input_list:
        samples.extend(_load_input_list(list_path))

    unique: dict[str, SourceSample] = {}
    for sample in samples:
        key = os.path.normcase(str(sample.path))
        previous = unique.get(key)
        if previous is not None and previous != sample:
            raise ValueError(f"conflicting duplicate labels or quad for {sample.path}")
        unique[key] = sample
    return sorted(unique.values(), key=lambda sample: os.path.normcase(str(sample.path)))


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _read_image(path: Path) -> np.ndarray:
    # imdecode/fromfile works with non-ASCII Windows paths unlike some imread builds.
    encoded = np.fromfile(str(path), dtype=np.uint8)
    image = cv2.imdecode(encoded, cv2.IMREAD_COLOR)
    if image is None or image.ndim != 3 or image.shape[2] != 3:
        raise ValueError(f"cannot decode image: {path}")
    return image


def _write_jpeg(path: Path, image: np.ndarray, quality: int) -> str:
    ok, encoded = cv2.imencode(
        ".jpg", image, [int(cv2.IMWRITE_JPEG_QUALITY), int(quality)]
    )
    if not ok:
        raise ValueError(f"cannot encode image: {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = path.with_name(f".{path.name}.tmp")
    temp_path.write_bytes(encoded.tobytes())
    temp_path.replace(path)
    return hashlib.sha256(encoded.tobytes()).hexdigest()


def _resize_to_max_side(
    image: np.ndarray, max_side: int
) -> tuple[np.ndarray, np.ndarray]:
    height, width = image.shape[:2]
    if max_side == 0 or max(height, width) <= max_side:
        return image, np.eye(3, dtype=np.float32)
    scale = max_side / float(max(height, width))
    new_width = max(1, int(round(width * scale)))
    new_height = max(1, int(round(height * scale)))
    resized = cv2.resize(image, (new_width, new_height), interpolation=cv2.INTER_AREA)
    transform = np.array(
        [[new_width / width, 0.0, 0.0], [0.0, new_height / height, 0.0], [0.0, 0.0, 1.0]],
        dtype=np.float32,
    )
    return resized, transform


def _stable_seed(global_seed: int, source_sha: str, variant_index: int) -> int:
    payload = f"{global_seed}\0{source_sha}\0{variant_index}".encode("ascii")
    return int.from_bytes(hashlib.sha256(payload).digest()[:8], "big", signed=False)


def _perspective_matrix(
    width: int, height: int, fraction: float, rng: np.random.Generator
) -> tuple[np.ndarray, list[list[float]]]:
    source = np.array(
        [[0.0, 0.0], [width - 1.0, 0.0], [width - 1.0, height - 1.0], [0.0, height - 1.0]],
        dtype=np.float32,
    )
    x_jitter = width * fraction
    y_jitter = height * fraction
    destination = source + np.column_stack(
        (
            rng.uniform(-x_jitter, x_jitter, size=4),
            rng.uniform(-y_jitter, y_jitter, size=4),
        )
    ).astype(np.float32)
    matrix = cv2.getPerspectiveTransform(source, destination)
    return matrix.astype(np.float32), destination.round(4).tolist()


def _affine_matrix(
    width: int,
    height: int,
    angle: float,
    scale: float,
    translate_x: float,
    translate_y: float,
) -> np.ndarray:
    affine = cv2.getRotationMatrix2D(
        ((width - 1.0) / 2.0, (height - 1.0) / 2.0), angle, scale
    ).astype(np.float32)
    affine[0, 2] += translate_x
    affine[1, 2] += translate_y
    return np.vstack((affine, np.array([0.0, 0.0, 1.0], dtype=np.float32)))


def _apply_blur(image: np.ndarray, sigma: float) -> np.ndarray:
    return cv2.GaussianBlur(image, (0, 0), sigmaX=sigma, sigmaY=sigma)


def _apply_low_light(
    image: np.ndarray, gamma: float, gain: float, vignette: float
) -> np.ndarray:
    normalized = image.astype(np.float32) / 255.0
    darkened = np.power(normalized, gamma) * gain
    height, width = image.shape[:2]
    x = np.linspace(-1.0, 1.0, width, dtype=np.float32)
    y = np.linspace(-1.0, 1.0, height, dtype=np.float32)
    radius_squared = y[:, None] ** 2 + x[None, :] ** 2
    mask = 1.0 - vignette * np.clip(radius_squared / 2.0, 0.0, 1.0)
    darkened *= mask[:, :, None]
    return np.clip(darkened * 255.0, 0.0, 255.0).astype(np.uint8)


def _apply_glare(
    image: np.ndarray,
    center_x: float,
    center_y: float,
    axis_x: float,
    axis_y: float,
    angle: float,
    alpha: float,
) -> np.ndarray:
    height, width = image.shape[:2]
    mask = np.zeros((height, width), dtype=np.float32)
    center = (int(round(center_x * width)), int(round(center_y * height)))
    axes = (
        max(2, int(round(axis_x * width))),
        max(2, int(round(axis_y * height))),
    )
    cv2.ellipse(mask, center, axes, angle, 0.0, 360.0, 1.0, thickness=-1)
    softness = max(3.0, min(axes) * 0.42)
    mask = cv2.GaussianBlur(mask, (0, 0), sigmaX=softness, sigmaY=softness)
    mask = np.clip(mask * alpha, 0.0, 0.35)[:, :, None]
    # Slightly warm white resembles indoor reflections on the glossy screen.
    glare_color = np.array([238.0, 248.0, 255.0], dtype=np.float32)
    result = image.astype(np.float32) * (1.0 - mask) + glare_color * mask
    return np.clip(result, 0.0, 255.0).astype(np.uint8)


def _apply_noise(
    image: np.ndarray, sigma: float, rng: np.random.Generator
) -> np.ndarray:
    noise = rng.normal(0.0, sigma, size=image.shape).astype(np.float32)
    result = image.astype(np.float32) + noise
    return np.clip(result, 0.0, 255.0).astype(np.uint8)


def _augment(
    source: np.ndarray, profile: str, rng: np.random.Generator
) -> tuple[np.ndarray, np.ndarray, dict[str, Any]]:
    height, width = source.shape[:2]
    angle = 0.0
    scale = 1.0
    translate_x = 0.0
    translate_y = 0.0
    perspective_fraction = 0.0
    blur_sigma = 0.0
    low_light: tuple[float, float, float] | None = None
    glare: tuple[float, float, float, float, float, float] | None = None
    noise_sigma = 0.0

    if profile == "rotation":
        angle = float(rng.choice((-1.0, 1.0)) * rng.uniform(2.0, 14.0))
    elif profile == "perspective":
        perspective_fraction = float(rng.uniform(0.015, 0.065))
    elif profile == "scale":
        scale = float(rng.uniform(0.84, 1.14))
        translate_x = float(rng.uniform(-0.035, 0.035) * width)
        translate_y = float(rng.uniform(-0.035, 0.035) * height)
    elif profile == "blur":
        blur_sigma = float(rng.uniform(0.45, 2.0))
    elif profile == "low_light":
        low_light = (
            float(rng.uniform(1.15, 2.0)),
            float(rng.uniform(0.48, 0.88)),
            float(rng.uniform(0.08, 0.35)),
        )
    elif profile == "glare":
        glare = (
            float(rng.uniform(0.18, 0.82)),
            float(rng.uniform(0.20, 0.82)),
            float(rng.uniform(0.10, 0.28)),
            float(rng.uniform(0.025, 0.12)),
            float(rng.uniform(-70.0, 70.0)),
            float(rng.uniform(0.08, 0.28)),
        )
    elif profile == "noise":
        noise_sigma = float(rng.uniform(3.0, 14.0))
    elif profile == "mixed":
        angle = float(rng.uniform(-8.0, 8.0))
        scale = float(rng.uniform(0.90, 1.08))
        translate_x = float(rng.uniform(-0.02, 0.02) * width)
        translate_y = float(rng.uniform(-0.02, 0.02) * height)
        perspective_fraction = float(rng.uniform(0.008, 0.035))
        blur_sigma = float(rng.uniform(0.20, 1.10))
        low_light = (
            float(rng.uniform(1.05, 1.42)),
            float(rng.uniform(0.72, 0.96)),
            float(rng.uniform(0.02, 0.16)),
        )
        glare = (
            float(rng.uniform(0.18, 0.82)),
            float(rng.uniform(0.20, 0.82)),
            float(rng.uniform(0.08, 0.22)),
            float(rng.uniform(0.02, 0.09)),
            float(rng.uniform(-70.0, 70.0)),
            float(rng.uniform(0.04, 0.15)),
        )
        noise_sigma = float(rng.uniform(1.5, 7.0))
    else:
        raise ValueError(f"unknown profile: {profile}")

    if perspective_fraction:
        perspective, corners = _perspective_matrix(
            width, height, perspective_fraction, rng
        )
    else:
        perspective = np.eye(3, dtype=np.float32)
        corners = None
    affine = _affine_matrix(
        width, height, angle, scale, translate_x, translate_y
    )
    geometry = affine @ perspective
    result = cv2.warpPerspective(
        source,
        geometry,
        (width, height),
        flags=cv2.INTER_LINEAR,
        borderMode=cv2.BORDER_REFLECT_101,
    )

    if blur_sigma:
        result = _apply_blur(result, blur_sigma)
    if low_light is not None:
        result = _apply_low_light(result, *low_light)
    if glare is not None:
        result = _apply_glare(result, *glare)
    if noise_sigma:
        result = _apply_noise(result, noise_sigma, rng)

    params: dict[str, Any] = {
        "rotation_degrees": round(angle, 6),
        "scale": round(scale, 6),
        "translate_pixels": [round(translate_x, 4), round(translate_y, 4)],
        "perspective_fraction": round(perspective_fraction, 6),
        "blur_sigma": round(blur_sigma, 6),
        "noise_sigma": round(noise_sigma, 6),
    }
    if corners is not None:
        params["perspective_corners"] = corners
    if low_light is not None:
        params["low_light"] = {
            "gamma": round(low_light[0], 6),
            "gain": round(low_light[1], 6),
            "vignette": round(low_light[2], 6),
        }
    if glare is not None:
        params["glare"] = {
            "center_fraction": [round(glare[0], 6), round(glare[1], 6)],
            "axes_fraction": [round(glare[2], 6), round(glare[3], 6)],
            "angle_degrees": round(glare[4], 6),
            "alpha": round(glare[5], 6),
        }
    return result, geometry.astype(np.float32), params


def _transform_quad(
    quad: tuple[tuple[float, float], ...] | None, transform: np.ndarray
) -> list[list[float]] | None:
    if quad is None:
        return None
    points = np.array(quad, dtype=np.float32).reshape(1, 4, 2)
    transformed = cv2.perspectiveTransform(points, transform).reshape(4, 2)
    return [[round(float(x), 4), round(float(y), 4)] for x, y in transformed]


def _make_record(
    *,
    relative_image: Path,
    image_sha: str,
    width: int,
    height: int,
    source_id: str,
    source_path: Path,
    source_sha: str,
    labels: dict[str, int],
    profile: str,
    variant_index: int | None,
    variant_seed: int | None,
    transforms: dict[str, Any],
    screen_quad: list[list[float]] | None,
) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "image": relative_image.as_posix(),
        "image_sha256": image_sha,
        "width": width,
        "height": height,
        "source_id": source_id,
        "source_path": str(source_path),
        "source_sha256": source_sha,
        "group_id": source_id,
        "labels": labels,
        "profile": profile,
        "variant_index": variant_index,
        "variant_seed": variant_seed,
        "transforms": transforms,
        "screen_quad": screen_quad,
        "synthetic_digits": False,
    }


def _write_json(path: Path, value: Any) -> None:
    encoded = (json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n").encode(
        "utf-8"
    )
    temp_path = path.with_name(f".{path.name}.tmp")
    temp_path.write_bytes(encoded)
    temp_path.replace(path)


def _write_jsonl(path: Path, records: Iterable[dict[str, Any]]) -> None:
    lines = [
        json.dumps(record, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
        for record in records
    ]
    encoded = (("\n".join(lines) + "\n") if lines else "").encode("utf-8")
    temp_path = path.with_name(f".{path.name}.tmp")
    temp_path.write_bytes(encoded)
    temp_path.replace(path)


def _generate(args: argparse.Namespace, samples: list[SourceSample]) -> dict[str, Any]:
    output_dir = args.output.resolve()
    if output_dir.exists() and any(output_dir.iterdir()) and not args.overwrite:
        raise ValueError(
            f"output directory is not empty: {output_dir}; use --overwrite to replace matching files"
        )
    output_dir.mkdir(parents=True, exist_ok=True)

    records: list[dict[str, Any]] = []
    source_summaries: list[dict[str, Any]] = []
    for source_number, sample in enumerate(samples, start=1):
        source_sha = _sha256_file(sample.path)
        source_id = f"src_{source_number:02d}_{sample.path.stem}_{source_sha[:10]}"
        image = _read_image(sample.path)
        original_height, original_width = image.shape[:2]
        base, base_transform = _resize_to_max_side(image, args.max_side)
        height, width = base.shape[:2]
        source_dir = output_dir / "images" / source_id

        if not args.without_reference:
            relative_path = Path("images") / source_id / "reference.jpg"
            image_sha = _write_jpeg(
                output_dir / relative_path, base, args.jpeg_quality
            )
            records.append(
                _make_record(
                    relative_image=relative_path,
                    image_sha=image_sha,
                    width=width,
                    height=height,
                    source_id=source_id,
                    source_path=sample.path,
                    source_sha=source_sha,
                    labels=sample.labels,
                    profile="reference",
                    variant_index=None,
                    variant_seed=None,
                    transforms={
                        "source_size": [original_width, original_height],
                        "resize_to": [width, height],
                    },
                    screen_quad=_transform_quad(sample.screen_quad, base_transform),
                )
            )

        for variant_index in range(args.variants_per_input):
            profile = PROFILES[variant_index % len(PROFILES)]
            variant_seed = _stable_seed(args.seed, source_sha, variant_index)
            rng = np.random.default_rng(variant_seed)
            augmented, augmentation_transform, params = _augment(base, profile, rng)
            total_transform = augmentation_transform @ base_transform
            relative_path = (
                Path("images")
                / source_id
                / f"variant_{variant_index:04d}_{profile}.jpg"
            )
            image_sha = _write_jpeg(
                output_dir / relative_path, augmented, args.jpeg_quality
            )
            records.append(
                _make_record(
                    relative_image=relative_path,
                    image_sha=image_sha,
                    width=width,
                    height=height,
                    source_id=source_id,
                    source_path=sample.path,
                    source_sha=source_sha,
                    labels=sample.labels,
                    profile=profile,
                    variant_index=variant_index,
                    variant_seed=variant_seed,
                    transforms={
                        "source_size": [original_width, original_height],
                        "resize_to": [width, height],
                        **params,
                    },
                    screen_quad=_transform_quad(sample.screen_quad, total_transform),
                )
            )

        source_summaries.append(
            {
                "source_id": source_id,
                "path": str(sample.path),
                "sha256": source_sha,
                "labels": sample.labels,
                "has_screen_quad": sample.screen_quad is not None,
                "source_size": [original_width, original_height],
                "output_size": [width, height],
            }
        )

    _write_jsonl(output_dir / "manifest.jsonl", records)
    observed_digits = sorted(
        {
            int(character)
            for sample in samples
            for value in sample.labels.values()
            for character in str(value)
        }
    )
    missing_digits = sorted(set(range(10)) - set(observed_digits))
    summary = {
        "schema_version": 1,
        "tool": "augment_romsun.py",
        "tool_version": TOOL_VERSION,
        "seed": args.seed,
        "source_count": len(samples),
        "image_count": len(records),
        "variants_per_input": args.variants_per_input,
        "reference_included": not args.without_reference,
        "max_side": args.max_side,
        "jpeg_quality": args.jpeg_quality,
        "profiles": list(PROFILES),
        "observed_measurement_digits": observed_digits,
        "missing_measurement_digits": missing_digits,
        "digit_synthesis": {
            "enabled": False,
            "reason": (
                "The real photos do not provide verified, rectified digit boxes and "
                "device-faithful background patches for every row. Segment recombination "
                "would create unverified synthetic features and is unsafe for a 99% target."
            ),
        },
        "split_policy": {
            "group_field": "group_id",
            "rule": (
                "Keep every derivative of a source photo in the same split. Do not use "
                "these eight sources as a 99% acceptance set; collect independent real scans."
            ),
        },
        "limitations": [
            "Augmentation changes nuisance conditions but does not add independent devices, readings or capture sessions.",
            "The known eight photos contain only two measurement combinations.",
            "Missing measurement digits are not synthesized.",
            "A 99% end-to-end claim requires an independent frozen real-world acceptance set.",
        ],
        "sources": source_summaries,
        "manifest": "manifest.jsonl",
    }
    _write_json(output_dir / "dataset_info.json", summary)
    return summary


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(sys.argv[1:] if argv is None else argv)
    try:
        samples = _collect_samples(args)
        summary = _generate(args, samples)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2
    print(
        f"Generated {summary['image_count']} images from "
        f"{summary['source_count']} sources in {args.output.resolve()}"
    )
    if summary["missing_measurement_digits"]:
        missing = ", ".join(str(value) for value in summary["missing_measurement_digits"])
        print(f"Missing real measurement digits (not synthesized): {missing}")
    print(f"Manifest: {(args.output.resolve() / 'manifest.jsonl')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
