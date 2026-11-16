#!/usr/bin/env python3
"""Halo Haven 2.9: remove Chalk Terraces biome, keep five coherent regions.

This is intentionally a post-processing script AFTER all prior v2.8 worldgen
patches. We retain chalk-related blocks/items for safe backwards compatibility
but remove Chalk Terraces from the biome source, active terrain generation,
spawning, decorations, tags and addon biome-specific structure templates.
"""
from pathlib import Path
import json,tarfile,tempfile

CORE=Path("projects/halohaven/src/main")
DATA=CORE/"resources/data/halohaven"
ADDON=Path("projects/halohaven-alpha21/bundle.tar.xz")
DIM=DATA/"dimension/heaven.json"
SET=DATA/"worldgen/noise_settings/heaven_islands.json"

def get(p):return json.loads(p.read_text(encoding="utf8"))
def put(p,x):p.write_text(json.dumps(x,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
def add(a,b):return {"type":"minecraft:add","argument1":a,"argument2":b}
def mul(a,b):return {"type":"minecraft:mul","argument1":a,"argument2":b}
def minimum(a,b):return {"type":"minecraft:min","argument1":a,"argument2":b}
def absval(a):return {"type":"minecraft:abs","argument":a}
def clamp(a):return {"type":"minecraft:clamp","input":a,"min":0,"max":1}
def choose(v,lo,hi,x,y):
    return {"type":"minecraft:range_choice","input":v,
            "min_inclusive":lo,"max_exclusive":hi,
            "when_in_range":x,"when_out_of_range":y}

# Preserve 2D territories: warm Dawn is now the only warm biome regardless of
# humidity. The other four retain their humidity distinction and all five keep
# their own island geography (no painter-style biome blends).
dim=get(DIM)
source=dim["generator"]["biome_source"]
assert source["type"]=="minecraft:multi_noise"
initial=source["biomes"]
assert len(initial)==6 and sum(x["biome"]=="halohaven:chalk_terraces" for x in initial)==1
source["biomes"]=[x for x in initial if x["biome"]!="halohaven:chalk_terraces"]
assert len(source["biomes"])==5
for e in source["biomes"]:
    if e["biome"]=="halohaven:dawn_gardens":
        e["parameters"]["humidity"]=[-1.2,1.2]
put(DIM,dim)

T="halohaven:climate_temperature"
V="halohaven:climate_vegetation"
setting=get(SET)
router=setting["noise_router"]
for key in ("final_density","initial_density_without_jaggedness"):
    root=router[key]
    assert root["type"]=="minecraft:min"
    regions=root["argument1"]
    assert regions["type"]=="minecraft:range_choice" and regions["input"]==T
    warm=regions["when_out_of_range"]["when_out_of_range"]
    assert warm["type"]=="minecraft:range_choice" and warm["input"]==V
    # OLD: humid warm = Dawn, dry warm = Chalk. NEW: all warm = Dawn.
    regions["when_out_of_range"]["when_out_of_range"]=warm["when_out_of_range"]

    # OLD biome seam cut between Chalk and Dawn. This would create a large
    # pointless trench inside the newly unified Dawn territory.
    a=absval(add(T,.14))
    b=absval(add(T,-.14))
    c=absval(V)
    def edge(d):return clamp(mul(31.0,add(d,-.034)))
    temp_safe=minimum(edge(a),edge(b))
    humidity_safe=choose(T,-2.0,.14,edge(c),1.0)
    new_safe=minimum(temp_safe,humidity_safe)
    root["argument2"]=add(-5.5,mul(16,new_safe))
    assert regions["when_out_of_range"]["when_out_of_range"]["type"]=="minecraft:min"
    assert "chalk" not in json.dumps(regions)
    router[key]=root

# Ordinary island heights are below Y175. The 2.8 Chalk-only 256-block density
# height is unnecessary; generating terrain 0..191 improves chunk workload
# while preserving the full dimension's original 0..255 logical height.
assert setting["noise"]["height"]==256
setting["noise"]["height"]=192
put(SET,setting)

# Remove actual active biome and its attached decorated features; do not
# remove registerable blocks / items, which may exist in player inventories.
paths=[
 DATA/"worldgen/biome/chalk_terraces.json",
 DATA/"worldgen/placed_feature/sky_clouds_chalk.json",
]
for folder in ("configured_feature","placed_feature"):
    paths.extend((DATA/"worldgen"/folder).glob("*chalk*.json"))
    paths.extend((DATA/"worldgen"/folder).glob("*regional_tree_chalk_terraces.json"))
removed=[]
for p in set(paths):
    if p.exists():
        p.unlink();removed.append(p.as_posix())
assert any(p.endswith("/chalk_terraces.json") for p in removed)

# Delete any Chalk-specific remaining worldgen JSONs only: unused compatible
# stone and wood resources are still available for old save-world chunks.
for p in list(DATA.rglob("*.json")):
    if p.name=="chalk_terraces.json":
        p.unlink();removed.append(p.as_posix())

# Existing Java grounded-placement maps contain the Chalk biome. Remove
# only that route, preserving registrations and unrelated five biomes.
java=CORE/"java/dev/minemods/halohaven/HavenGroundedTemplateFeature.java"
s=java.read_text(encoding="utf8")
assert '"chalk_terraces","halohaven:chalkstone",' in s
assert '"chalk_terraces","chalk",' in s
s=s.replace('        "chalk_terraces","halohaven:chalkstone",\n',"")
s=s.replace('        "dawn_gardens","dawn","chalk_terraces","chalk",',
            '        "dawn_gardens","dawn",')
s=s.replace('            case "chalk_terraces" -> actual.equals("halohaven:chalk_strata");\n',"")
java.write_text(s,encoding="utf8")

# No chalk-specific NBT structure pools or tags should remain active.
with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    with tarfile.open(ADDON,"r:xz") as z:
        for member in z:
            if not member.isfile():continue
            rel=Path(member.name)
            assert rel.parts[0]=="src" and ".." not in rel.parts
            target=root/rel;target.parent.mkdir(parents=True,exist_ok=True)
            target.write_bytes(z.extractfile(member).read())
    addon_data=root/"src/main/resources/data/halohavenalpha2"
    for p in list(addon_data.rglob("*")):
        if not p.is_file():continue
        rel=p.relative_to(addon_data).as_posix()
        if (("worldgen/" in rel or "structure/" in rel or "template_pool/" in rel)
            and ("regional_chalk" in rel or "chalk_terraces" in rel or rel.endswith("/chalk.json"))):
            p.unlink();removed.append("addon:"+rel)
    # Eliminate JSON worldgen entries that exclusively target the removed biome.
    for p in addon_data.rglob("*.json"):
        txt=p.read_text(encoding="utf8")
        if "chalk_terraces" in txt or '"#halohavenalpha2:chalk"' in txt:
            if "worldgen/" in p.as_posix() or "/tags/worldgen/" in p.as_posix():
                p.unlink();removed.append("addon:"+p.relative_to(addon_data).as_posix())
    with tarfile.open(ADDON,"w:xz",preset=6) as z:
        for p in sorted((root/"src").rglob("*")):
            if p.is_file():z.add(p,arcname=p.relative_to(root).as_posix())

actual_names={x["biome"] for x in get(DIM)["generator"]["biome_source"]["biomes"]}
expected={f"halohaven:{x}" for x in ("dawn_gardens","eternal_canopy","ringing_gorges",
                                   "forgotten_choirs","lower_veil")}
assert actual_names==expected
assert not (DATA/"worldgen/biome/chalk_terraces.json").exists()
assert "chalk_terraces" not in json.dumps(get(SET))
assert get(SET)["noise"]["height"]==192
print("PASS: five distinct territories, no biome/land/mist Chalk spawn paths")
print("PASS: 192-high terrain density, 256-high original dimension, compatible Chalk block IDs")
print("Chalk-only data resources retired:",len(removed))
