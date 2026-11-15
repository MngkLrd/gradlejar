"""Pocket Realms Stage II 0.7.0 compiled-distribution checks."""
from pathlib import Path
import zipfile,json,struct
root=Path(__file__).resolve().parents[1]/"projects/pocketrealms"
jar=root/"build/libs/pocketrealms-0.7.0.jar"
assert jar.exists(),f"Missing JAR: {jar}"
required=[
"com/kirill/pocketrealms/entity/Knocker.class",
"com/kirill/pocketrealms/entity/CeilingLurker.class",
"com/kirill/pocketrealms/entity/ChoirWraith.class",
"com/kirill/pocketrealms/world/DepthsEncounterDirector.class",
"com/kirill/pocketrealms/world/PocketTeleporter.class",
"com/kirill/pocketrealms/world/PocketRunEntry.class",
"com/kirill/pocketrealms/block/DescentGateBlock.class",
"com/kirill/pocketrealms/client/DepthsModel.class",
"com/kirill/pocketrealms/client/DepthsRenderers.class",
"assets/pocketrealms/blockstates/descent_gate.json",
"assets/pocketrealms/models/block/descent_gate.json",
"assets/pocketrealms/textures/block/descent_gate.png",
"META-INF/neoforge.mods.toml"]
with zipfile.ZipFile(jar) as z:
    names=set(z.namelist())
    for path in required:assert path in names,path
    for monster in ("knocker","ceiling_lurker","choir_wraith"):
        texture=f"assets/pocketrealms/textures/entity/{monster}.png"
        assert texture in names,texture
        data=z.read(texture)
        assert data[:8]==b"\x89PNG\r\n\x1a\n"
        assert struct.unpack(">II",data[16:24])==(128,128)
    assert struct.unpack(">II",z.read("assets/pocketrealms/textures/block/descent_gate.png")[16:24])==(32,32)
    assert len(names)==len(z.namelist()),"Duplicate jar members"
pkg=root/"src/main/java/com/kirill/pocketrealms"
r=(pkg/"world/PocketRunEntry.java").read_text("utf8")
assert 'tag.putInt("RealmStage",stage);' in r
assert 'tag.contains("RealmStage")' in r
g=(pkg/"world/PocketGenerator.java").read_text("utf8")
assert "entry.stage==1" in g and "placeDescentGate" in g
assert "entry.stage>=2?155:180" in g
t=(pkg/"world/PocketTeleporter.java").read_text("utf8")
assert "advanceStage" in t and "PocketGenerator.generate(realm,next)" in t
model=(pkg/"client/DepthsModel.java").read_text("utf8")
for part in ("knocker()","lurker()","choir()"):
    assert part in model
print(f"PASS 0.7.0: {jar.stat().st_size} bytes, 3 custom mobs/meshes/UV, NBT stages, gate, two timed raids")
