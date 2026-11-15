#!/usr/bin/env python3
"""Halo Haven 2.3 - geography, ecology, architecture fixes. Run after 2.2 art bundler."""
from pathlib import Path
import json, copy, re, tarfile, tempfile, random
P=Path("projects/halohaven")
DATA=P/"src/main/resources/data/halohaven"
JAVA=P/"src/main/java/dev/minemods/halohaven"
AD=Path("projects/halohaven-alpha21/bundle.tar.xz")
assert AD.exists()

def patch_java(name, f):
    p=JAVA/name
    txt=p.read_text(encoding="utf8")
    out=f(txt)
    assert out!=txt, name
    p.write_text(out,encoding="utf8")
def save(p,data):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf8")
def load(p): return json.loads(p.read_text(encoding="utf8"))
def replace(s,a,b,n=1):
    assert a in s,(a,s[:200])
    return s.replace(a,b,n)

# The old 2.2 "0.9" scale is still close to vanilla-scale biomes (several km):
# this is the actual climate source used by the noise-router, not a cosmetic temperature map.
for name,xz in (("climate_temperature",6.6),("climate_vegetation",5.0)):
    file=DATA/"worldgen/density_function"/(name+".json")
    x=load(file)
    assert x["type"]=="minecraft:shifted_noise"
    x["xz_scale"]=xz
    save(file,x)
noise=DATA/"worldgen/noise_settings/heaven_islands.json"
settings=load(noise)
router=settings["noise_router"]
assert router["temperature"]=="halohaven:climate_temperature"
assert router["vegetation"]=="halohaven:climate_vegetation"
# Keep large continents & regional silhouette, add traversable saddle ridges and satellite-islands.
for key in ("final_density","initial_density_without_jaggedness"):
    parent=router[key]
    assert parent["type"]=="minecraft:add"
    orig=parent["argument1"]
    router[key]={
        "type":"minecraft:add",
        "argument1":{"type":"minecraft:add","argument1":orig,"argument2":0.16},
        "argument2":{"type":"minecraft:mul","argument1":0.075,
                     "argument2":{"type":"minecraft:noise","noise":"minecraft:continentalness",
                                  "xz_scale":2.4,"y_scale":0}}
    }
save(noise,settings)

# Remove the vanilla Cherry tree from worldgen, give each region a distinct crown geometry,
# bark material and leaf color rather than shared rotated sprites.
cf=DATA/"worldgen/configured_feature"
pf=DATA/"worldgen/placed_feature"
sky=load(cf/"sky_crown_tree.json")
assert sky["type"]=="minecraft:tree"
ancient=load(cf/"ancient_canopy_tree.json")
for cfg,wood in ((sky,"dawnwood"),(ancient,"heartwood")):
    cfg["config"]["trunk_provider"]["state"]={"Name":"halohavenalpha2:"+wood+"_log",
                                                 "Properties":{"axis":"y"}}
    cfg["config"]["foliage_provider"]["state"]={
        "Name":"halohavenalpha2:"+wood+"_leaves",
        "Properties":{"distance":"7","persistent":"true","waterlogged":"false"}}
    cfg["config"]["dirt_provider"]["state"]={"Name":"halohaven:skygrass"}
save(cf/"sky_crown_tree.json",sky)
save(cf/"ancient_canopy_tree.json",ancient)

# Explicit variants of tree silhouettes: a single base no longer serves all 6 biomes.
variants={
"dawn_gardens":("dawnwood",7,4,2,4,3,"halohaven:sunlit_loam"),
"chalk_terraces":("chalkwood",5,3,1,2,3,"halohaven:chalkstone"),
"eternal_canopy":("heartwood",17,8,4,5,6,"halohaven:canopy_loam"),
"ringing_gorges":("ringwood",9,5,2,2,4,"halohaven:resonant_shale"),
"forgotten_choirs":("choirwood",8,5,1,3,3,"halohaven:choirstone"),
"lower_veil":("veilwood",10,6,2,4,5,"halohaven:umbral_silt")}
for biome,(wood,base,randa,randb,radius,ht,soil) in variants.items():
    cfg=copy.deepcopy(sky)
    t=cfg["config"]["trunk_placer"]
    t["base_height"]=base;t["height_rand_a"]=randa;t["height_rand_b"]=randb
    fl=cfg["config"]["foliage_placer"]
    fl["radius"]={"type":"minecraft:constant","value":radius}
    fl["height"]=ht
    cfg["config"]["trunk_provider"]["state"]={"Name":"halohavenalpha2:"+wood+"_log","Properties":{"axis":"y"}}
    cfg["config"]["foliage_provider"]["state"]={
        "Name":"halohavenalpha2:"+wood+"_leaves",
        "Properties":{"distance":"7","persistent":"true","waterlogged":"false"}}
    # Dirt conversion occurs at the base of Minecraft trees; using the matching terrain block
    # avoids vanilla dirt blocks appearing in an otherwise alien ecosystem.
    cfg["config"]["dirt_provider"]["state"]={"Name":soil}
    save(cf/("regional_tree_"+biome+".json"),cfg)
    obj={"feature":"halohaven:regional_tree_"+biome,
         "placement":[{"type":"minecraft:count","count":2 if biome in ("eternal_canopy","lower_veil") else 1},
         {"type":"minecraft:in_square"},
         {"type":"minecraft:heightmap","heightmap":"OCEAN_FLOOR_WG"},
         {"type":"minecraft:biome"}]}
    save(pf/("regional_tree_"+biome+".json"),obj)
    bp=DATA/"worldgen/biome"/(biome+".json")
    data=load(bp)
    features=data["features"]
    for row in features:
        row[:]=[id for id in row if not any(p in id for p in (
            "sky_crown_tree","ancient_canopy_tree"))]
    features[9].append("halohaven:regional_tree_"+biome)
    save(bp,data)

# A real ceiling-side light. In darkness below islands it is placed from the void side,
# after scanning *up* to a cloudstone underside, never suspended at arbitrary coordinates.
glowcf={
 "type":"minecraft:simple_block",
 "config":{"to_place":{"type":"minecraft:simple_state_provider",
                       "state":{"Name":"halohavenalpha2:undersky_glow"}}}}
save(cf/"undersky_glow.json",glowcf)
glowpf={
 "feature":"halohaven:undersky_glow",
 "placement":[
   {"type":"minecraft:count","count":8},
   {"type":"minecraft:in_square"},
   {"type":"minecraft:height_range","height":{"type":"minecraft:uniform",
       "min_inclusive":{"absolute":30},"max_inclusive":{"absolute":105}}},
   {"type":"minecraft:environment_scan","direction":"up",
    "target_condition":{"type":"minecraft:matching_blocks",
                         "blocks":["halohaven:cloudstone","halohaven:veilstone","halohaven:moonstone",
                                   "halohaven:chalkstone","halohaven:choirstone"]},
    "allowed_search_condition":{"type":"minecraft:matching_blocks","blocks":"minecraft:air"},
    "max_steps":64},
   {"type":"minecraft:random_offset",
    "xz_spread":0,"y_spread":{"type":"minecraft:constant","value":-1}},
   {"type":"minecraft:block_predicate_filter",
    "predicate":{"type":"minecraft:matching_blocks","blocks":"minecraft:air"}},
   {"type":"minecraft:biome"}
 ]}
save(pf/"undersky_glow.json",glowpf)
for biome in variants:
    bp=DATA/"worldgen/biome"/(biome+".json")
    data=load(bp); data["features"][9].append("halohaven:undersky_glow")
    save(bp,data)

# Protect player-valuable block entities while making repeat generation more conservative.
def village(txt):
    txt=replace(txt,'            if(d>24 && random.nextInt(bloomFrequency)==0){',
                '            if(d>24 && random.nextInt(bloomFrequency)==0){')
    txt=txt.replace('district==2?Blocks.LILY_OF_THE_VALLEY:','district==2?HavenBiomeBlocks.VEIL_LICHEN.get():')
    txt=txt.replace('(random.nextBoolean()?Blocks.AZURE_BLUET:Blocks.PINK_TULIP)',
                    '(random.nextBoolean()?HaloHaven.CELESTIAL_BLOOM.get():HavenBiomeBlocks.SUNPETAL.get())')
    txt=txt.replace('random.nextBoolean()?Blocks.ALLIUM:Blocks.LILY_OF_THE_VALLEY',
                    'random.nextBoolean()?HaloHaven.CELESTIAL_BLOOM.get():HavenBiomeBlocks.SUNPETAL.get()')
    # Previous door clearance removed BOTH jambs, leaving a 3-wide breach in the façade.
    txt=replace(txt,
        'if(step<=0)for(int yy=1;yy<=3;yy++) place(l,cx+x,TOP_Y+yy,cz+entranceZ+step*(-sz),Blocks.AIR);',
        'if(step<0 || x==0)for(int yy=1;yy<=3;yy++) place(l,cx+x,TOP_Y+yy,cz+entranceZ+step*(-sz),Blocks.AIR);')
    txt=replace(txt,
        '        l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),3);\n    }',
        '''        BlockPos pos=new BlockPos(x,y,z);
        if(l.getBlockEntity(pos) instanceof ChestBlockEntity && b!=Blocks.CHEST)return;
        if(l.getBlockState(pos).getBlock() instanceof DoorBlock && b!=Blocks.AIR)return;
        l.setBlock(pos,b.defaultBlockState(),3);
    }''')
    # avoid vanilla cherry when this region's custom log exists
    txt=txt.replace('nativeWood("heartwood_log")',
      'nativeWood("dawnwood_log")')
    return txt
patch_java("HavenVillages.java",village)

def arch(txt):
    txt=replace(txt,
        '        l.setBlock(new BlockPos(x,y,z),block.defaultBlockState(),3);\n    }',
        '''        BlockPos pos=new BlockPos(x,y,z);
        if(l.getBlockEntity(pos) instanceof ChestBlockEntity && block!=Blocks.CHEST)return;
        l.setBlock(pos,block.defaultBlockState(),3);
    }''')
    # never suspend decorations: every hanging chandelier must be on a post or under an actual roof.
    txt=txt.replace('''            for(int dx : new int[]{-4,4})for(int dz:new int[]{-3,3})
                p(l,x+dx,GROUND+4,z+dz,HaloHaven.CLOUD_LANTERN.get());''',
        '''            for(int dx : new int[]{-4,4})for(int dz:new int[]{-3,3}){
                p(l,x+dx,GROUND+1,z+dz,HaloHaven.GILDED_PILLAR.get());
                p(l,x+dx,GROUND+2,z+dz,HaloHaven.CLOUD_LANTERN.get());
            }''')
    # continuous garden enclosure instead of four distant isolated "fence posts".
    txt=replace(txt,
        '''            for(int dx:new int[]{-7,7})for(int dz:new int[]{-5,5}){
                p(l,x+dx,GROUND+1,z+dz,HaloHaven.AZURE_FENCE.get());
                p(l,x+dx,GROUND+2,z+dz,HaloHaven.CLOUD_LANTERN.get());
            }''',
        '''            for(int dz:new int[]{-5,5})for(int dx=-7;dx<=7;dx++){
                if(dz==-5 && Math.abs(dx)<2)continue; // walkable entrance
                int atX=x+dx,atZ=z+dz;
                p(l,atX,GROUND,atZ,HaloHaven.PEARL_BRICKS.get());
                p(l,atX,GROUND+1,atZ,HaloHaven.AZURE_FENCE.get());
                if(dx==-7 || dx==7){
                    p(l,atX,GROUND+2,atZ,HaloHaven.CLOUD_LANTERN.get());
                }
            }''')
    return txt
patch_java("HeavenArchitecture.java",arch)

def landmark(txt):
    txt=txt.replace('Blocks.CRACKED_STONE_BRICKS','HavenBiomeBlocks.VEILSTONE.get()')
    txt=txt.replace('Blocks.CHERRY_LOG','HavenVillages.nativeWood("veilwood_log")')
    txt=txt.replace('Blocks.LANTERN','HaloHaven.CLOUD_LANTERN.get()')
    return txt if txt!=landmark_src else txt
landmark_src=(JAVA/"HavenLandmarks.java").read_text()
landmark_patched=landmark(landmark_src)
if landmark_patched!=landmark_src:
    (JAVA/"HavenLandmarks.java").write_text(landmark_patched,encoding="utf8")

# Disable arbitrary spawning of decoration blocks on top of chests / doors in re-used houses.
# Preserve config and all registered block IDs to keep preexisting saves valid.

with tempfile.TemporaryDirectory() as tmp:
    root=Path(tmp)
    with tarfile.open(AD,"r:xz") as src:
        for info in src:
            if not info.isfile():continue
            rel=Path(info.name)
            assert rel.parts[0]=="src" and ".." not in rel.parts
            dest=root/rel;dest.parent.mkdir(parents=True,exist_ok=True)
            dest.write_bytes(src.extractfile(info).read())
    source=root/"src/main/java/dev/halohaven/alpha2/HaloHavenAlpha2.java"
    code=source.read_text(encoding="utf8")
    assert "    public static final DeferredRegister<CreativeModeTab> TABS" in code
    extra=[]
    for wood in ("chalkwood","ringwood","choirwood"):
        field=wood.upper()
        extra.append('    public static final DeferredBlock<Block> '+field+
           '_LOG = BLOCKS.register("'+wood+'_log", () -> new RotatedPillarBlock('+
           'BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).strength(2.0F)));\n')
        extra.append('    public static final DeferredBlock<Block> '+field+
           '_LEAVES = BLOCKS.register("'+wood+'_leaves", () -> new LeavesBlock('+
           'BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).strength(0.2F).noOcclusion()));\n')
    extra.append('    public static final DeferredBlock<Block> UNDERSKY_GLOW = BLOCKS.register("undersky_glow",'
                 ' () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GLOWSTONE).lightLevel(s -> 14)));\n')
    code=code.replace('    public static final DeferredRegister<CreativeModeTab> TABS',"".join(extra)+"\n    public static final DeferredRegister<CreativeModeTab> TABS")
    source.write_text(code,encoding="utf8")
    # Replace any remaining stock cherry and grass in authored NBT palettes, but never loot or air.
    try:
        import nbtlib
        for file in (root/"src/main/resources/data/halohavenalpha2/structure").glob("*.nbt"):
            nbt=nbtlib.load(file,gzipped=True)
            changed=False
            for p in nbt.get("palette",[]):
                block=str(p.get("Name",""))
                replacement={
                    "minecraft:cherry_log":"halohavenalpha2:heartwood_log",
                    "minecraft:cherry_wood":"halohavenalpha2:heartwood_wood",
                    "minecraft:cracked_stone_bricks":"halohavenalpha2:fractured_veilstone",
                    "minecraft:short_grass":"halohaven:celestial_bloom",
                    "minecraft:lantern":"halohaven:cloud_lantern"
                }.get(block)
                if replacement:
                    p["Name"]=nbtlib.String(replacement)
                    # different property families need different state properties
                    if block in ("minecraft:short_grass","minecraft:lantern"):
                        p.pop("Properties",None)
                    changed=True
            if changed:nbt.save(file,gzipped=True)
    except ImportError as e:
        raise RuntimeError("nbtlib required for NBT material audit") from e
    # Regional structures: stagger different region-specific weights and salts rather than
    # six overlapping structure sets with the same random placement salt.
    world=root/"src/main/resources/data/halohavenalpha2/worldgen"
    for i,name in enumerate(variants):
        alias=("dawn","chalk","canopy","gorge","choir","veil")[i]
        for f in (world/"structure_set").glob("*.json"):
            if f.stem!="microstructures_"+alias:continue
            cfg=load(f)
            cfg["placement"]["salt"]=1900432+i*30911
            cfg["placement"]["spacing"]=13
            cfg["placement"]["separation"]=7
            cfg["placement"]["exclusion_zone"]["chunk_count"]=7
            save(f,cfg)
    with tarfile.open(AD,"w:xz",preset=6) as tar:
        for file in sorted((root/"src").rglob("*")):
            if file.is_file():tar.add(file,arcname=file.relative_to(root).as_posix())
print("Worldgen patched: 6 fast climates, 6 native tree families, ceiling glows, village materials & door passages")
