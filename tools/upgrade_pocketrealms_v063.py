"""Pocket Realms 0.6.3 overlay. Run AFTER the verified 0.6.2 overlay.
No third-party libraries; scripts materialize Java and 32x32 block sprites.
"""
from pathlib import Path
import json
import random
import struct
import zlib

REPO=Path(__file__).resolve().parents[1]
ROOT=REPO/"projects/pocketrealms"
STAGED=REPO/"tools/pocketrealms_v063"
PKG=ROOT/"src/main/java/com/kirill/pocketrealms"
RES=ROOT/"src/main/resources/assets/pocketrealms"

def patch(path,old,new):
    text=path.read_text("utf8")
    if text.count(old)!=1:
        raise SystemExit(f"0.6.3 API patch anchor missing or duplicated: {path}: {old!r}")
    path.write_text(text.replace(old,new,1),encoding="utf8")

# Ensure every compiled overlay comes from checked-in, reviewable Java sources.
files=sorted(STAGED.rglob("*.java"))
if len(files)<7:raise SystemExit("Incomplete Pocket Realms 0.6.3 Java source overlay")
for file in files:
    target=PKG/file.relative_to(STAGED)
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_bytes(file.read_bytes())
    print("V063 PATCH",file.relative_to(STAGED))

# Same timed block class as the original Crumbling Stone, but a separate,
# visibly fractured purple-brick design in the generated maze.
modblocks=PKG/"registry/ModBlocks.java"
patch(modblocks,
    '    public static final DeferredBlock<Block> CRUMBLING_STONE = BLOCKS.register("crumbling_stone",',
    '''    public static final DeferredBlock<Block> UNSTABLE_VOID_BRICKS = BLOCKS.register("unstable_void_bricks",
            () -> new CrumblingStoneBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_DEEPSLATE_BRICKS)
                    .strength(2.0F, 1200.0F).noOcclusion()));
    public static final DeferredBlock<Block> CRUMBLING_STONE = BLOCKS.register("crumbling_stone",''')

items=PKG/"registry/ModItems.java"
patch(items,
    '    public static final DeferredItem<Item> CRUMBLING_STONE = ITEMS.registerItem("crumbling_stone",',
    '''    public static final DeferredItem<Item> UNSTABLE_VOID_BRICKS = ITEMS.registerItem("unstable_void_bricks",
            properties -> new BlockItem(ModBlocks.UNSTABLE_VOID_BRICKS.get(), properties));
    public static final DeferredItem<Item> CRUMBLING_STONE = ITEMS.registerItem("crumbling_stone",''')

tabs=PKG/"registry/ModCreativeTabs.java"
patch(tabs,
    '                output.accept(ModItems.CRUMBLING_STONE.get());',
    '                output.accept(ModItems.UNSTABLE_VOID_BRICKS.get());\n                output.accept(ModItems.CRUMBLING_STONE.get());')

for locale,name in (("en_us","Unstable Void Bricks"),("ru_ru","Нестабильные кирпичи Бездны")):
    f=RES/"lang"/(locale+".json")
    translations=json.loads(f.read_text("utf8"))
    translations["block.pocketrealms.unstable_void_bricks"]=name
    f.write_text(json.dumps(translations,indent=2,ensure_ascii=False)+"\n",encoding="utf8")

def encode(rows):
    width=len(rows[0]);height=len(rows)
    rgba=b"".join(b"\x00"+b"".join(bytes((*pixel,255)) for pixel in row) for row in rows)
    def chunk(tag,contents):
        return struct.pack(">I",len(contents))+tag+contents+struct.pack(">I",zlib.crc32(tag+contents)&0xffffffff)
    return (b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",width,height,8,6,0,0,0))
            +chunk(b"IDAT",zlib.compress(rgba,9))+chunk(b"IEND",b""))

def sprite(cracking):
    n=32
    rnd=random.Random(637+int(cracking))
    dark=(29,27,37)
    mortar=(23,21,30)
    mid=(66,59,78)
    high=(91,75,98)
    fissure=(155,80,152) if not cracking else (220,116,206)
    inner=(98,52,116) if not cracking else (178,65,177)
    image=[[mid for _ in range(n)] for _ in range(n)]
    def fill(x0,y0,x1,y1,color):
        for y in range(max(0,y0),min(n,y1)):
            for x in range(max(0,x0),min(n,x1)):image[y][x]=color
    # Large, deliberately readable 2x2 clusters, no subpixel noise.
    for y in range(0,n,2):
        for x in range(0,n,2):
            c=rnd.choice((dark,mid,mid,mid,high))
            fill(x,y,x+2,y+2,c)
    for y in (0,15,31):
        fill(0,y,n,y+1,mortar)
    for x,y0,y1 in ((9,0,15),(24,0,15),(4,16,31),(19,16,31)):
        fill(x,y0,x+1,y1,mortar)
        fill(x+1,y0+1,x+2,y1-1,high)
    # Central fracture shaped like a lightning tree; two-pixel readable lines.
    stroke=[(15,1),(15,4),(17,6),(17,9),(14,12),(14,15),(12,18),
            (13,21),(10,24),(11,28),(9,31)]
    for i,(x,y) in enumerate(stroke):
        fill(x,y,x+2,y+3,inner)
        fill(x,y,x+1,y+2,fissure)
    for x,y in ((14,12),(12,18),(13,21)):
        fill(x,y,x+7,y+1,inner)
        fill(x+4,y-2,x+6,y+1,fissure)
    if cracking:
        for x,y in ((3,4),(4,8),(5,12),(22,6),(24,10),(20,15),(26,19),(23,23),(19,27)):
            fill(x,y,x+2,y+5,fissure)
            fill(x,y+4,x+4,y+5,inner)
        fill(7,25,12,27,(231,168,210))
    return encode(image)

textures=RES/"textures/block"
textures.mkdir(parents=True,exist_ok=True)
for broken in (False,True):
    suffix="_cracking" if broken else ""
    (textures/f"unstable_void_bricks{suffix}.png").write_bytes(sprite(broken))

def dump(rel,payload):
    f=RES/rel
    f.parent.mkdir(parents=True,exist_ok=True)
    f.write_text(json.dumps(payload,indent=2)+"\n",encoding="utf8")

for cracking in (False,True):
    name="unstable_void_bricks"+("_cracking" if cracking else "")
    dump(f"models/block/{name}.json",{"parent":"minecraft:block/cube_all",
        "textures":{"all":f"pocketrealms:block/{name}"}})
dump("blockstates/unstable_void_bricks.json",{"variants":{
    "cracking=false":{"model":"pocketrealms:block/unstable_void_bricks"},
    "cracking=true":{"model":"pocketrealms:block/unstable_void_bricks_cracking"}})
dump("models/item/unstable_void_bricks.json",{"parent":"pocketrealms:block/unstable_void_bricks"})

properties=ROOT/"gradle.properties"
patch(properties,"mod_version=0.6.2","mod_version=0.6.3")

with (ROOT/"README.md").open("a",encoding="utf8") as f:
    f.write("""
\n## Pocket Realms 0.6.3 - Last Sanctuary
- Enlarged grid 49x49 and boss-ready 19x19 sanctuary in the distant quadrant.
- Three broad sanctuary entrances, patterned arena floor, four gothic supports,
  reward chest and exit portal, NO boss spawned yet.
- Sanctuary and access ring permanently excluded from timed and following collapse.
- Every corridor floor deletion checked against a real live player-to-exit route.
- Up to 32 procedurally drilled, noncritical open holes and 60 unstable brick traps.
- Purple-fissured 32x32 unstable bricks use real server timed destruction:
  stepping turns on the bright cracking blockstate, then drops away after 52 ticks.
- Dread Director adds directional behind-player horror audio, restrained darkness,
  illusionary footfalls, souls and heartbeat cues while outside sanctuary.
- Pocket Door broadcasts real PORTAL and REVERSE_PORTAL particle effects every 4 ticks.
- Java 21, NeoForge 1.21.1; no additional required dependencies.
""")
print("Pocket Realms v0.6.3 overlay complete, Java+sprites updated.")
