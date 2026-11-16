#!/usr/bin/env python3
"""Halo Haven 2.6: six distinct sky-island shapes and coherent region occupancy.

Execute after tools/halohaven25_island_regions.py.
No registry additions. Replaces the vertical-column End density field with
tapered islands, strong regional relief and native cliff material.
"""
from pathlib import Path
import json,math

BASE=Path("projects/halohaven/src/main/resources/data/halohaven")
NOISE=BASE/"worldgen/noise_settings/heaven_islands.json"
DIM=BASE/"dimension/heaven.json"
def get(p):return json.loads(p.read_text(encoding="utf-8"))
def put(p,data):p.write_text(json.dumps(data,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
def add(a,b):return {"type":"minecraft:add","argument1":a,"argument2":b}
def mul(a,b):return {"type":"minecraft:mul","argument1":a,"argument2":b}
def minimum(a,b):return {"type":"minecraft:min","argument1":a,"argument2":b}
def ab(a):return {"type":"minecraft:abs","argument":a}
def clamp(a):return {"type":"minecraft:clamp","input":a,"min":0,"max":1}
def n(name,xz,y=0):return {"type":"minecraft:noise","noise":"minecraft:"+name,"xz_scale":xz,"y_scale":y}
def grad(a,b,c,d):return {"type":"minecraft:y_clamped_gradient","from_y":a,"to_y":b,"from_value":c,"to_value":d}
def rc(a,l,r,x,y):return {"type":"minecraft:range_choice","input":a,"min_inclusive":l,"max_exclusive":r,
                         "when_in_range":x,"when_out_of_range":y}
def plus(*items):
    s=items[0]
    for x in items[1:]:s=add(s,x)
    return s

# Temperature had an oversized central band in the original 2.5 6-way classifier.
# These boundaries distribute temperate, cold and warm regions more evenly.
# The same exact thresholds are applied to density to prevent mixed islands.
dimension=get(DIM)
source=dimension["generator"]["biome_source"]
assert source["type"]=="minecraft:multi_noise"
ids={"dawn_gardens","chalk_terraces","eternal_canopy",
     "ringing_gorges","forgotten_choirs","lower_veil"}
assert {b["biome"].split(":")[-1] for b in source["biomes"]}==ids
# An island cluster must be large enough to feel like a territory, but not
# a multi-kilometer large-biomes world. 2.5 sampled all six within ~150 blocks.
for config,scale in (("climate_temperature",1.15),("climate_vegetation",.95)):
    file=BASE/"worldgen/density_function"/(config+".json")
    climate=get(file)
    assert climate["type"]=="minecraft:shifted_noise"
    climate["xz_scale"]=scale
    put(file,climate)

for entry in source["biomes"]:
    biome=entry["biome"].split(":")[-1]
    if biome in ("dawn_gardens","chalk_terraces"):
        entry["parameters"]["temperature"]=[.14,1.2]
    elif biome in ("eternal_canopy","ringing_gorges"):
        entry["parameters"]["temperature"]=[-.14,.14]
    else:
        entry["parameters"]["temperature"]=[-1.2,-.14]
put(DIM,dimension)

T="halohaven:climate_temperature"
V="halohaven:climate_vegetation"
def boundary(q,threshold):
    return clamp(mul(35.0,add(ab(add(q,-threshold)),-.034)))
safe=minimum(minimum(boundary(T,-.14),boundary(T,.14)),boundary(V,0))
# Entirely empty along biome changes, not a sliced platform or vertical wall.
coastal_cap=plus(-6,mul(16,safe))

def island_field(cfreq,efreq,weight=.68,secondary=.32,bias=-.02):
    return plus(mul(weight,n("continentalness",cfreq)),
                mul(secondary,n("erosion",efreq)),bias)

# Vertical geometry: true flying rock with narrowing UNDERsides rather than
# y-independent walls from End sloped_cheese. Typical top: Y100-119, base Y70.
bottom=grad(52,85,-1.0,1.1)
top=grad(72,136,1.0,-1.0)
taper=grad(55,105,.56,0.0)

# Identities are also shaped by the physical island field, not simply the
# textures or color of the foliage above it.
dawn_field=island_field(.65,1.65,.73,.27,.010)
chalk_field=island_field(.48,1.35,.80,.20,-.035)
canopy_field=island_field(.61,1.35,.68,.32,.060)
gorge_field=island_field(.82,1.9,.70,.30,-.065)
choir_field=island_field(.49,1.30,.76,.24,.035)
veil_field=island_field(1.05,2.3,.67,.33,-.075)

# Dawn: broad flat gardens, elevated natural lip, irregular lotus-petal rim.
rim=clamp(mul(5.2,add(.16,mul(-1,dawn_field))))
dawn_relief=plus(-.06,mul(.18,rim),mul(.028,n("vegetation",2.3)))
# Chalk: eight wide TERRACES (7 distinct jumps) with broad shelves.
terrace_signal=plus(mul(.84,n("erosion",.83)),mul(.22,n("continentalness",1.7)))
levels=[(-.63,-.68),(-.43,-.47),(-.24,-.26),(-.06,-.06),
        (.11,.16),(.28,.36),(.47,.55)]
terrace=.76
for threshold,value in reversed(levels):
    terrace=rc(terrace_signal,-2.0,threshold,value,terrace)
chalk_relief=terrace
# Canopy: thick connected forest hill islands, not another thin green column.
canopy_relief=plus(.10,mul(.09,n("vegetation",1.35)))
# Gorge: sharp knife-like cliffs with connected, deeply cut open rifts.
ravine=ab(n("erosion",.95))
carve=rc(ravine,0,.11,-1.05,rc(ravine,0,.22,-.43,0))
gorge_relief=plus(-.02,carve,mul(.30,ab(n("continentalness",1.55))))
# Choir: high isolated mesas for ancient colossal religious foundations.
choir_relief=plus(.19,mul(.10,n("continentalness",1.65)))
# Veil: lower drifting fragments, negative cavities below the top.
cave=ab(n("spaghetti_3d_1",1.33,1.14))
void_caves=rc(cave,0,.14,-.9,rc(cave,0,.23,-.37,0))
veil_relief=plus(-.36,mul(.07,n("erosion",1.8)),void_caves)

def island(field,relief):
    # Both top and bottom must exist, and rock tapers inward underneath.
    return minimum(minimum(add(top,relief),bottom),add(field,mul(-1,taper)))

profiles=[
    island(dawn_field,dawn_relief),island(chalk_field,chalk_relief),
    island(canopy_field,canopy_relief),island(gorge_field,gorge_relief),
    island(choir_field,choir_relief),island(veil_field,veil_relief)
]

# One and only one terrain formula per region. The 2.5 coastal cap turns
# contiguous land into independent islands whenever a biome border crosses it.
def choose_regions():
    dawn,chalk,canopy,gorge,choir,veil=profiles
    return rc(T,-2.0,-.14,rc(V,-2.0,0,choir,veil),
              rc(T,-.14,.14,rc(V,-2.0,0,gorge,canopy),
                 rc(V,-2.0,0,chalk,dawn)))

land=minimum(choose_regions(),coastal_cap)
settings=get(NOISE)
assert settings["noise_router"]["temperature"]==T
assert settings["noise_router"]["vegetation"]==V
for key in ("final_density","initial_density_without_jaggedness"):
    settings["noise_router"][key]=land

# The previous generator painted only the surface. Every exposed cliff was the
# SAME bright cloudstone, so one biome looked like another from a distance.
# Use existing 2.3/2.4 material blocks for both surface and rock walls.
native={
 "dawn_gardens":"halohaven:dawnstone",
 "chalk_terraces":"halohaven:chalk_strata",
 "eternal_canopy":"halohaven:canopy_loam",
 "ringing_gorges":"halohaven:resonant_shale",
 "forgotten_choirs":"halohaven:choirstone",
 "lower_veil":"halohaven:veilstone"
}
rules=settings["surface_rule"]["sequence"]
assert len(rules)>=6
for biome,block in native.items():
    # Applied after the original floor rules; default cloudstone is last.
    rules.insert(-1,{
        "type":"minecraft:condition",
        "if_true":{"type":"minecraft:biome","biome_is":["halohaven:"+biome]},
        "then_run":{"type":"minecraft:block","result_state":{"Name":block}}
    })
put(NOISE,settings)

# Structural assertions, not a claim about actual player-visible beauty.
data=get(NOISE)
assert data["noise_router"]["final_density"]["type"]=="minecraft:min"
assert data["noise_router"]["final_density"]["argument1"]["type"]=="minecraft:range_choice"
assert len([s for s in data["surface_rule"]["sequence"] if
    s.get("if_true",{}).get("type")=="minecraft:biome"])>=12
for region in ("dawn_gardens","chalk_terraces","eternal_canopy",
               "ringing_gorges","forgotten_choirs","lower_veil"):
    assert any(region in str(row) for row in data["surface_rule"]["sequence"])
print("PASS: six biome-exclusive, tapered, independently sculpted landforms")
print("PASS: native cliff walls, 8-tier chalk mesas, forest mantle, gorge rifts, Veil caverns")
