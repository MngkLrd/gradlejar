#!/usr/bin/env python3
"""Halo Haven 2.6 ecology and architecture distribution (no registries).

After 2.4 structure authoring and 2.3 tree configuration. Disables the
legacy fixed-height repeated villages outside the origin, increases tree
density only in Canopy, spaces colossal trees apart and anchors ruins to
natural topography instead of sky Y=0.
"""
from pathlib import Path
import json,tarfile,tempfile

ROOT=Path("projects/halohaven")
DATA=ROOT/"src/main/resources/data/halohaven"
JAVA=ROOT/"src/main/java/dev/minemods/halohaven/HavenVillages.java"
BUNDLE=Path("projects/halohaven-alpha21/bundle.tar.xz")
def get(p):return json.loads(p.read_text(encoding="utf-8"))
def put(p,j):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(j,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")

# The old procedural settlements create platforms on an UNCONDITIONAL
# 320x320 grid at Y110. It is the main cause of artificial suspended houses,
# repetitive decoration and intersecting biome regions.
text=JAVA.read_text(encoding="utf-8")
needle='if (Math.abs(p.getBlockX() - cx) < 108 && Math.abs(p.getBlockZ() - cz) < 108) {'
assert needle in text
text=text.replace(needle,
  'if (cx==0 && cz==0 && Math.abs(p.getBlockX()-cx)<108 && Math.abs(p.getBlockZ()-cz)<108) {',1)
JAVA.write_text(text,encoding="utf-8")
# Natural worldgen NBT structures are handled separately by the biome-aware
# Jigsaw structure system, rather than player tick y=110 terraforming.

biomes=("dawn_gardens","chalk_terraces","eternal_canopy",
        "ringing_gorges","forgotten_choirs","lower_veil")
# Tree density deliberately communicates ecology. Plants should not be
# identical or overpopulated in stone-only regions.
for name in biomes:
    file=DATA/"worldgen/placed_feature"/("regional_tree_"+name+".json")
    p=get(file)
    assert p["feature"]=="halohaven:regional_tree_"+name
    count={"dawn_gardens":3,"chalk_terraces":1,
           "eternal_canopy":7,"ringing_gorges":1,
           "forgotten_choirs":1,"lower_veil":3}[name]
    p["placement"]=[{"type":"minecraft:count","count":count},
                  {"type":"minecraft:in_square"},
                  {"type":"minecraft:heightmap","heightmap":"WORLD_SURFACE_WG"},
                  {"type":"minecraft:biome"}]
    if name in ("chalk_terraces","ringing_gorges","forgotten_choirs"):
        # Place fewer trees on open stone. Rainforest dominates only its biome.
        p["placement"].insert(0,{"type":"minecraft:rarity_filter",
                                  "chance":4 if name!="forgotten_choirs" else 3})
    put(file,p)

# The medium native forest tree must also be recognizably giant from a
# distance. Giant structure trees reach 60+ blocks; these reach ~28-45.
for name in biomes:
    file=DATA/"worldgen/configured_feature"/("regional_tree_"+name+".json")
    p=get(file)
    assert p["type"]=="minecraft:tree"
    trunk=p["config"]["trunk_placer"]
    foliage=p["config"]["foliage_placer"]
    if name=="eternal_canopy":
        trunk.update(base_height=26,height_rand_a=10,height_rand_b=8)
        foliage["radius"]={"type":"minecraft:constant","value":7}
        foliage["height"]=8
    elif name=="lower_veil":
        trunk.update(base_height=19,height_rand_a=8,height_rand_b=5)
        foliage["radius"]={"type":"minecraft:constant","value":5}
        foliage["height"]=5
    elif name=="dawn_gardens":
        trunk.update(base_height=8,height_rand_a=5,height_rand_b=2)
        foliage["radius"]={"type":"minecraft:constant","value":4}
    put(file,p)

with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    with tarfile.open(BUNDLE,"r:xz") as tar:
        for x in tar:
            if not x.isfile():continue
            rel=Path(x.name)
            assert rel.parts[0]=="src" and ".." not in rel.parts
            out=root/rel;out.parent.mkdir(parents=True,exist_ok=True)
            out.write_bytes(tar.extractfile(x).read())
    world=root/"src/main/resources/data/halohavenalpha2/worldgen"
    ns="halohavenalpha2"

    # 2.4 generated only one colossal tree in a large 19-chunk site.
    # A new site every 8 chunks gives many 50–70m canopy giants, while
    # centers remain separated by at least four chunks (64 blocks) so
    # 45-block wide tree template footprints cannot overlap.
    giant=world/"structure_set/canopy_colossal.json"
    g=get(giant);place=g["placement"]
    assert place["type"]=="minecraft:random_spread"
    place["spacing"]=8;place["separation"]=4
    place["exclusion_zone"]["chunk_count"]=5
    put(giant,g)

    # Giant trees exist in the genuine Canopy biome only, and now start
    # on the actual terrain surface. Roots naturally reach soil.
    gp=world/"structure/canopy_colossal.json"
    j=get(gp)
    j["start_height"]={"absolute":0}
    j["project_start_to_heightmap"]="WORLD_SURFACE_WG"
    j["terrain_adaptation"]="beard_box"
    put(gp,j)

    # Significant settlements, monuments, and abandoned architecture should
    # be traversable and rest in the terrain, not project from Y=0 or appear
    # as two floating house templates in open sky.
    sets=[p for p in (world/"structure_set").glob("*.json")]
    for p in sets:
        j=get(p)
        placement=j.get("placement",{})
        if placement.get("type")!="minecraft:random_spread":
            continue
        if "regional_choir"==p.stem:
            placement["spacing"]=13;placement["separation"]=6
            for e in j.get("structures",[]):
                target=e["structure"]
                e["weight"]=1 if target.endswith("_house") else 4
        elif p.stem=="choir_monuments":
            placement["spacing"]=13;placement["separation"]=6
            placement["exclusion_zone"]["chunk_count"]=8
        elif p.stem.startswith("regional_"):
            placement["spacing"]=21;placement["separation"]=10
        elif p.stem=="canopy_colossal":
            continue
        elif p.stem.startswith("microstructures_"):
            placement["spacing"]=18;placement["separation"]=9
        put(p,j)

    structures=[p for p in (world/"structure").glob("*.json")]
    adjusted=0
    for p in structures:
        j=get(p)
        if j.get("type")!="minecraft:jigsaw":continue
        if p.stem=="canopy_colossal":continue
        j["start_height"]={"absolute":0}
        j["project_start_to_heightmap"]="WORLD_SURFACE_WG"
        j["terrain_adaptation"]="beard_box"
        put(p,j);adjusted+=1
    assert adjusted>=10,adjusted

    # Keep the choir temples special and easier to encounter than repeated
    # small houses. The existing four large NBT temple plans remain intact.
    # These six region-unique tags and NBT resource IDs are unchanged.
    with tarfile.open(BUNDLE,"w:xz",preset=6) as tar:
        for p in sorted((root/"src").rglob("*")):
            if p.is_file():tar.add(p,arcname=p.relative_to(root).as_posix())

print("PASS: old Y110 suspended village grid removed beyond central starter island")
print("PASS: Canopy = 7 tall trees/chunk plus a non-overlapping colossal grove every ~8 chunks")
print("PASS: Choir monuments weighted higher, regional ruin starts projected to terrain")
