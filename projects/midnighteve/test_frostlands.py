#!/usr/bin/env python3
"""Verify the saved world's actual generator settings, not merely resource existence."""
from pathlib import Path
import gzip,sys
worlds=[p for root in map(Path,sys.argv[1:]) for p in root.rglob("ci-world/level.dat")]
if not worlds:raise SystemExit("No saved test world")
for p in worlds:
    data=gzip.decompress(p.read_bytes())
    if b"midnighteve:frostlands" in data and b"minecraft:noise" in data:
        print("PASS: saved world uses MIDNIGHT Frostlands custom noise settings:",p)
        break
else:raise SystemExit("FAIL: Frostlands noise settings not present in saved world")
