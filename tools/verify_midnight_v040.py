from pathlib import Path
import json
import struct
import sys
base = Path(sys.argv[1])
assets = base / "src/main/resources/assets/midnighteve"
data = base / "src/main/resources/data/midnighteve"
textures = sorted((assets/"textures").rglob("*.png"))
if len(textures) != 12:
    raise SystemExit(f"Expected 12 standalone vanilla textures, got {len(textures)}")
for f in textures:
    b = f.read_bytes()
    if not b.startswith(b"\x89PNG\r\n\x1a\n") or len(b) < 24:
        raise SystemExit(f"Bad PNG: {f}")
    width, height = struct.unpack(">II", b[16:24])
    expected = 64 if f.parent.name == "entity" else 16
    if (width,height) != (expected,expected):
        raise SystemExit(f"Bad texture resolution {f}: {width}x{height}")
for f in (base/"src/main/resources").rglob("*.json"):
    json.loads(f.read_text(encoding="utf8"))
names = ["firlight_woods","peppermint_grove","starlit_steppe",
         "crystal_crown","mirror_fjords","silent_snowfields"]
for name in names:
    obj = json.loads((data/"worldgen/biome"/f"{name}.json").read_text())
    if f"midnighteve:{name}_flora" not in obj["features"][9]:
        raise SystemExit(f"Missing flora in {name}")
    if name in ("firlight_woods","peppermint_grove","silent_snowfields"):
        if f"midnighteve:{name}_workshop" not in obj["features"][9]:
            raise SystemExit(f"Missing workshop in {name}")
for name in ("fir_planks","frost_bricks","workshop_star"):
    if not (assets/"blockstates"/f"{name}.json").is_file():
        raise SystemExit(f"Missing model for {name}")
print("PASS: vanilla 16px/64px textures, valid JSON and configured biome features")
