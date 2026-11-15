"""Jar smoke test for Pocket Realms 0.6.2; no Minecraft client required."""
from pathlib import Path
import struct
import zipfile
root=Path(__file__).resolve().parents[1]
jar=root/"projects/pocketrealms/build/libs/pocketrealms-0.6.2.jar"
assert jar.exists(),jar
with zipfile.ZipFile(jar) as z:
    names=set(z.namelist())
    expected=[
        "com/kirill/pocketrealms/world/SafeMazeRouting.class",
        "com/kirill/pocketrealms/world/PressureErosion.class",
        "com/kirill/pocketrealms/world/LivingLabyrinth.class",
        "com/kirill/pocketrealms/world/RiftThreatDirector.class",
        "com/kirill/pocketrealms/client/HorrorZombieModel.class",
        "com/kirill/pocketrealms/client/HorrorSkeletonModel.class",
        "com/kirill/pocketrealms/entity/VoidPursuer.class",
        "com/kirill/pocketrealms/entity/WallGrasper.class",
        "assets/pocketrealms/textures/entity/corrupted_zombie.png",
        "assets/pocketrealms/textures/entity/corrupted_skeleton.png",
        "assets/pocketrealms/blockstates/ritual_slab.json",
        "META-INF/neoforge.mods.toml",
    ]
    for key in expected:assert key in names,f"JAR missing {key}"
    for name in expected:
        if name.endswith(".png"):
            b=z.read(name)
            assert b[:8]==b"\x89PNG\r\n\x1a\n",name
            assert struct.unpack(">II",b[16:24])==(64,64),f"Bad 64x64 model texture: {name}"
    assert len(z.namelist())==len(names),"Duplicate jar entries"
print(f"PASS Pocket Realms 0.6.2: {len(expected)} required files, 64x64 skins, no duplicates")
