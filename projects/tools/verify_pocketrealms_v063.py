"""Validate v0.6.3's physically packaged gameplay classes, models and textures."""
from pathlib import Path
import json
import struct
import zipfile
root=Path(__file__).resolve().parents[1]
jar=root/"projects/pocketrealms/build/libs/pocketrealms-0.6.3.jar"
assert jar.exists(),f"Missing JAR {jar}"
required=[
"com/kirill/pocketrealms/world/PocketDreadDirector.class",
"com/kirill/pocketrealms/world/PressureErosion.class",
"com/kirill/pocketrealms/world/SafeMazeRouting.class",
"com/kirill/pocketrealms/world/CollapseController.class",
"com/kirill/pocketrealms/block/PocketDoorBlock.class",
"com/kirill/pocketrealms/block/CrumblingStoneBlock.class",
"assets/pocketrealms/textures/block/unstable_void_bricks.png",
"assets/pocketrealms/textures/block/unstable_void_bricks_cracking.png",
"assets/pocketrealms/blockstates/unstable_void_bricks.json",
"assets/pocketrealms/models/block/unstable_void_bricks.json",
"assets/pocketrealms/models/block/unstable_void_bricks_cracking.json",
"assets/pocketrealms/models/item/unstable_void_bricks.json",
"META-INF/neoforge.mods.toml"
]
with zipfile.ZipFile(jar) as z:
    names=z.namelist()
    assert len(names)==len(set(names)),"Duplicate archive members"
    for name in required:assert name in names,"Missing "+name
    for name in required:
        if name.endswith(".png"):
            b=z.read(name)
            assert b[:8]==b"\x89PNG\r\n\x1a\n"
            assert struct.unpack(">II",b[16:24])==(32,32)
    states=json.loads(z.read("assets/pocketrealms/blockstates/unstable_void_bricks.json"))
    assert set(states["variants"])=={"cracking=false","cracking=true"}
    assert z.read("assets/pocketrealms/textures/block/unstable_void_bricks.png")!=z.read(
        "assets/pocketrealms/textures/block/unstable_void_bricks_cracking.png")
source=root/"projects/pocketrealms/src/main/java/com/kirill/pocketrealms/world/PocketGenerator.java"
gen=source.read_text(encoding="utf8")
assert "private static final int GRID = 49;" in gen
assert "for(int dx=-9;dx<=9;dx++)" in gen
assert "hasRoute(open,1,1,GRID-12,GRID-12)" in gen
print(f"PASS: Pocket Realms 0.6.3 ({jar.stat().st_size} bytes), arena, 32x32 timed bricks, fear system, door particles")
