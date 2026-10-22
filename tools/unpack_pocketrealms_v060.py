"""Restore Pocket Realms 0.6.0 hand-built architectural rooms and block textures."""
from pathlib import Path
import base64,hashlib,io,tarfile,lzma,subprocess,json
repo=Path(__file__).resolve().parents[1]
root=repo/"projects/pocketrealms"
parts=[root/f"upgrade-v060-{i:02d}.b64" for i in range(4)]
raw=base64.b64decode("".join(p.read_text("ascii") for p in parts),validate=True)
sha=hashlib.sha256(raw).hexdigest()
if sha!="bdd801ce1aa6042976f8cda55a456e69aea17ba826390a5f8bc2742005cb0af8":
    raise SystemExit("Pocket Realms room archive failed SHA256: "+sha)
patch=None
with tarfile.open(fileobj=io.BytesIO(lzma.decompress(raw)),mode="r:") as arc:
    for m in arc:
        name=Path(m.name)
        if not m.isfile() or name.is_absolute() or ".." in name.parts:
            raise SystemExit("Invalid archive path: "+m.name)
        with arc.extractfile(m) as fh:contents=fh.read()
        if m.name=="rooms060.patch":
            patch=contents
        else:
            dst=root/name
            dst.parent.mkdir(parents=True,exist_ok=True)
            dst.write_bytes(contents)
if patch is None:raise SystemExit("Missing generator patch")
check=subprocess.run(["patch","--dry-run","--fuzz=0","-p1"],input=patch,cwd=root,capture_output=True)
if check.returncode:
    raise SystemExit("Room generator source conflict:\n"+check.stdout.decode()+check.stderr.decode())
apply=subprocess.run(["patch","--fuzz=0","-p1"],input=patch,cwd=root,capture_output=True)
if apply.returncode:raise SystemExit(apply.stdout.decode()+apply.stderr.decode())
blocks=root/"src/main/java/com/kirill/pocketrealms/registry/ModBlocks.java"
s=blocks.read_text("utf8")
needle="    public static final DeferredBlock<Block> CORRUPTION_NEST = BLOCKS.register("
registrations="""    public static final DeferredBlock<Block> ASHEN_TILES = BLOCKS.register("ashen_tiles",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_TILES).strength(10.0F, 1200.0F)));
    public static final DeferredBlock<Block> RIFT_CARVED_STONE = BLOCKS.register("rift_carved_stone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE_BRICKS).strength(10.0F, 1200.0F)));
    public static final DeferredBlock<Block> RIBBED_VOID_PILLAR = BLOCKS.register("ribbed_void_pillar",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE_BRICKS).strength(10.0F, 1200.0F)));
    public static final DeferredBlock<Block> RITUAL_MOSAIC = BLOCKS.register("ritual_mosaic",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).strength(10.0F, 1200.0F)));
"""
if s.count(needle)!=1:raise SystemExit("Cannot register new room stone blocks")
blocks.write_text(s.replace(needle,registrations+needle),encoding="utf8")
props=root/"gradle.properties"
text=props.read_text("utf8")
if text.count("mod_version=0.5.3")!=1:raise SystemExit("Unexpected old version")
props.write_text(text.replace("mod_version=0.5.3","mod_version=0.6.0"),encoding="utf8")
langroot=root/"src/main/resources/assets/pocketrealms/lang"
for locale,names in [
    ("en_us",["Ashen Tiles","Rift-Carved Stone","Ribbed Void Pillar","Ritual Mosaic"]),
    ("ru_ru",["Пепельные плиты","Резной рифт-камень","Ребристая колонна Бездны","Ритуальная мозаика"])
]:
    path=langroot/(locale+".json")
    j=json.loads(path.read_text("utf8"))
    for key,value in zip(["ashen_tiles","rift_carved_stone","ribbed_void_pillar","ritual_mosaic"],names):
        j["block.pocketrealms."+key]=value
    path.write_text(json.dumps(j,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
with (root/"README.md").open("a",encoding="utf8") as readme:
    readme.write("""
\n## Pocket Realms 0.6.0 — Real Chambers
- 39x39 labyrinth with five-to-six enclosed rooms instead of corridor blobs.
- Six themes: Ossuary, Ritual, Archive, Prison, Forge, Cistern.
- Stone arches, 2 open doorways, support columns, furniture, occasional minor loot barrels.
- Four custom 16x16 pixel-art structural textures.
- Ring corridor around each room; shortest-wall graph repair ensures every open cell remains reachable without opening room walls.
- Exit chamber keeps distance from custom rooms; shifting walls never erase room borders.
- One shared pocket dimension and old server-side gameplay rules remain.
""")
print("Pocket Realms 0.6.0 complete overlay installed, SHA256="+sha)
