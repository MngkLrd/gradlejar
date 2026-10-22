"""Pocket Realms 0.6.2 post-0.6.1 gameplay and horror assets overlay.
Copies full Java files checked in under tools/pocketrealms_v062, then creates
deterministic 64x64 pixel-clustered native Minecraft-style horror skins.
"""
from pathlib import Path
import random
import struct
import zlib

REPO=Path(__file__).resolve().parents[1]
ROOT=REPO/"projects"/"pocketrealms"
STAGED=REPO/"tools"/"pocketrealms_v062"
PKG=ROOT/"src/main/java/com/kirill/pocketrealms"
RES=ROOT/"src/main/resources/assets/pocketrealms"

for f in sorted(STAGED.rglob("*.java")):
    relative=f.relative_to(STAGED)
    destination=PKG/relative
    destination.parent.mkdir(parents=True,exist_ok=True)
    destination.write_bytes(f.read_bytes())
    print("PATCHED",relative)

p=ROOT/"gradle.properties"
v=p.read_text("utf8")
if v.count("mod_version=0.6.1")!=1:
    raise SystemExit("Wrong base for 0.6.2 overlay")
p.write_text(v.replace("mod_version=0.6.1","mod_version=0.6.2"),encoding="utf8")


def rgba_png(rows):
    height=len(rows);width=len(rows[0])
    raw=b"".join(b"\x00"+b"".join(bytes([*rgb,255]) for rgb in row) for row in rows)
    def chunk(t,payload):
        return struct.pack(">I",len(payload))+t+payload+struct.pack(">I",zlib.crc32(t+payload)&0xffffffff)
    return b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",width,height,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(raw,9))+chunk(b"IEND",b"")


def render_skin(kind):
    n=64;rnd=random.Random(621 if kind=="zombie" else 622)
    if kind=="zombie":
        dark=(25,28,28);shadow=(39,45,42);base=(59,67,60)
        bone=(135,118,98);blood=(108,25,36);bloodlite=(148,38,48)
    else:
        dark=(24,26,29);shadow=(45,45,48);base=(88,83,78)
        bone=(165,155,137);blood=(87,26,37);bloodlite=(129,33,44)
    palette=(dark,shadow,base,bone)
    pixels=[[palette[(x//3+y//4+rnd.randrange(3))%4] if rnd.random()<.12
             else (base if (x//4+y//4)%4==0 else shadow)
             for x in range(n)] for y in range(n)]

    def rect(x1,y1,x2,y2,color):
        for y in range(max(0,y1),min(n,y2)):
            for x in range(max(0,x1),min(n,x2)): pixels[y][x]=color

    # Minecraft model UV. Keep the head, torso, arms and legs visible from every side.
    # Head: front face x8..15 / y8..15.
    rect(8,8,16,16,bone if kind=="skeleton" else base)
    rect(8,8,16,10,dark)
    for x in (9,13):
        rect(x,10,x+2,12,dark)
        rect(x,11,x+1,12,(206,45,55))
    rect(9,13,15,15,dark)
    for x in (9,11,13):
        rect(x,13,x+1,14,(218,202,170))
    rect(11,15,16,16,blood)
    if kind=="zombie":
        rect(8,10,9,15,dark)
        rect(15,8,16,13,blood)
        rect(14,12,16,13,bloodlite)
    else:
        rect(8,12,10,13,blood)
        rect(14,8,16,10,dark)
        rect(12,14,14,16,(222,201,168))

    # Front of body x20..27 y20..31, exposed rib cage and torn vertical wound.
    rect(20,20,28,32,shadow)
    rect(23,21,25,31,dark)
    for y in (22,25,28):
        rect(20,y,24,y+1,bone)
        rect(25,y+1,28,y+2,bone)
    rect(22,26,27,29,blood)
    rect(23,27,25,29,bloodlite)
    rect(20,31,28,32,dark)
    # Add a different large tear on the back.
    rect(32,20,40,32,shadow)
    rect(34,22,37,29,dark)
    rect(35,24,38,27,blood)
    rect(36,25,38,27,bloodlite)
    # Wrapped dark forearms with two ragged wounds.
    for sx in (40,48,52,56):
        rect(sx,20,sx+3,32,base)
        rect(sx+1,25,sx+3,28,blood)
    # Thick clustered bone scarring on the sides.
    for x in range(0,64,8):
        rect(x,40,x+2,44,dark)
        rect(x+2,43,x+5,45,bone)
    # Matte, not glossy: no random single-pixel white noise or anti-aliasing.
    return rgba_png(pixels)


for mob,kind in (("corrupted_zombie","zombie"),("corrupted_skeleton","skeleton")):
    target=RES/"textures/entity"/(mob+".png")
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_bytes(render_skin(kind))

readme=ROOT/"README.md"
with readme.open("a",encoding="utf8") as f:
    f.write("""
\n## Pocket Realms 0.6.2 - Horror Overhaul
- 13x13 boss-ready terminal arena and portal with open centre (no boss yet)
- 3-4 varied ceremonial rooms, wider architectural archways
- Early physical erosion of the trail, confirmed with flood-fill route safety
- Faster terminal collapse after 180-245s, not 240-400s
- 8 shifting shortcuts near players, sealed only after route validation
- More reliable Pursuer and wall-grasping encounters
- Enemy placement checks open clearance and distances; hostile friendly fire is disabled
- Old containers are emptied before sector rebuild, and cleared on collapse
- Two custom geometrically mutated horror models, 64x64 authored pixel skins
- No permanent home-building or storage; one-use raid contract unchanged
""")
print("Pocket Realms 0.6.2 installed, horror skins generated.")
