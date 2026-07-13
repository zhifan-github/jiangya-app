#!/usr/bin/env python3
"""Evaluate the frozen 1,000-session ROMSUN recognition acceptance set.

The input is a CSV file with one real capture session per row. Required fields:

    truth_systolic,truth_diastolic,truth_heart_rate,
    pred_systolic,pred_diastolic,pred_heart_rate

``session_id`` is optional but recommended. A rejected scan may leave all three
prediction fields empty, put ``reject`` in all three fields, or set an optional
``rejected`` field to true / an optional ``status`` field to reject. Partial
predictions are invalid and stop evaluation.

The fixed acceptance gate is intentionally not configurable:

* exactly 1,000 independent real scan sessions;
* at least 997 exact-triplet correct results;
* at most one wrong automatic fill;
* all other sessions may be rejected.

Example:

    python evaluate_acceptance.py acceptance.csv
    python evaluate_acceptance.py acceptance.csv --json
"""

from __future__ import annotations

import argparse
import csv
import json
import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Mapping, Sequence, TextIO


REQUIRED_SESSIONS = 1000
MIN_CORRECT = 997
MAX_WRONG = 1

TRUTH_FIELDS = (
    "truth_systolic",
    "truth_diastolic",
    "truth_heart_rate",
)
PREDICTION_FIELDS = (
    "pred_systolic",
    "pred_diastolic",
    "pred_heart_rate",
)
REQUIRED_FIELDS = TRUTH_FIELDS + PREDICTION_FIELDS

REJECT_TOKENS = {"reject", "rejected"}
TRUE_TOKENS = {"1", "true", "yes", "y", *REJECT_TOKENS}
FALSE_TOKENS = {"0", "false", "no", "n", "accept", "accepted", "success", "recognized"}


class AcceptanceInputError(ValueError):
    """Raised when an acceptance CSV row is ambiguous or malformed."""


@dataclass(frozen=True)
class Reading:
    systolic: int
    diastolic: int
    heart_rate: int


@dataclass(frozen=True)
class WrongExample:
    session_id: str
    truth: Reading
    prediction: Reading


@dataclass(frozen=True)
class AcceptanceResult:
    total_sessions: int
    correct: int
    wrong: int
    reject: int
    exact_triplet_accuracy: float
    wrong_autofill_rate: float
    accepted_precision: float | None
    passed: bool
    failures: tuple[str, ...]
    wrong_examples: tuple[WrongExample, ...]

    def to_dict(self) -> dict[str, object]:
        value = asdict(self)
        value["thresholds"] = {
            "required_sessions": REQUIRED_SESSIONS,
            "minimum_correct": MIN_CORRECT,
            "maximum_wrong": MAX_WRONG,
        }
        return value


def _clean(value: object) -> str:
    return "" if value is None else str(value).strip()


def _parse_integer(value: object, field: str, line_number: int) -> int:
    text = _clean(value)
    try:
        return int(text)
    except ValueError as exc:
        raise AcceptanceInputError(
            f"line {line_number}: {field} must be an integer, got {text!r}"
        ) from exc


def _parse_truth(row: Mapping[str, object], line_number: int) -> Reading:
    return Reading(
        systolic=_parse_integer(row.get(TRUTH_FIELDS[0]), TRUTH_FIELDS[0], line_number),
        diastolic=_parse_integer(row.get(TRUTH_FIELDS[1]), TRUTH_FIELDS[1], line_number),
        heart_rate=_parse_integer(row.get(TRUTH_FIELDS[2]), TRUTH_FIELDS[2], line_number),
    )


def _parse_optional_decision(
    row: Mapping[str, object], field: str, line_number: int
) -> bool | None:
    text = _clean(row.get(field)).lower()
    if not text:
        return None
    if text in TRUE_TOKENS:
        return True
    if text in FALSE_TOKENS:
        return False
    raise AcceptanceInputError(
        f"line {line_number}: {field} must indicate accepted or rejected, got {text!r}"
    )


def _explicit_rejection(
    row: Mapping[str, object], line_number: int
) -> bool | None:
    rejected = _parse_optional_decision(row, "rejected", line_number)
    status = _parse_optional_decision(row, "status", line_number)
    if rejected is not None and status is not None and rejected != status:
        raise AcceptanceInputError(
            f"line {line_number}: rejected and status disagree"
        )
    return rejected if rejected is not None else status


def _parse_prediction(
    row: Mapping[str, object], line_number: int
) -> Reading | None:
    values = [_clean(row.get(field)) for field in PREDICTION_FIELDS]
    normalized = [value.lower() for value in values]
    is_no_value = [not value or value in REJECT_TOKENS for value in normalized]
    explicit_rejection = _explicit_rejection(row, line_number)

    if all(is_no_value):
        if explicit_rejection is False:
            raise AcceptanceInputError(
                f"line {line_number}: scan is marked accepted but has no prediction"
            )
        return None

    if any(is_no_value):
        raise AcceptanceInputError(
            f"line {line_number}: prediction must contain all three integers or be rejected"
        )

    prediction = Reading(
        systolic=_parse_integer(values[0], PREDICTION_FIELDS[0], line_number),
        diastolic=_parse_integer(values[1], PREDICTION_FIELDS[1], line_number),
        heart_rate=_parse_integer(values[2], PREDICTION_FIELDS[2], line_number),
    )
    if explicit_rejection is True:
        raise AcceptanceInputError(
            f"line {line_number}: rejected scan must not contain a numeric prediction"
        )
    return prediction


def evaluate_rows(rows: Iterable[Mapping[str, object]]) -> AcceptanceResult:
    """Evaluate already parsed rows; exposed for lightweight automated tests."""

    total = 0
    correct = 0
    wrong = 0
    reject = 0
    wrong_examples: list[WrongExample] = []
    seen_session_ids: set[str] = set()

    for line_number, row in enumerate(rows, start=2):
        if not any(_clean(value) for value in row.values()):
            continue
        total += 1
        session_id = _clean(row.get("session_id")) or f"row-{line_number}"
        if session_id in seen_session_ids:
            raise AcceptanceInputError(
                f"line {line_number}: duplicate session_id {session_id!r}"
            )
        seen_session_ids.add(session_id)

        truth = _parse_truth(row, line_number)
        prediction = _parse_prediction(row, line_number)
        if prediction is None:
            reject += 1
        elif prediction == truth:
            correct += 1
        else:
            wrong += 1
            if len(wrong_examples) < 20:
                wrong_examples.append(WrongExample(session_id, truth, prediction))

    if total == 0:
        raise AcceptanceInputError("input contains no scan sessions")

    failures: list[str] = []
    if total != REQUIRED_SESSIONS:
        failures.append(
            f"requires exactly {REQUIRED_SESSIONS} sessions, found {total}"
        )
    if correct < MIN_CORRECT:
        failures.append(f"requires at least {MIN_CORRECT} correct, found {correct}")
    if wrong > MAX_WRONG:
        failures.append(f"allows at most {MAX_WRONG} wrong, found {wrong}")

    accepted = correct + wrong
    return AcceptanceResult(
        total_sessions=total,
        correct=correct,
        wrong=wrong,
        reject=reject,
        exact_triplet_accuracy=correct / total,
        wrong_autofill_rate=wrong / total,
        accepted_precision=(correct / accepted) if accepted else None,
        passed=not failures,
        failures=tuple(failures),
        wrong_examples=tuple(wrong_examples),
    )


def evaluate_csv(handle: TextIO) -> AcceptanceResult:
    reader = csv.DictReader(handle)
    headers = set(reader.fieldnames or ())
    missing = [field for field in REQUIRED_FIELDS if field not in headers]
    if missing:
        raise AcceptanceInputError(
            "missing required CSV field(s): " + ", ".join(missing)
        )
    return evaluate_rows(reader)


def _format_percentage(value: float | None) -> str:
    return "n/a" if value is None else f"{value:.4%}"


def _print_human(result: AcceptanceResult) -> None:
    print(f"Sessions: {result.total_sessions}")
    print(f"Correct exact triplets: {result.correct}")
    print(f"Wrong automatic fills: {result.wrong}")
    print(f"Rejected scans: {result.reject}")
    print(
        "Exact-triplet accuracy: "
        f"{_format_percentage(result.exact_triplet_accuracy)} "
        f"({result.correct}/{result.total_sessions})"
    )
    print(
        "Wrong autofill rate: "
        f"{_format_percentage(result.wrong_autofill_rate)} "
        f"({result.wrong}/{result.total_sessions})"
    )
    print(f"Accepted precision: {_format_percentage(result.accepted_precision)}")
    print(f"Verdict: {'PASS' if result.passed else 'FAIL'}")
    for failure in result.failures:
        print(f"- {failure}")
    if result.wrong_examples:
        print("Wrong examples (up to 20):")
        for example in result.wrong_examples:
            truth = example.truth
            prediction = example.prediction
            print(
                f"- {example.session_id}: "
                f"truth={truth.systolic}/{truth.diastolic}/{truth.heart_rate}, "
                f"prediction={prediction.systolic}/{prediction.diastolic}/"
                f"{prediction.heart_rate}"
            )


def _parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Evaluate the strict ROMSUN 1,000-session acceptance gate."
    )
    parser.add_argument("input", type=Path, help="Acceptance CSV file.")
    parser.add_argument(
        "--json", action="store_true", help="Write machine-readable JSON output."
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(sys.argv[1:] if argv is None else argv)
    try:
        with args.input.open("r", encoding="utf-8-sig", newline="") as handle:
            result = evaluate_csv(handle)
    except (OSError, AcceptanceInputError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2

    if args.json:
        print(json.dumps(result.to_dict(), ensure_ascii=False, indent=2))
    else:
        _print_human(result)
    return 0 if result.passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
