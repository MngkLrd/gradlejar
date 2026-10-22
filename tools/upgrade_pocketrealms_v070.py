"""Pocket Realms 0.7.0 Stage II integration, after v0.6.4."""
from pathlib import Path
import json,random,struct,zlib
repo=Path(__file__).resolve().parents[1]
root=repo/"projects/pocketrealms"
java=root/"src/main/java/com/kirill/pocketrealms"
assets=root/"src/main/resources/assets/pocketrealms"
stage=repo/"tools/pocketrealms_v070"
def patch(path,needle,replacement):
    text=path.read_text("utf8")
    if text.count(needle)!=1:
        raise SystemExit(f"v070 patch anchor {path}: count {text.count(needle)}; {needle!r}")
    path.write_text(text.replace(needle,replacement),encoding="utf8")
for file in sorted(stage.rglob("*.java")):
    target=java/file.relative_to(stage)
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_bytes(file.read_bytes())
    print("V070",file.relative_to(stage))

blocks=java/"registry/ModBlocks.java"
patch(blocks,"import com.kirill.pocketrealms.block.CrumblingStoneBlock;",
             "import com.kirill.pocketrealms.block.CrumblingStoneBlock;\nimport com.kirill.pocketrealms.block.DescentGateBlock;")
patch(blocks,'    public static final DeferredBlock<Block> RETURN_PORTAL = BLOCKS.register("return_portal",',
    '''    public static final DeferredBlock<Block> DESCENT_GATE = BLOCKS.register("descent_gate",
            () -> new DescentGateBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRYING_OBSIDIAN)
                    .strength(-1.0F,3600000F).lightLevel(s -> 13).noCollission().noOcclusion()));
    public static final DeferredBlock<Block> RETURN_PORTAL = BLOCKS.register("return_portal",''')

entities=java/"registry/ModEntities.java"
patch(entities,"import com.kirill.pocketrealms.entity.VeilGhost;",
    """import com.kirill.pocketrealms.entity.VeilGhost;
import com.kirill.pocketrealms.entity.Knocker;
import com.kirill.pocketrealms.entity.CeilingLurker;
import com.kirill.pocketrealms.entity.ChoirWraith;""")
patch(entities,'    public static final Supplier<EntityType<VoidPursuer>> VOID_PURSUER = ENTITIES.register(',
    '''    public static final Supplier<EntityType<Knocker>> KNOCKER = ENTITIES.register(
            "knocker", () -> EntityType.Builder.of(Knocker::new,MobCategory.MONSTER)
                    .sized(1.45F,1.85F).clientTrackingRange(12).build("knocker"));
    public static final Supplier<EntityType<CeilingLurker>> CEILING_LURKER = ENTITIES.register(
            "ceiling_lurker", () -> EntityType.Builder.of(CeilingLurker::new,MobCategory.MONSTER)
                    .sized(.95F,.45F).clientTrackingRange(12).build("ceiling_lurker"));
    public static final Supplier<EntityType<ChoirWraith>> CHOIR_WRAITH = ENTITIES.register(
            "choir_wraith", () -> EntityType.Builder.of(ChoirWraith::new,MobCategory.MONSTER)
                    .sized(.62F,2.45F).clientTrackingRange(14).build("choir_wraith"));
    public static final Supplier<EntityType<VoidPursuer>> VOID_PURSUER = ENTITIES.register(''')

attrib=java/"registry/ModEntityAttributes.java"
patch(attrib,"import com.kirill.pocketrealms.entity.VeilGhost;",
    """import com.kirill.pocketrealms.entity.VeilGhost;
import com.kirill.pocketrealms.entity.Knocker;
import com.kirill.pocketrealms.entity.CeilingLurker;
import com.kirill.pocketrealms.entity.ChoirWraith;""")
patch(attrib,"        event.put(ModEntities.VEIL_HOUND.get(), VeilHound.createAttributes().build());",
    """        event.put(ModEntities.KNOCKER.get(),Knocker.attributes().build());
        event.put(ModEntities.CEILING_LURKER.get(),CeilingLurker.attributes().build());
        event.put(ModEntities.CHOIR_WRAITH.get(),ChoirWraith.attributes().build());
        event.put(ModEntities.VEIL_HOUND.get(), VeilHound.createAttributes().build());""")

for loc,strings in {
 "en_us":{"block.pocketrealms.descent_gate":"Descent Gate","entity.pocketrealms.knocker":"The Knocker",
          "entity.pocketrealms.ceiling_lurker":"Ceiling Lurker","entity.pocketrealms.choir_wraith":"Choir Wraith"},
 "ru_ru":{"block.pocketrealms.descent_gate":"Врата Спуска","entity.pocketrealms.knocker":"Таран",
          "entity.pocketrealms.ceiling_lurker":"Потолочный охотник","entity.pocketrealms.choir_wraith":"Певчий Бездны"}
}.items():
    path=assets/"lang"/(loc+".json")
    d=json.loads(path.read_text("utf8"));d.update(strings)
    path.write_text(json.dumps(d,ensure_ascii=False,indent=2)+"\n",encoding="utf8")

def png(rows):
    h=len(rows);w=len(rows[0])
    raw=b"".join(b"\x00"+b"".join(bytes(c) for c in row) for row in rows)
    def chunk(t,d):
        return struct.pack(">I",len(d))+t+d+struct.pack(">I",zlib.crc32(t+d)&0xffffffff)
    return b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",w,h,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(raw,9))+chunk(b"IEND",b"")
skins=[
    ("knocker",701,(35,30,41),(78,65,79),(190,103,70)),
    ("ceiling_lurker",702,(23,29,37),(74,70,96),(151,200,186)),
    ("choir_wraith",703,(27,28,45),(91,66,117),(179,98,208))
]
texdir=assets/"textures/entity";texdir.mkdir(parents=True,exist_ok=True)
for name,seed,dark,mid,glow in skins:
    r=random.Random(seed)
    rows=[]
    for y in range(128):
        line=[]
        for x in range(128):
            phase=((x//4)*7+(y//4)*11+r.randrange(3))%17
            rgb=glow if phase==0 else (mid if phase<6 else dark)
            line.append((*rgb,255))
        rows.append(line)
    for y in range(15,20):
        for x in (30,31,40,41):
            rows[y][x]=(*glow,255)
    for y in range(24,28):
        for x in range(27,45):
            rows[y][x]=(*dark,255)
    (texdir/f"{name}.png").write_bytes(png(rows))

blocksdir=assets/"textures/block";blocksdir.mkdir(parents=True,exist_ok=True)
colors=((31,26,48,255),(54,40,76,255),(80,43,105,255),(133,69,162,255))
rows=[]
for y in range(32):
    row=[]
    for x in range(32):
        d=min(abs(x-15),abs(y-15))
        ring=abs(x-15)+abs(y-15)
        k=3 if ring in (8,9,15,16,22,23) else (2 if d<4 else (1 if ((x//3+y//3)%3==0) else 0))
        row.append(colors[k])
    rows.append(row)
(blocksdir/"descent_gate.png").write_bytes(png(rows))
def dump(rel,obj):
    path=assets/rel;path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(obj,indent=2)+"\n",encoding="utf8")
dump("models/block/descent_gate.json",{"parent":"minecraft:block/cube_all",
     "textures":{"all":"pocketrealms:block/descent_gate"}})
dump("blockstates/descent_gate.json",{"variants":{"":{"model":"pocketrealms:block/descent_gate"}}})

patch(root/"gradle.properties","mod_version=0.6.4","mod_version=0.7.0")
with (root/"README.md").open("a",encoding="utf8") as f:
    f.write("""
\n## Pocket Realms 0.7.0 - The Descent
- Stage I -> optional Stage II Hollow Depths through an in-world Descent Gate.
- Old Return Portal works. Stage II occupies a different reusable sector; stage NBT persists.
- Stage II is sculk-themed, has increased holes, more unstable bricks, shorter collapse timer,
  stronger terminal rewards, and three new stage-exclusive monster types.
- Knocker: 1.45 x 1.85, wide bruiser with line-of-sight charge and vulnerable recovery.
- Ceiling Lurker: .95 x .45, ceiling scuttle, dive, short Slowness, withdrawal.
- Choir Wraith: .62 x 2.45, hovering, audible wind-up, LOS-based sonic cone.
- Distinct complex custom 3D geometries and 128x128 pixel-cluster UV materials.
- Minecraft 1.21.1 NeoForge 21.1.x, Java 21.
""")
print("PASS Pocket Realms 0.7.0 layered stage and entity sources applied")
