#!/usr/bin/env python3
"""Assert that a Minecraft region contains actual MIDNIGHT biome palette IDs."""
from pathlib import Path
import gzip
import sys
import zlib

roots = [Path(x) for x in sys.argv[1:]]
regions = [p for r in roots for p in r.rglob("ci-world/region/*.mca")]
print("Region files:", len(regions))
if not regions:
    sys.exit("No generated Anvil region files; server smoke test is incomplete.")
searched = 0
for path in regions:
    with path.open("rb") as file:
        header = file.read(4096)
        for n in range(1024):
            entry = header[4*n:4*n+4]
            sector = int.from_bytes(entry[:3], "big")
            if not sector:
                continue
            file.seek(sector*4096)
            length = int.from_bytes(file.read(4), "big")
            if length < 2 or length > 4_000_000:
                continue
            codec = file.read(1)[0]
            payload = file.read(length - 1)
            try:
                data = zlib.decompress(payload) if codec == 2 else gzip.decompress(payload) if codec == 1 else payload if codec == 3 else b""
            except (zlib.error, OSError):
                continue
            searched += 1
            if b"midnighteve:" in data:
                print("PASS: Chunk palettes contain MIDNIGHT biome IDs:", path, "chunk index", n)
                sys.exit(0)
sys.exit(f"FAIL: scanned {searched} chunks, none contain MIDNIGHT biome IDs. Check level-type/world preset loading.")
