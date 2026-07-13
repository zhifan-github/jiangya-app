"""Train a tiny offline classifier for the ROMSUN seven-segment digit cells.

The generator keeps the seven-segment label exact while varying geometry,
stroke bloom, outline/fill, exposure, blur, glare and sensor noise. It is a
supplement to real-photo validation, not a substitute for the frozen device
acceptance set.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import cv2
import numpy as np
import tensorflow as tf

WIDTH = 64
HEIGHT = 96
PATTERNS = {
    0: (1, 1, 1, 1, 1, 1, 0),
    1: (0, 1, 1, 0, 0, 0, 0),
    2: (1, 1, 0, 1, 1, 0, 1),
    3: (1, 1, 1, 1, 0, 0, 1),
    4: (0, 1, 1, 0, 0, 1, 1),
    5: (1, 0, 1, 1, 0, 1, 1),
    6: (1, 0, 1, 1, 1, 1, 1),
    7: (1, 1, 1, 0, 0, 0, 0),
    8: (1, 1, 1, 1, 1, 1, 1),
    9: (1, 1, 1, 1, 0, 1, 1),
}


def segment_polygons(thickness: int) -> list[np.ndarray]:
    t = thickness
    return [
        np.array([[14, 5], [50, 5], [50 + t // 2, 9], [50, 13], [14, 13], [14 - t // 2, 9]]),
        np.array([[51, 11], [57, 15], [57, 43], [53, 47], [49, 43], [49, 15]]),
        np.array([[53, 49], [57, 53], [57, 81], [51, 87], [49, 81], [49, 53]]),
        np.array([[14, 83], [50, 83], [50 + t // 2, 87], [50, 91], [14, 91], [14 - t // 2, 87]]),
        np.array([[7, 49], [15, 53], [15, 81], [11, 87], [5, 81], [5, 53]]),
        np.array([[7, 11], [15, 15], [15, 43], [11, 47], [5, 43], [5, 15]]),
        np.array([[14, 43], [50, 43], [54, 48], [50, 53], [14, 53], [10, 48]]),
    ]


def render_digit(digit: int, rng: np.random.Generator) -> np.ndarray:
    canvas = np.zeros((HEIGHT, WIDTH), np.uint8)
    thickness = int(rng.integers(6, 11))
    outline = bool(rng.random() < 0.68)
    for enabled, polygon in zip(PATTERNS[digit], segment_polygons(thickness)):
        if not enabled:
            continue
        if outline:
            cv2.polylines(canvas, [polygon], True, int(rng.integers(190, 256)), int(rng.integers(2, 5)))
        else:
            cv2.fillConvexPoly(canvas, polygon, int(rng.integers(190, 256)))

    source = np.float32([[0, 0], [WIDTH - 1, 0], [WIDTH - 1, HEIGHT - 1], [0, HEIGHT - 1]])
    jitter = rng.uniform(-4.0, 4.0, (4, 2)).astype(np.float32)
    target = source + jitter
    canvas = cv2.warpPerspective(canvas, cv2.getPerspectiveTransform(source, target), (WIDTH, HEIGHT))

    angle = float(rng.uniform(-7.0, 7.0))
    scale = float(rng.uniform(0.88, 1.08))
    tx = float(rng.uniform(-3.0, 3.0))
    ty = float(rng.uniform(-3.0, 3.0))
    affine = cv2.getRotationMatrix2D((WIDTH / 2, HEIGHT / 2), angle, scale)
    affine[:, 2] += (tx, ty)
    canvas = cv2.warpAffine(canvas, affine, (WIDTH, HEIGHT))

    if rng.random() < 0.45:
        kernel = np.ones((int(rng.choice([2, 3])), int(rng.choice([2, 3]))), np.uint8)
        canvas = cv2.dilate(canvas, kernel)
    if rng.random() < 0.22:
        canvas = cv2.GaussianBlur(canvas, (3, 3), float(rng.uniform(0.4, 1.2)))
    if rng.random() < 0.12:
        x = int(rng.integers(0, WIDTH))
        cv2.line(canvas, (x, 0), (min(WIDTH - 1, x + int(rng.integers(3, 10))), HEIGHT - 1),
                 int(rng.integers(25, 90)), int(rng.integers(2, 7)))

    gain = float(rng.uniform(0.65, 1.25))
    noise = rng.normal(0.0, rng.uniform(1.0, 12.0), canvas.shape)
    return np.clip(canvas.astype(np.float32) * gain + noise, 0, 255).astype(np.uint8)


def make_dataset(per_class: int, seed: int) -> tuple[np.ndarray, np.ndarray]:
    rng = np.random.default_rng(seed)
    images = np.empty((per_class * 10, HEIGHT, WIDTH, 1), np.uint8)
    labels = np.empty(per_class * 10, np.int64)
    index = 0
    for digit in range(10):
        for _ in range(per_class):
            images[index, :, :, 0] = render_digit(digit, rng)
            labels[index] = digit
            index += 1
    order = rng.permutation(len(labels))
    return images[order], labels[order]


def build_model() -> tf.keras.Model:
    return tf.keras.Sequential([
        tf.keras.layers.Input((HEIGHT, WIDTH, 1)),
        tf.keras.layers.Rescaling(1.0 / 255.0),
        tf.keras.layers.Conv2D(16, 5, activation="relu", padding="same"),
        tf.keras.layers.MaxPooling2D(),
        tf.keras.layers.Conv2D(32, 3, activation="relu", padding="same"),
        tf.keras.layers.MaxPooling2D(),
        tf.keras.layers.Conv2D(48, 3, strides=2, activation="relu", padding="same"),
        tf.keras.layers.Flatten(),
        tf.keras.layers.Dense(64, activation="relu"),
        tf.keras.layers.Dropout(0.15),
        tf.keras.layers.Dense(10, activation="softmax"),
    ])


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--metrics", type=Path, required=True)
    parser.add_argument("--train-per-class", type=int, default=1400)
    parser.add_argument("--validation-per-class", type=int, default=250)
    args = parser.parse_args()

    tf.keras.utils.set_random_seed(20260713)
    train_x, train_y = make_dataset(args.train_per_class, 20260713)
    validation_x, validation_y = make_dataset(args.validation_per_class, 20260714)
    model = build_model()
    model.compile(optimizer="adam", loss="sparse_categorical_crossentropy", metrics=["accuracy"])
    history = model.fit(
        train_x,
        train_y,
        validation_data=(validation_x, validation_y),
        batch_size=96,
        epochs=5,
        verbose=2,
    )

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_model = converter.convert()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(tflite_model)

    predictions = model.predict(validation_x, batch_size=128, verbose=0).argmax(axis=1)
    accuracy = float((predictions == validation_y).mean())
    per_digit = {
        str(digit): float((predictions[validation_y == digit] == digit).mean())
        for digit in range(10)
    }
    metrics = {
        "synthetic_validation_accuracy": accuracy,
        "per_digit_accuracy": per_digit,
        "model_bytes": len(tflite_model),
        "epochs": len(history.history["loss"]),
        "warning": "Synthetic validation is not the 1000-session device acceptance test.",
    }
    args.metrics.parent.mkdir(parents=True, exist_ok=True)
    args.metrics.write_text(json.dumps(metrics, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(metrics, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
