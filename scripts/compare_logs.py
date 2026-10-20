#!/usr/bin/env python3
"""Compare two experiment logs ignoring the host-dependent wall-clock overhead column.

Usage: python scripts/compare_logs.py results/EXP-A.log /path/to/your/EXP-A.log

Every simulated metric (response, waiting, makespan, starvation counts, Jain index) is
seed-deterministic and must match to the last decimal; only `overhead.ms` varies with the host.
Rows in blocks whose header contains "overhead" have their last numeric column dropped before
comparison. Exit code 0 = identical, 1 = differences (printed as a unified diff).
"""
import difflib
import re
import sys

NUM = re.compile(r"[-\d.]+±[-\d.]+|[-\d.]+(?=\s*$)")


def normalise(path):
    out, drop_last = [], False
    for line in open(path, encoding="utf-8"):
        line = line.rstrip("\n")
        if re.search(r"\boverhead", line) and "±" not in line:
            drop_last = True            # header of a block with an overhead column
        elif line.strip().startswith(("===", "####", "[", "(")) or not line.strip():
            drop_last = drop_last and "±" in line
        if drop_last and "±" in line:
            vals = NUM.findall(line)
            if vals:
                line = line[: line.rfind(vals[-1])].rstrip()
        if re.search(r"start \d\d:\d\d:\d\d|\bstart \d", line):
            continue                    # timestamps from run_all.sh
        out.append(line)
    return out


if __name__ == "__main__":
    a, b = sys.argv[1], sys.argv[2]
    la, lb = normalise(a), normalise(b)
    if la == lb:
        print(f"IDENTICAL (simulated metrics): {a} == {b}")
        sys.exit(0)
    sys.stdout.writelines(difflib.unified_diff(la, lb, a, b, lineterm=""))
    print()
    sys.exit(1)
