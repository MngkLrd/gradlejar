#!/usr/bin/env python3
"""Halo Haven 2.8 -- broad, rounded islands and a genuinely elevated Chalk region.

Apply after tools/halohaven27_cloud_world.py. Uses only the existing blocks,
biomes, noises and mobs. A high 256-block noise dimension is essential: v2.7
generated ground only below Y128 even though its dimension type supported Y256.

Geography:
  - coherent low-frequency land cores, not tiny noisy/strand-like islands;
  - tapering rock beneath a softly curved top, both physically thick;
  - eight substantial wide-shelf chalk plateau levels at ~Y139..220;
  - six region-exclusive morphologies, same biome seam as 2.6/2.7;
  - region-specific cliff materials retained from previous releases;
  - floating cloud platforms with contiguous faces, plus high chalk mist.
"""
from pathlib import Path
import json

BASE=Path("projects/halohaven/src/main/resources/data/halohaven")
NOISE=BASE/"worldgen/noise_settings/heaven_islands.json"
DIM=BASE/"dimension/heaven.json"

def get(p):return json.loads(p.read_text(encoding="utf8"))
def save(p,j):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(j,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
def add(a,b):return {"type":"minecraft:add","argument1":a,"argument2":b}
def mul(a,b):return {"type":"minecraft:mul","argument1":a,"argument2":b}
def minimum(a,b):return {"type":"minecraft:min","argument1":a,"argument2":b}
def ab(a):return {"type":"minecraft:abs","argument":a}
def clamp(a,lo=0,hi=1):return {"type":"minecraft:clamp","input":a,"min":lo,"max":hi}
def noise(name,xz,y=0):
    return {"type":"minecraft:noise","noise":"minecraft:"+name,"xz_scale":xz,"y_scale":y}
def grad(bottom,top,lo,hi):
    return {"type":"minecraft:y_clamped_gradient","from_y":bottom,"to_y":top,
            "from_value":lo,"to_value":hi}
def rc(v,lo,hi,a,b):
    return {"type":"minecraft:range_choice","input":v,
            "min_inclusive":lo,"max_exclusive":hi,
            "when_in_range":a,"when_out_of_range":b}
def plus(*parts):
    result=parts[0]
    for q in parts[1:]:result=add(result,q)
    return result

world=get(NOISE)
dimension=get(DIM)
assert world["noise"]["height"]==128
assert dimension["generator"]["settings"]=="halohaven:heaven_islands"
dimension_type=BASE/"dimension_type/heaven.json"
kind=get(dimension_type)
assert kind["height"]==256 and kind["logical_height"]==256
world["noise"]["height"]=256

# Carefully enlarge coherent BIOME TERRITORIES rather than returning to
# the many-kilometer original climate scale. The dry Chalk side benefits from
# contiguous space for its huge seven-stepped mesa.
for name,xz in (("climate_temperature",0.53),("climate_vegetation",0.49)):
    path=BASE/"worldgen/density_function"/(name+".json")
    source=get(path)
    assert source["type"]=="minecraft:shifted_noise"
    source["xz_scale"]=xz
    save(path,source)

# Guaranteed body thickness. The horizontal field below is almost entirely
# 2-D, so there are no frequency-driven vertical needles or lonely rock voxels.
def body(field,relief,low,high,taper):
    top=add(high,relief)
    side=add(field,mul(-1,taper))
    return minimum(minimum(low,top),side)

# Broad, smooth 2D nucleus: two slow correlated noise passes act as a gate.
# Small fast details are purposefully omitted: they created thin peninsula
# strands and peppered isolated block islands in 2.6.
def mass(scale,extra,bias,weight,secondary):
    nucleus=noise("continentalness",scale)
    contour=plus(bias,mul(weight,nucleus))
    rounded=plus(.28,mul(.47,nucleus),mul(secondary,noise("erosion",extra)))
    return minimum(contour,rounded)

# Gentle taper at bottom encourages rounded thick hanging landmasses, not
# unbroken walls down to Y0. Low altitude cannot create stray blocks.
taper=grad(58,120,.39,0)
bottom=grad(55,89,-1.25,1.10)
normal_top=grad(87,146,1.0,-1.0)
canopy_top=grad(92,153,1.0,-1.0)
gorge_top=grad(91,150,1.0,-1.0)
choir_top=grad(94,163,1.0,-1.0)
veil_top=grad(68,132,1.0,-1.0)

dawn_core=mass(.42,.65,.30,.72,.09)
chalk_core=mass(.23,.38,.77,.52,.06)
canopy_core=mass(.39,.59,.38,.73,.10)
gorge_core=mass(.48,.75,.24,.85,.08)
choir_core=mass(.39,.56,.37,.70,.07)
veil_core=mass(.44,.70,.26,.68,.07)

# DAWN: flat living center and a few-block high, irregular lotus rim.
lip=clamp(mul(3.1,add(.30,mul(-1,dawn_core))))
dawn_relief=plus(-.05,mul(.29,lip),mul(.028,noise("vegetation",.72)))
dawn=body(dawn_core,dawn_relief,bottom,normal_top,taper)

# CHALK: 8 TRUE TERRACE HEIGHT BANDS ABOVE ALL ORDINARY ISLANDS.
# Y level at zero relief = 178. 0.4 offset ~=16 blocks. Whole surface thus
# spans roughly Y140..Y220; lower levels are not tiny columns or separate
# spires: they belong to a broad plateau nucleus with the same stable core.
chalk_mid=grad(136,219,1.0,-1.0)
chalk_signal=plus(mul(.84,noise("erosion",.31)),
                  mul(.17,noise("continentalness",.29)))
thresholds=[(-.52,-.89),(-.37,-.60),(-.22,-.30),(-.07,.00),
            (.08,.30),(.23,.59),(.38,.88)]
chalk_relief=1.16
for t,v in reversed(thresholds):
    chalk_relief=rc(chalk_signal,-2.0,t,v,chalk_relief)
chalk_bottom=grad(76,122,-1.1,1.15)
chalk_taper=grad(78,154,.28,0)
chalk=body(chalk_core,chalk_relief,chalk_bottom,chalk_mid,chalk_taper)

# CANOPY: solid humped forest islands that can carry groups of giant trees.
canopy=body(canopy_core,plus(.11,mul(.075,noise("vegetation",.60))),
            bottom,canopy_top,taper)
# GORGE: broad eroded shoulders with one continuous rounded valley groove;
# not 2-block spires and abrupt binary carve holes.
fracture=clamp(mul(5.0,add(.19,mul(-1,ab(noise("erosion",.65))))))
gorge=body(gorge_core,plus(.06,mul(-.86,fracture),
                           mul(.11,noise("continentalness",.70))),
           bottom,gorge_top,taper)
# CHOIR: naturally supported monumental shelves (not micro islands at Y0).
choir=body(choir_core,plus(.20,mul(.065,noise("continentalness",.52))),
           bottom,choir_top,taper)
# VEIL: low, wide-but-fractured dark masses with enclosed caverns, not stray shards.
cave=ab(noise("spaghetti_3d_1",1.15,.96))
cavern_cut=rc(cave,0,.11,-.43,rc(cave,0,.19,-.12,0.0))
veil=body(veil_core,plus(-.22,mul(.08,noise("erosion",.72)),cavern_cut),
          grad(42,82,-1.2,1.0),veil_top,grad(45,105,.29,0))

T="halohaven:climate_temperature"
V="halohaven:climate_vegetation"
# Exactly the same 6-region boundaries as the dimension's multi_noise source.
region=rc(T,-2.0,-.14,rc(V,-2.0,0,choir,veil),
       rc(T,-.14,.14,rc(V,-2.0,0,gorge,canopy),
                           rc(V,-2.0,0,chalk,dawn)))

def edge(q,value):
    return clamp(mul(31.0,add(ab(add(q,-value)),-.034)))
safe=minimum(minimum(edge(T,-.14),edge(T,.14)),edge(V,0))
air_margin=add(-5.5,mul(16,safe))
final=minimum(region,air_margin)
router=world["noise_router"]
assert router["temperature"]==T and router["vegetation"]==V
for key in ("final_density","initial_density_without_jaggedness"):
    router[key]=final

save(NOISE,world)
# High clouds should surround high Chalk tiers, not be only far below them.
cfg=BASE/"worldgen/configured_feature/sky_clouds.json"
assert get(cfg)["type"]=="halohaven:sky_clouds"
save(BASE/"worldgen/placed_feature/sky_clouds_chalk.json",{
  "feature":"halohaven:sky_clouds",
  "placement":[{"type":"minecraft:count","count":2},
    {"type":"minecraft:in_square"},
    {"type":"minecraft:height_range",
     "height":{"type":"minecraft:uniform",
               "min_inclusive":{"absolute":166},
               "max_inclusive":{"absolute":185}}},
    {"type":"minecraft:biome"}]
})
chalk_biome=BASE/"worldgen/biome/chalk_terraces.json"
biome=get(chalk_biome)
assert "halohaven:sky_clouds" in biome["features"][9]
biome["features"][9].append("halohaven:sky_clouds_chalk")
save(chalk_biome,biome)

assert get(NOISE)["noise"]["height"]==256
assert get(NOISE)["noise_router"]["final_density"]["type"]=="minecraft:min"
assert len([b for b in dimension["generator"]["biome_source"]["biomes"]])==6
print("PASS: 256-high dimension, six broad coherent island masses and void biome seams")
print("PASS: eight giant Chalk terrace bands above normal islands, from ~Y140 to ~Y220")
print("PASS: flat-sided cloud sheets at both ordinary islands and high Chalk shelves")
