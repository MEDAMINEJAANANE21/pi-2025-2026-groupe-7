#!/usr/bin/env python3
"""Compute and print global coverage totals from a JaCoCo CSV report."""

from __future__ import annotations

import csv
import sys
from pathlib import Path


def pct(covered: int, missed: int) -> float:
    total = covered + missed
    return (covered * 100.0 / total) if total > 0 else 0.0


def main() -> int:
    csv_path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("server/target/site/jacoco/jacoco.csv")

    if not csv_path.exists():
        print(f"JaCoCo CSV not found: {csv_path}")
        return 1

    instruction_missed = instruction_covered = 0
    branch_missed = branch_covered = 0
    line_missed = line_covered = 0

    with csv_path.open(newline="", encoding="utf-8") as fh:
        reader = csv.DictReader(fh)
        required = {
            "INSTRUCTION_MISSED",
            "INSTRUCTION_COVERED",
            "BRANCH_MISSED",
            "BRANCH_COVERED",
            "LINE_MISSED",
            "LINE_COVERED",
        }

        if not required.issubset(set(reader.fieldnames or [])):
            missing = sorted(required.difference(set(reader.fieldnames or [])))
            print(f"Invalid JaCoCo CSV format, missing columns: {', '.join(missing)}")
            return 2

        for row in reader:
            instruction_missed += int(row["INSTRUCTION_MISSED"])
            instruction_covered += int(row["INSTRUCTION_COVERED"])
            branch_missed += int(row["BRANCH_MISSED"])
            branch_covered += int(row["BRANCH_COVERED"])
            line_missed += int(row["LINE_MISSED"])
            line_covered += int(row["LINE_COVERED"])

    instruction_total = instruction_missed + instruction_covered
    branch_total = branch_missed + branch_covered
    line_total = line_missed + line_covered

    print(f"Coverage source: {csv_path}")
    print("Global coverage")
    print(
        f"- Instruction: {pct(instruction_covered, instruction_missed):.2f}% "
        f"({instruction_covered}/{instruction_total})"
    )
    if branch_total > 0:
        print(f"- Branch:      {pct(branch_covered, branch_missed):.2f}% ({branch_covered}/{branch_total})")
    else:
        print("- Branch:      N/A (no branches)")
    print(f"- Line:        {pct(line_covered, line_missed):.2f}% ({line_covered}/{line_total})")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
