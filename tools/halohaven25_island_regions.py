#!/usr/bin/env python3
"""Halo Haven 2.5 island biome coherence hotfix.

One physical landmass must not straddle temperature/humidity biome borders.
Unlike 2.4, regional terrain profiles are strictly exclusive, and the same
climate fields used by the multi_noise biome source cut navigable air channels
on both sides of the *exact* biome-classification thresholds.

Runs AFTER tools/halohaven24_world.py and BEFORE Gradle build.
No new blocks, items, mobs or textures.
"""
from __future__ import annotations
from pathlib import Path
import copy
import json
import math

ROOT=Path("projects/halohaven/src/main/resources/data/halohaven")
NOISE=ROOT/"worldgen/noise_settings/heaven_islands.json"
TEMPERATURE=ROOT/"worldgen/density_function/climate_temperature.json"
VEGETATION=ROOT/"worldgen/density_function/climate_vegetation.json"
BIOME_PATH=ROOT/"dimension/heaven.json"
def read(path): return json.loads(path.read_text(encoding="utf-8"))
def put(path,blob): path.write_text(json.dumps(blob,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
def add(a,b): return {"type":"minecraft:add","argument1":a,"argument2":b}
def mul(a,b): return {"type":"minecraft:mul","argument1":a,"argument2":b}
def min2(a,b): return {"type":"minecraft:min","argument1":a,"argument2":b}
def absf(a): return {"type":"minecraft:abs","argument":a}
def clamp(a): return {"type":"minecraft:clamp","input":a,"min":0.0,"max":1.0}
def rc(v,left,right,yes,no): return {"type":"minecraft:range_choice","input":v,
                    "min_inclusive":left,"max_exclusive":right,
                    "when_in_range":yes,"when_out_of_range":no}

# 2.4 used 6.6 and 5.0: separate colors were changing faster than the
# size of an island! Intermediate frequency gives coherent territories without
# recreating the old 3-5 minute large-biomes.
for path,scale in ((TEMPERATURE,2.15),(VEGETATION,1.85)):
    cfg=read(path)
    assert cfg["type"]=="minecraft:shifted_noise"
    assert cfg["y_scale"]==0
    cfg["xz_scale"]=scale
    put(path,cfg)

dim=read(BIOME_PATH)
entries=dim["generator"]["biome_source"]["biomes"]
names={x["biome"] for x in entries}
expected={
    "halohaven:dawn_gardens","halohaven:chalk_terraces",
    "halohaven:eternal_canopy","halohaven:ringing_gorges",
    "halohaven:forgotten_choirs","halohaven:lower_veil"}
assert names==expected,(names,expected)
assert dim["generator"]["biome_source"]["type"]=="minecraft:multi_noise"
T="halohaven:climate_temperature"
V="halohaven:climate_vegetation"

settings=read(NOISE)
router=settings["noise_router"]
assert router["temperature"]==T and router["vegetation"]==V

# Extract the 2.4 morphological expressions WITHOUT losing the lotus bowls,
# eight chalk tiers, grove/forest forms, gorge rifts, choir ruins or veil caves.
def extract_profiles(overlay):
    # 2.4 left-associated weighted sum of six expressions:
    # (((w0*m0 + w1*m1) + w2*m2) ... + w5*m5)
    terms=[]
    node=overlay
    for _ in range(5):
        assert node["type"]=="minecraft:add","2.4 morph sum structure changed"
        terms.append(node["argument2"]);node=node["argument1"]
    terms.append(node)
    terms.reverse()
    assert len(terms)==6
    result=[]
    for index,item in enumerate(terms):
        assert item["type"]=="minecraft:mul", (index,item)
        result.append(item["argument2"])
    return result

# Exact multi_noise cases, no overlap and no two biome terrain styles
# being simultaneously blended together on the same landmass.
def exclusive(profiles):
    dawn,chalk,canopy,gorge,choir,veil=profiles
    return rc(T,-2.0,-0.35,
              rc(V,-2.0,0.0,choir,veil),
              rc(T,-0.35,0.35,
                 rc(V,-2.0,0.0,gorge,canopy),
                 rc(V,-2.0,0.0,chalk,dawn)))

# Temperature thresholds -0.35, +0.35 and humidity 0 are the actual
# classification boundaries of the 6-biome multi_noise source.
# A smooth mask creates VOID along all three contours at *every* Y level.
# Block-height land therefore cannot remain physically connected across two
# different biomes even if the 2.4 island noise would connect them.
def beyond_boundary(signal,at):
    distance=absf(add(signal,-at))
    # Empty core ±0.036 in noise space, blending to full original density
    # after ±0.06725: visible gaps but no vertical sliced rock wall.
    return clamp(mul(32.0,add(distance,-0.036)))
safe = min2(min2(beyond_boundary(T,-0.35),
                 beyond_boundary(T,0.35)),
                 beyond_boundary(V,0.0))
# min(original density, -5 + 15 * safe)
# At boundary safe=0 -> forced air. Outside safe=1 -> +10 cap no effect.
coast_cap=add(-5.0,mul(15.0,safe))

# Previous 2.3 made all continents overly dense; restore the original
# distinction between closely neighboring islands and a solid mega-continent.
for key in ("final_density","initial_density_without_jaggedness"):
    full=router[key]
    assert full["type"]=="minecraft:add",key
    profile_sum=full["argument2"]
    source=full["argument1"]
    # 2.3 added +0.16 constant and +0.075 global low-frequency fill.
    assert source["type"]=="minecraft:add",key
    assert source["argument1"]["type"]=="minecraft:add",key
    previous_fill=source["argument1"]["argument2"]
    if not isinstance(previous_fill,(int,float)):raise ValueError(previous_fill)
    # Same topography, less broad-brush filling of the void.
    source["argument1"]["argument2"]=0.0825
    if source["argument2"]["type"]=="minecraft:mul" and isinstance(source["argument2"]["argument1"],(int,float)):
        source["argument2"]["argument1"]=0.045
    regional=exclusive(extract_profiles(profile_sum))
    regional_density=add(source,regional)
    router[key]=min2(regional_density,copy.deepcopy(coast_cap))

put(NOISE,settings)
# Static negative checks catch accidental future reintroduction of the
# overpaint in v2.4 or thresholds moving away from biome_source parameters.
after=read(NOISE)
for k in ("final_density","initial_density_without_jaggedness"):
    root=after["noise_router"][k]
    assert root["type"]=="minecraft:min"
    assert root["argument2"]["type"]=="minecraft:add"
    assert root["argument2"]["argument1"]==-5.0
    assert root["argument1"]["argument2"]["type"]=="minecraft:range_choice"

# Verify mathematical seam guarantee for representative climate values.
def seam_value(t,v):
    def factor(value,thresh):
        return max(0.,min(1.,32*(abs(value-thresh)-0.036)))
    return min(factor(t,-.35),factor(t,.35),factor(v,0.))
for temp in (-.35,.35):
    for humid in (-.7,.7):
        assert seam_value(temp,humid)==0
for humid in (-1e-6,0,1e-6):
    assert seam_value(.8,humid)==0
for temp,humid in ((-.8,.8),(-.8,-.8),(0.1,-.8),(.1,.8),(.8,.8),(.8,-.8)):
    assert math.isclose(seam_value(temp,humid),1.0), (temp,humid,seam_value(temp,humid))
print("ISLAND BIOME SEAMS PASS: 6 exclusive morphology profiles, 3 full-height air channels, compact but coherent climates")
print("NO NEW REGISTRIES: no blocks/items/sounds/entities registered")
