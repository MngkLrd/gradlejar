"""Distribution regression checks for Minecraft 1.21.1 NeoForge Pocket Realms 0.6.4."""
from pathlib import Path
import json
import zipfile
import struct
root=Path(__file__).resolve().parents[1]
jar=root/"projects/pocketrealms/build/libs/pocketrealms-0.6.4.jar"
assert jar.exists(),jar
expected=[
"com/kirill/pocketrealms/entity/VeilHound.class",
"com/kirill/pocketrealms/entity/VeilGhost.class",
"com/kirill/pocketrealms/world/HauntingDirector.class",
"com/kirill/pocketrealms/world/PocketTeleporter.class",
"com/kirill/pocketrealms/item/RealmTotemItem.class",
"com/kirill/pocketrealms/client/VeilHoundRenderer.class",
"com/kirill/pocketrealms/client/VeilGhostRenderer.class",
"assets/pocketrealms/textures/entity/veil_hound.png",
"assets/pocketrealms/textures/entity/veil_ghost.png",
"META-INF/neoforge.mods.toml"]
items=["realm_totem","veil_hood","veil_coat","veil_leggings","veil_boots"]
with zipfile.ZipFile(jar) as z:
    names=set(z.namelist())
    assert all(name in names for name in expected),set(expected)-names
    for item in items:
        for name in (f"assets/pocketrealms/models/item/{item}.json",
            f"assets/pocketrealms/textures/item/{item}.png",
            f"data/pocketrealms/recipe/{item}.json"):
            assert name in names,name
        recipe=json.loads(z.read(f"data/pocketrealms/recipe/{item}.json"))
        assert recipe["result"]["id"]==f"pocketrealms:{item}"
        assert all(len(s)==len(recipe["pattern"][0]) for s in recipe["pattern"])
        asset=z.read(f"assets/pocketrealms/textures/item/{item}.png")
        assert struct.unpack(">II",asset[16:24])==(16,16)
    for name in ("veil_hound","veil_ghost"):
        asset=z.read(f"assets/pocketrealms/textures/entity/{name}.png")
        assert struct.unpack(">II",asset[16:24])==(64,64)
    assert len(z.namelist())==len(names),"Duplicate archive entries"
sources=root/"projects/pocketrealms/src/main/java/com/kirill/pocketrealms"
collapse=(sources/"world/CollapseController.java").read_text()
assert "SafeMazeRouting.canAlter" not in collapse
teleporter=(sources/"world/PocketTeleporter.java").read_text()
assert "Blocks.TORCH" in teleporter and "startTotemRun" in teleporter
hound=(sources/"entity/VeilHound.java").read_text()
assert "teleportTo" not in hound and "setPos(getX()" in hound
print(f"PASS: Pocket Realms 0.6.4 {jar.stat().st_size} bytes; 2 mobs, 5 items, 5 recipes, real torch, lethal collapse.")
