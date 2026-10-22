"""Restore actual custom 3D monsters and wall-safe hands for 1.21.1 NeoForge."""
from pathlib import Path
import base64, hashlib, io, lzma, tarfile
root=Path(__file__).resolve().parents[1]/"projects"/"pocketrealms"
parts=[root/f"upgrade-v052-{i:02d}.b64" for i in range(4)]
blob=base64.b64decode("".join(p.read_text("ascii") for p in parts),validate=True)
actual=hashlib.sha256(blob).hexdigest()
expected="c6e59a2c00e145cb3f2ef067c4541e375696d21b69a78fb4a91e9eb20c2d3d9e"
if actual!=expected:raise SystemExit("Corrupted 0.5.2 transfer: "+actual)
with tarfile.open(fileobj=io.BytesIO(lzma.decompress(blob)),mode="r:") as archive:
    for member in archive.getmembers():
        rel=Path(member.name)
        if not member.isfile() or rel.is_absolute() or ".." in rel.parts:
            raise SystemExit(f"Unsafe archive member {member.name}")
        out=root/rel
        out.parent.mkdir(parents=True,exist_ok=True)
        with archive.extractfile(member) as src:out.write_bytes(src.read())
# LayerDefinitions must be registered on the client-only event bus.
file=root/"src/main/java/com/kirill/pocketrealms/client/PocketClientEvents.java"
s=file.read_text(encoding="utf8")
anchor="event.registerLayerDefinition(GraspingHandsModel.LAYER, GraspingHandsModel::createBodyLayer);"
if s.count(anchor)!=1:raise SystemExit("Missing LayerDefinitions registry anchor")
s=s.replace(anchor,anchor+"\n        event.registerLayerDefinition(VoidPursuerModel.LAYER, VoidPursuerModel::createBodyLayer);")
file.write_text(s,encoding="utf8")
with (root/"README.md").open("a",encoding="utf8") as fh:
    fh.write("\n## 0.5.2 custom models and collision repairs\n"
             "- Wall hands: no torso or legs, wall-anchored 3D forearms/palms/claws, "
             "collision box 0.58 x 0.98 placed entirely on corridor side of wall. "
             "No wall suffocation, can be killed by player damage.\n"
             "- Void Pursuer: now extends Monster instead of Zombie and renders a completely new "
             "asymmetric, long-limbed ModelPart mesh (not ZombieModel), with unique 128x128 UV skin "
             "and 0.78 x 2.55 hitbox.\n")
# Source overlay must use the SAME sound IDs as 0.5.1's two OGG files.
pursuer=root/"src/main/java/com/kirill/pocketrealms/entity/VoidPursuer.java"
body=pursuer.read_text(encoding="utf8")
for old,new in [
    ("ModSounds.PURSUER_AMBIENT.get()","ModSounds.PURSUER_IDLE.get()"),
    ("ModSounds.PURSUER_HURT.get()","net.minecraft.sounds.SoundEvents.WARDEN_HURT"),
]:
    if body.count(old)!=1:raise SystemExit("Missing 0.5.2 sound reference: "+old)
    body=body.replace(old,new,1)
if body.count("ModSounds.PURSUER_ATTACK.get()")!=2:raise SystemExit("Missing death + hit sound references")
body=body.replace("ModSounds.PURSUER_ATTACK.get()","net.minecraft.sounds.SoundEvents.WARDEN_DEATH",1)
body=body.replace("ModSounds.PURSUER_ATTACK.get()","net.minecraft.sounds.SoundEvents.WARDEN_ATTACK_IMPACT",1)
pursuer.write_text(body,encoding="utf8")
director=root/"src/main/java/com/kirill/pocketrealms/world/RiftThreatDirector.java"
d=director.read_text(encoding="utf8")
if d.count("ModSounds.PURSUER_AMBIENT.get()")!=1:raise SystemExit("Missing director sound ref")
director.write_text(d.replace("ModSounds.PURSUER_AMBIENT.get()","ModSounds.PURSUER_IDLE.get()",1),encoding="utf8")
print("Pocket Realms 0.5.2 sources restored and client model layers wired.")
