#!/usr/bin/env python3
"""MIDNIGHT Reborn 0.5: standalone resource and template compiler. Python stdlib only."""
from pathlib import Path
import json, gzip, struct, zlib, shutil, random, math
P=Path(__file__).resolve().parent
S=P/"src/main"
if S.exists(): shutil.rmtree(S)
R=S/"resources"; D=R/"data/midnighteve"; A=R/"assets/midnighteve"
def write(path, obj):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(obj,indent=2,ensure_ascii=False)+"\n",encoding="utf8")
def text(path,s):
    path.parent.mkdir(parents=True,exist_ok=True); path.write_text(s,encoding="utf8")
def mc(s): return s if ":" in s else "minecraft:"+s
def mod(s): return "midnighteve:"+s

# Rebuild the climate palette and all generation registrations from scratch.
names=["firlight_woods","peppermint_grove","starlit_steppe","crystal_crown","mirror_fjords","silent_snowfields"]
vanilla_ores=("ore_dirt ore_gravel ore_granite_upper ore_granite_lower ore_diorite_upper ore_diorite_lower "
"ore_andesite_upper ore_andesite_lower ore_tuff ore_coal_upper ore_coal_lower ore_iron_upper ore_iron_middle "
"ore_iron_small ore_gold ore_gold_lower ore_redstone ore_redstone_lower ore_diamond ore_diamond_medium "
"ore_diamond_large ore_diamond_buried ore_lapis ore_lapis_buried ore_copper underwater_magma disk_sand disk_clay disk_gravel").split()
def mob(n,w,a,b): return {"type":mc(n),"weight":w,"minCount":a,"maxCount":b}
regions=[
    ("Firlight Forest","Светохвойный лес",-.35,.92,0xc7d7e1,0x7eabbf,0x5a744c,"trees_taiga patch_large_fern patch_grass_taiga_2 patch_berry_common",["wolf","fox","rabbit","sheep"]),
    ("Hearthwood Valley","Долина тёплых очагов",-.10,.8,0xe2ded4,0x99b8d2,0x7e9069,"trees_snowy patch_grass_plain patch_pumpkin",["rabbit","sheep","chicken","cow"]),
    ("Starlit Meadow","Звёздные луга",-.20,.75,0xc9d8e7,0x88acd4,0x8f9f80,"patch_grass_plain patch_pumpkin",["rabbit","sheep","horse"]),
    ("Crystal Crown","Хрустальная корона",-.8,.55,0xb2c9e2,0x7794bb,0x879f9f,"ice_patch ice_spike",["goat","rabbit"]),
    ("Mirror Fjords","Зеркальные фьорды",-.6,.65,0xc0d8e2,0x91b3cb,0x7193a0,"",["polar_bear","rabbit"]),
    ("Silent Tundra","Безмолвная тундра",-.4,.5,0xd2d9e3,0x91add0,0x9aaba0,"trees_grove patch_grass_plain",["rabbit","fox"])
]
en={"generator.midnighteve.endless_eve":"MIDNIGHT: Reborn","item.midnighteve.wintermark":"Wintermark","item.midnighteve.wrapped_gift":"Wrapped Gift","block.midnighteve.gift_crate":"Gift Box","message.midnighteve.gift":"A little winter surprise!"}
ru={"generator.midnighteve.endless_eve":"MIDNIGHT: Reborn","item.midnighteve.wintermark":"Зимняя марка","item.midnighteve.wrapped_gift":"Праздничный подарок","block.midnighteve.gift_crate":"Подарочная коробка","message.midnighteve.gift":"Маленький зимний сюрприз!"}
for i,n in enumerate(names):
    eng,rus,temp,wet,fog,sky,green,flora,fauna=regions[i]
    en["biome.midnighteve."+n]=eng; ru["biome.midnighteve."+n]=rus
    feat=[[],["lake_lava_underground","lake_lava_surface"],["amethyst_geode"],["monster_room","monster_room_deep"],[],[],vanilla_ores,[],["spring_water","spring_lava"],["glow_lichen"]+flora.split(),["freeze_top_layer"]]
    if i==4: feat[1]+=["iceberg_packed","iceberg_blue"]
    spawners={k:[] for k in ["ambient","axolotls","creature","misc","monster","underground_water_creature","water_ambient","water_creature"]}
    spawners["ambient"]=[mob("bat",10,2,6)]
    spawners["creature"]=[mob(x,8,1,3) for x in fauna]
    spawners["monster"]=[mob("zombie",95,2,4),mob("skeleton",55,2,3),mob("stray",55,2,3),mob("spider",80,2,4),mob("creeper",75,1,3),mob("enderman",10,1,2),mob("witch",5,1,1),mob("slime",75,1,3)]
    spawners["underground_water_creature"]=[mob("glow_squid",10,2,4)]
    if i==4: spawners["water_ambient"]=[mob("cod",12,3,6),mob("salmon",10,2,4)]
    write(D/"worldgen/biome"/(n+".json"),{"has_precipitation":True,"temperature":temp,"downfall":wet,
        "effects":{"fog_color":fog,"sky_color":sky,"water_color":0x456e9b,"water_fog_color":0x233f61,"grass_color":green,"foliage_color":green,
          "mood_sound":{"sound":"minecraft:ambient.cave","tick_delay":6000,"block_search_extent":8,"offset":2.0}},
        "carvers":{"air":[mc(v) for v in ["cave","cave_extra_underground","canyon"]]},
        "features":[[mc(v) for v in a] for a in feat],"spawn_costs":{},"spawners":spawners})
# Native trees give each land region an identifiable silhouette.
def uniform(a,b):return {"type":"minecraft:uniform","min_inclusive":a,"max_inclusive":b}
def state(n,**props):
    d={"Name":mc(n)}
    if props:d["Properties"]=props
    return {"type":"minecraft:simple_state_provider","state":d}
for name,base,extra,radius,crown,count,giant in [
 ("old_fir",15,9,4,7,2,True),("hearth_fir",8,3,3,3,2,False),
 ("tundra_pine",3,2,2,2,1,False)]:
    foliage={"type":"minecraft:mega_pine_foliage_placer","radius":0,"offset":0,"crown_height":uniform(5,9)} if giant else {"type":"minecraft:spruce_foliage_placer","radius":uniform(max(1,radius-1),radius),"offset":uniform(0,1),"trunk_height":uniform(1,crown)}
    config={"decorators":[],"dirt_provider":state("dirt"),"foliage_placer":foliage,
       "foliage_provider":state("spruce_leaves",distance="7",persistent="false",waterlogged="false"),
       "force_dirt":False,"ignore_vines":True,"minimum_size":{"type":"minecraft:two_layers_feature_size","limit":2,"lower_size":1 if giant else 0,"upper_size":2},
       "trunk_placer":{"type":"minecraft:giant_trunk_placer" if giant else "minecraft:straight_trunk_placer","base_height":base,"height_rand_a":extra,"height_rand_b":2},
       "trunk_provider":state("spruce_log",axis="y")}
    if giant:config["decorators"]=[{"type":"minecraft:alter_ground","provider":state("podzol",snowy="false")}]
    write(D/"worldgen/configured_feature"/(name+".json"),{"type":"minecraft:tree","config":config})
    write(D/"worldgen/placed_feature"/(name+".json"),{"feature":mod(name),"placement":[
        {"type":"minecraft:count","count":count},{"type":"minecraft:in_square"},
        {"type":"minecraft:surface_water_depth_filter","max_water_depth":0},
        {"type":"minecraft:heightmap","heightmap":"OCEAN_FLOOR"},
        {"type":"minecraft:block_predicate_filter","predicate":{"type":"minecraft:would_survive","state":{"Name":"minecraft:spruce_sapling","Properties":{"stage":"0"}}}},
        {"type":"minecraft:biome"}]})
for region,feature in [("firlight_woods","old_fir"),("peppermint_grove","hearth_fir"),("silent_snowfields","tundra_pine")]:
    p=D/"worldgen/biome"/(region+".json")
    b=json.loads(p.read_text());b["features"][9].append(mod(feature));write(p,b)
# Smooth vanilla climate axes, with peaks and coast selected separately.
biomes=[]
for c in [(-1.2,-.25),(-.25,.06),(.06,.38),(.38,1.2)]:
 for h in [(-1,-.25),(-.25,.3),(.3,1)]:
  for e in [(-1,-.35),(-.35,.25),(.25,1)]:
   for t in [(-1,-.3),(-.3,.35),(.35,1)]:
    for w in [(-1,-.55),(-.55,.55),(.55,1)]:
     if c[1]<=-.25: n=names[4]
     elif c[0]>=.06 and e[1]<=-.35 and (w[1]<=-.55 or w[0]>=.55): n=names[3]
     elif h[0]>=.3: n=names[0]
     elif h[1]<=-.25: n=names[5]
     elif t[0]>=.35 or e[0]>=.25: n=names[2]
     else: n=names[1]
     biomes.append({"biome":mod(n),"parameters":{"temperature":list(t),"humidity":list(h),"continentalness":list(c),"erosion":list(e),"weirdness":list(w),"depth":[-1,1],"offset":0}})
write(D/"worldgen/world_preset/endless_eve.json",{"dimensions":{
 "minecraft:overworld":{"type":"minecraft:overworld","generator":{"type":"minecraft:noise","settings":"minecraft:overworld","biome_source":{"type":"minecraft:multi_noise","biomes":biomes}}},
 "minecraft:the_nether":{"type":"minecraft:the_nether","generator":{"type":"minecraft:noise","settings":"minecraft:nether","biome_source":{"type":"minecraft:multi_noise","preset":"minecraft:nether"}}},
 "minecraft:the_end":{"type":"minecraft:the_end","generator":{"type":"minecraft:noise","settings":"minecraft:end","biome_source":{"type":"minecraft:the_end"}}}}})
write(R/"data/minecraft/tags/worldgen/world_preset/normal.json",{"replace":False,"values":[mod("endless_eve")]})
for key in ["mineshaft","ruined_portal_standard","stronghold"]:
    write(R/("data/minecraft/tags/worldgen/biome/has_structure/"+key+".json"),{"replace":False,"values":[mod(n) for n in names]})
# Vanilla NBT writer. Explicit types prevent corrupted template data.
def T(t,v): return (t,v)
def I(v): return T(3,v)
def B(v): return T(1,v)
def F(v): return T(5,v)
def Str(v): return T(8,v)
def C(v): return T(10,v)
def L(t,v): return T(9,(t,v))
def utf(v):
    b=v.encode("utf8"); return struct.pack(">H",len(b))+b
def payload(tag):
    t,v=tag
    if t==1:return struct.pack(">b",v)
    if t==3:return struct.pack(">i",v)
    if t==5:return struct.pack(">f",v)
    if t==6:return struct.pack(">d",v)
    if t==8:return utf(v)
    if t==9:
        typ,ls=v; return struct.pack(">bi",typ,len(ls))+b"".join(payload(T(typ,x)) for x in ls)
    if t==10:return b"".join(bytes([x[0]])+utf(k)+payload(x) for k,x in v.items())+b"\0"
    raise ValueError(t)
class Template:
    def __init__(self,w,h,d):self.size=(w,h,d);self.blocks={};self.entities=[];self.doors=[];self.rooms=[];self.block_nbt={}
    def put(self,x,y,z,name,**props):
        assert all(0<=a<b for a,b in zip((x,y,z),self.size)), (x,y,z,self.size)
        self.blocks[x,y,z]=(mc(name),tuple(sorted((k,str(v).lower()) for k,v in props.items())))
    def fill(self,x1,y1,z1,x2,y2,z2,name,**props):
        for x in range(x1,x2+1):
         for y in range(y1,y2+1):
          for z in range(z1,z2+1):self.put(x,y,z,name,**props)
    def resident(self,x,y,z,role,profession):
        def stack(item,count):return C({"id":Str(mc(item)),"count":I(count)})
        trade={
          "Baker":[("wheat",12,"midnighteve:wintermark",1),("midnighteve:wintermark",1,"cookie",8)],
          "Giftkeeper":[("emerald",3,"midnighteve:wintermark",1),("midnighteve:wintermark",3,"midnighteve:wrapped_gift",1)],
          "Woodworker":[("spruce_log",12,"midnighteve:wintermark",1),("midnighteve:wintermark",2,"lantern",3)],
          "Ranger":[("coal",10,"midnighteve:wintermark",1),("midnighteve:wintermark",2,"bread",6)],
          "Postmaster":[("paper",12,"midnighteve:wintermark",1),("midnighteve:wintermark",3,"midnighteve:wrapped_gift",1)],
          "Chandler":[("honeycomb",4,"midnighteve:wintermark",1),("midnighteve:wintermark",2,"lantern",3)],
          "Fisher":[("cod",10,"midnighteve:wintermark",1),("midnighteve:wintermark",1,"cooked_cod",6)]
        }[role]
        offers=[{"buy":stack(a,b),"sell":stack(c,d),"uses":I(0),"maxUses":I(12),"xp":I(2),"rewardExp":B(1),"priceMultiplier":F(0.0)} for a,b,c,d in trade]
        nbt={"id":Str("minecraft:villager"),"CustomName":Str(json.dumps({"text":role})),"PersistenceRequired":B(1),
            "VillagerData":C({"type":Str("minecraft:snow"),"profession":Str(mc(profession)),"level":I(2)}),
            "Offers":C({"Recipes":L(10,offers)}),"Tags":L(8,["midnighteve_resident","midnighteve_role_"+role.lower()])}
        self.entities.append({"pos":L(6,[x+.5,float(y),z+.5]),"blockPos":L(3,[x,y,z]),"nbt":C(nbt)})
    def save(self,n):
        palette=[]; index={}; blocks=[]
        # full blocks before stateful components; explicit air only within constructed rooms
        for pos,state in sorted(self.blocks.items(),key=lambda e:(e[0][1],e[0][0],e[0][2])):
            if state not in index:
                index[state]=len(palette); name,props=state
                v={"Name":Str(name)}
                if props:v["Properties"]=C({k:Str(v) for k,v in props})
                palette.append(v)
            entry={"pos":L(3,list(pos)),"state":I(index[state])}
            if pos in self.block_nbt:entry["nbt"]=C(self.block_nbt[pos])
            blocks.append(entry)
        for x,y,z in self.doors:
            assert self.blocks[x,y,z][0].endswith("_door") and self.blocks[x,y+1,z][0].endswith("_door")
            assert self.blocks[x,y,z+1][0]=="minecraft:air"
        for x,y,z in self.rooms:
            assert self.blocks[x,y,z][0]=="minecraft:air"
            assert self.blocks[x,y+1,z][0]=="minecraft:air"
        nbt=C({"DataVersion":I(3955),"size":L(3,list(self.size)),"palette":L(10,palette),"blocks":L(10,blocks),"entities":L(10,self.entities)})
        path=D/"structure"/(n+".nbt");path.parent.mkdir(parents=True,exist_ok=True)
        path.write_bytes(gzip.compress(b"\x0a\x00\x00"+payload(nbt),mtime=0))
        print(n, self.size, len(blocks),"blocks,",len(self.entities),"residents,",len(self.doors),"doors")
def path(t,x1,z1,x2,z2):
    t.fill(x1,0,z1,x2,2,z2,"stone")
    for x in range(x1,x2+1):
      for z in range(z1,z2+1):
       t.put(x,3,z,"cobblestone" if (x*17+z*13)%5==0 else "stone_bricks")
       t.fill(x,4,z,x,6,z,"air")
def cabin(t,x,z,w,d,role=None,wall="spruce_planks",roof="dark_oak",profession="farmer"):
    # floor y3; foundation, complete timber walls, windows and gable roof
    X=x+w-1; Z=z+d-1; cx=x+w//2
    t.fill(x,0,z,X,2,Z,"cobblestone")
    t.fill(x,3,z,X,3,Z,"spruce_planks")
    t.fill(x,4,z,X,8,Z,"air")
    for xx in range(x,X+1):
     for zz in range(z,Z+1):
      if xx in (x,X) or zz in (z,Z):
       for yy in range(4,9):
        beam=(xx in (x,X) and zz in (z,Z)) or yy==8 or (zz in (z,Z) and (xx-x)%4==0)
        t.put(xx,yy,zz,"stripped_spruce_log" if beam else ("stone_bricks" if yy==4 else wall),**({"axis":"y"} if beam else {}))
    for xx in [x+2,X-2]:
     for yy in (5,6):
      for zz in (z,Z): t.put(xx,yy,zz,"glass")
    for zz in [z+2,Z-2]:
     for yy in (5,6):
      for xx in (x,X):t.put(xx,yy,zz,"glass")
    for yy,half in [(4,"lower"),(5,"upper")]:
        t.put(cx,yy,z,"spruce_door",facing="north",half=half,hinge="left",open=False,powered=False)
    t.doors.append((cx,4,z)); t.rooms.append((cx,4,z+1))
    # thick gable ends, stairs across both roof slopes, eaves and snow ridge
    for xx in range(x-1,X+2):
        delta=min(xx-(x-1),X+1-xx); yy=9+delta
        for zz in range(z-1,Z+2):
            if xx==cx:
                t.put(xx,yy,zz,roof+"_slab",type="bottom",waterlogged=False)
            else:t.put(xx,yy,zz,roof+"_stairs",facing="east" if xx<cx else "west",half="bottom",shape="straight",waterlogged=False)
        if x<=xx<=X:
            for zz in (z,Z):
                for gy in range(9,yy):t.put(xx,gy,zz,wall)
    # ceiling closes all rooms; loft window at each gable, chimney capped by slab
    t.fill(x+1,9,z+1,X-1,9,Z-1,"spruce_planks")
    t.put(cx,10,z,"glass");t.put(cx,10,Z,"glass")
    for yy in range(4,15): t.put(X-1,yy,Z-2,"bricks")
    t.put(X-1,15,Z-2,"brick_slab",type="bottom",waterlogged=False)
    t.put(X-2,4,Z-2,"furnace",facing="west",lit=False)
    t.put(x+1,4,Z-1,"barrel",facing="up",open=False)
    t.put(x+2,4,Z-1,"crafting_table")
    t.put(x+1,4,z+2,"red_bed",facing="south",part="foot",occupied=False)
    t.put(x+1,4,z+3,"red_bed",facing="south",part="head",occupied=False)
    t.put(cx,8,z+d//2,"lantern",hanging=True,waterlogged=False)
    t.put(X-1,4,z+2,"spruce_stairs",facing="west",half="bottom",shape="straight",waterlogged=False)
    t.put(X-2,4,z+2,"spruce_fence",north=False,south=False,east=False,west=False,waterlogged=False)
    t.put(X-2,5,z+2,"spruce_pressure_plate",powered=False)
    t.put(cx-2,4,Z-1,"bookshelf")
    t.put(cx+1,4,Z-1,"midnighteve:gift_crate")
    # entrance steps and lamps outside; explicitly leave accessible path
    t.fill(cx-1,3,z-2,cx+1,3,z-1,"stone_bricks")
    t.fill(cx-1,4,z-2,cx+1,5,z-1,"air")
    for xx in (cx-2,cx+2):
        t.put(xx,4,z-1,"cobblestone_wall",up=True,north="none",south="none",east="none",west="none",waterlogged=False)
        t.put(xx,5,z-1,"lantern",hanging=False,waterlogged=False)
    if role:
        # Role-specific furnishing; a striped awning protects the entry.
        t.fill(cx-2,7,z-1,cx+2,7,z-1,"red_wool")
        for xx in (cx-1,cx+1):
            t.put(xx,7,z-1,"white_wool")
        if role=="Baker":
            t.put(x+2,4,Z-2,"smoker",facing="north",lit=False)
            t.put(x+3,4,Z-1,"spruce_planks");t.put(x+3,5,Z-1,"cake",bites=0)
            t.put(x+1,4,Z-3,"hay_block",axis="y")
        elif role=="Giftkeeper":
            t.put(x+2,4,Z-2,"chest",facing="north",type="single",waterlogged=False)
            t.put(cx+2,4,Z-1,"midnighteve:gift_crate")
        elif role=="Woodworker":
            t.put(x+2,4,Z-2,"smithing_table")
            t.put(x+3,4,Z-1,"stripped_spruce_log",axis="x")
        else:
            t.put(x+2,4,Z-2,"cartography_table")
        t.resident(cx,4,z+3,role,profession)
def tree(t,x,z,y=4):
    for yy in range(y,y+12):t.put(x,yy,z,"spruce_log",axis="y")
    # tapered clustered fir silhouette, staggered tiers, no solid cone/cube
    for dy,r in [(3,3),(4,4),(5,3),(6,2),(7,3),(8,2),(9,2),(10,1),(11,1)]:
        for dx in range(-r,r+1):
         for dz in range(-r,r+1):
          if (dx or dz) and abs(dx)+abs(dz)<=r+1 and (abs(dx)!=r or abs(dz)<=1):
           t.put(x+dx,y+dy,z+dz,"spruce_leaves",persistent=True,distance=1,waterlogged=False)
        for dx,dz in [(r,0),(-r,0),(0,r),(0,-r)]:
            if dy in (4,7,10):
                t.put(x+dx,y+dy,z+dz,["red_wool","yellow_wool","white_wool"][(dy+dx+dz)%3])
                if dy!=10:t.put(x+dx,y+dy-1,z+dz,"lantern",hanging=True,waterlogged=False)
    t.put(x,y+12,z,"glowstone")
    for dx,dz in [(-2,-2),(2,-2),(-2,2),(2,2),(1,3),(-3,1)]:
        t.put(x+dx,y,z+dz,"midnighteve:gift_crate")
    for dx,dz in [(5,0),(-5,0),(0,5),(0,-5)]:
        t.put(x+dx,y,z+dz,"stone_brick_wall",up=True,north="none",south="none",east="none",west="none",waterlogged=False)
        t.put(x+dx,y+1,z+dz,"lantern",hanging=False,waterlogged=False)
# A compact square, asymmetrical frontage, actual furnished interiors.
town=Template(43,19,39)
path(town,4,16,38,24);path(town,18,4,24,34);path(town,15,14,27,26)
cabin(town,3,4,11,11,"Baker","stripped_birch_wood","dark_oak","farmer")
cabin(town,28,5,11,11,"Woodworker","spruce_planks","dark_oak","toolsmith")
cabin(town,3,27,11,10,"Giftkeeper","white_terracotta","dark_oak","librarian")
cabin(town,28,27,11,10,"Ranger","spruce_planks","spruce","butcher")
# connect all entrances, including northern houses, to square around the buildings
path(town,7,1,17,2);path(town,14,1,17,18);path(town,25,2,33,3);path(town,25,2,27,16)
path(town,7,24,9,25);path(town,32,24,34,25)
tree(town,21,20)
# One persistent celebration controller; lamps are ordinary world blocks, not particles.
def festival(t,x,z):
    t.put(x,4,z,"midnighteve:festival_hearth",stage=0)
    t.put(x,5,z,"air")
    for dx,dz in [(-3,-2),(3,-2),(-3,2),(3,2)]:
        t.put(x+dx,3,z+dz,"stone_bricks")
        t.put(x+dx,4,z+dz,"stone_brick_wall",up=True,north="none",south="none",east="none",west="none",waterlogged=False)
        t.put(x+dx,5,z+dz,"midnighteve:festival_lamp",lit=False)
festival(town,21,25)
town.save("frostmarket")
lodge=Template(32,19,25)
path(lodge,4,3,28,7);path(lodge,3,8,28,21)
cabin(lodge,4,9,13,12,"Ranger","spruce_planks","dark_oak","butcher")
tree(lodge,24,13)
lodge.save("evergreen_lodge")
# Smaller, real buildings with a separate footprint appropriate for uplands.
watch=Template(21,19,21)
path(watch,4,3,16,7)
cabin(watch,5,8,11,11,"Giftkeeper","stone_bricks","spruce","librarian")
for y in range(4,14):watch.put(17,y,14,"stripped_spruce_log",axis="y")
watch.fill(16,14,13,18,14,15,"spruce_planks")
watch.put(17,15,14,"lantern",hanging=False,waterlogged=False)
watch.save("northwatch")

# Postal station: enclosed timber office plus a real covered sorting/loading yard.
post=Template(32,19,26)
path(post,6,3,25,5);path(post,19,6,26,7)
post.fill(17,0,7,29,2,20,"cobblestone")
post.fill(17,3,7,29,3,20,"spruce_planks")
cabin(post,4,8,11,12,"Postmaster","spruce_planks","dark_oak","librarian")
for x in (18,28):
    for z in (8,19):
        post.fill(x,4,z,x,9,z,"stripped_spruce_log",axis="y")
for x in range(17,30):
    for z in range(7,21):
        h=10+min(x-17,29-x)//2
        post.put(x,h,z,"spruce_planks")
        if x in (17,29):post.put(x,h,z,"dark_oak_stairs",facing="east" if x==17 else "west",half="bottom",shape="straight",waterlogged=False)
for x in range(19,28,2):
    post.put(x,4,18,"barrel",facing="up",open=False)
    post.put(x,4,10,"midnighteve:gift_crate")
for x in (20,26):
    post.put(x,10,13,"stripped_spruce_log",axis="z")
    post.put(x,9,13,"lantern",hanging=True,waterlogged=False)
post.save("winter_post")

# Candle hall: taller stone nave, buttresses, coloured glass, pews and tower.
hall=Template(31,25,31)
path(hall,12,2,18,7);path(hall,9,3,21,6);path(hall,3,5,11,6)
x0,x1,z0,z1=8,22,8,24
hall.fill(x0,0,z0,x1,3,z1,"stone_bricks")
hall.fill(x0,4,z0,x1,12,z1,"air")
for x in range(x0,x1+1):
    for z in range(z0,z1+1):
        if x in (x0,x1) or z in (z0,z1):
            hall.fill(x,4,z,x,12,z,"stone_bricks")
for x in (x0,x1):
    for z in (11,16,21):
        hall.fill(x,7,z,x,10,z,"yellow_stained_glass")
        q=x-1 if x==x0 else x+1
        hall.fill(q,4,z-2,q,9,z-2,"stone_bricks")
        hall.put(q,10,z-2,"stone_brick_stairs",facing="east" if x==x0 else "west",half="bottom",shape="straight",waterlogged=False)
for x in range(x0-1,x1+2):
    y=13+min(x-x0+1,x1+1-x)
    for z in range(z0-1,z1+2):
        if x==15:hall.put(x,y,z,"dark_oak_slab",type="bottom",waterlogged=False)
        else:hall.put(x,y,z,"dark_oak_stairs",facing="east" if x<15 else "west",half="bottom",shape="straight",waterlogged=False)
    if x0<=x<=x1:
        for z in (z0,z1):
            if y>13:hall.fill(x,13,z,x,y-1,z,"stone_bricks")
hall.fill(14,14,z1,16,17,z1,"red_stained_glass")
for y,half in ((4,"lower"),(5,"upper")):
    hall.put(15,y,z0,"spruce_door",facing="north",half=half,hinge="left",open=False,powered=False)
hall.doors.append((15,4,z0));hall.rooms.append((15,4,z0+1))
for z in (12,15,18):
    for x in (10,11,12,18,19,20):
        hall.put(x,4,z,"spruce_stairs",facing="north",half="bottom",shape="straight",waterlogged=False)
for x in (10,20):
    for z in (10,16,22):
        hall.put(x,4,z,"stone_brick_wall",up=True,north="none",south="none",east="none",west="none",waterlogged=False)
        hall.put(x,5,z,"lantern",hanging=False,waterlogged=False)
hall.put(15,4,22,"lectern",facing="north",has_book=False,powered=False)
# Freestanding belfry: stone plinth, open bell chamber supported by four columns.
hall.fill(2,0,8,6,3,12,"stone_bricks")
for x in (2,6):
    for z in (8,12): hall.fill(x,4,z,x,13,z,"stone_bricks")
hall.fill(2,8,8,6,8,12,"stone_bricks")
hall.fill(2,13,8,6,13,12,"spruce_planks")
hall.put(4,12,10,"bell",attachment="ceiling",facing="north",powered=False)
for y,r in ((14,3),(15,2),(16,1),(17,0)):
    for x in range(4-r,5+r):
        for z in range(10-r,11+r):
            hall.put(x,y,z,"dark_oak_planks")
hall.put(4,18,10,"lantern",hanging=False,waterlogged=False)
hall.resident(17,4,22,"Chandler","cleric")
festival(hall,15,5)
hall.save("candle_hall")

# Fjord trading post: two separate roofs, storage racks, raised timber landing.
fjord=Template(36,19,30)
path(fjord,7,3,25,5);path(fjord,23,6,28,10)
fjord.fill(16,0,5,20,2,25,"cobblestone")
cabin(fjord,4,8,11,10,"Fisher","spruce_planks","spruce","fisherman")
cabin(fjord,22,12,9,10,None,"stripped_spruce_wood","dark_oak")
for x in range(16,21):
    for z in range(5,26):
        fjord.put(x,3,z,"spruce_planks")
for z in (6,12,18,24):
    for x in (16,20):
        fjord.fill(x,0,z,x,4,z,"spruce_log",axis="y")
        fjord.put(x,5,z,"lantern",hanging=False,waterlogged=False)
for x in (23,25,27,29):
    fjord.put(x,4,23,"barrel",facing="up",open=False)
fjord.save("fjord_trading_post")
for i,(n,allowed,spacing,sep) in enumerate([
 ("frostmarket",["peppermint_grove","starlit_steppe"],38,12),
 ("evergreen_lodge",["firlight_woods","silent_snowfields"],29,9),
 ("northwatch",["crystal_crown","silent_snowfields"],34,11),
 ("winter_post",["peppermint_grove","firlight_woods"],36,12),
 ("candle_hall",["starlit_steppe","peppermint_grove"],42,14),
 ("fjord_trading_post",["mirror_fjords"],32,10)
]):
    write(D/"tags/worldgen/biome"/("has_structure/"+n+".json"),{"replace":False,"values":[mod(b) for b in allowed]})
    write(D/"worldgen/template_pool"/(n+".json"),{"fallback":"minecraft:empty","elements":[{"weight":1,"element":{"element_type":"minecraft:single_pool_element","location":mod(n),"processors":"minecraft:empty","projection":"rigid"}}]})
    write(D/"worldgen/structure"/(n+".json"),{"type":"minecraft:jigsaw","biomes":"#"+mod("has_structure/"+n),"step":"surface_structures","spawn_overrides":{},"terrain_adaptation":"beard_box","start_pool":mod(n),"size":0,"start_height":{"absolute":-3},"project_start_to_heightmap":"WORLD_SURFACE_WG","max_distance_from_center":80,"use_expansion_hack":False})
    write(D/"worldgen/structure_set"/(n+".json"),{"structures":[{"structure":mod(n),"weight":1}],"placement":{"type":"minecraft:random_spread","spacing":spacing,"separation":sep,"salt":271811+i*7817}})
    en["structure.midnighteve."+n]=n.replace("_"," ").title()
    ru["structure.midnighteve."+n]={"frostmarket":"Морозная ярмарка","evergreen_lodge":"Хвойный приют","northwatch":"Северная сторожка","winter_post":"Зимняя почта","candle_hall":"Зал свечей","fjord_trading_post":"Торговый двор фьордов"}[n]
# Art: hand-authored clusters with limited ramps, transparent item silhouettes.
def png(path,rows,pal):
    assert len(rows)==16 and all(len(r)==16 for r in rows)
    def chunk(k,v):return struct.pack(">I",len(v))+k+v+struct.pack(">I",zlib.crc32(k+v)&0xffffffff)
    data=b"".join(b"\0"+b"".join(bytes(pal[ch]) for ch in row) for row in rows)
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",16,16,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(data))+chunk(b"IEND",b""))
def rgba(s):return tuple(bytes.fromhex(s))+ (255,)
coin=["................",".....oooooo.....","...oohhhhhhoo...","..ohhmmmhhhho...","..ohmmlmmmhho...",".ohmmlLlmmdhho..",".ohmlLLLlmddho..",".ohmlllllmddho..",".ohmmlLlmdddho..",".ohmmmlmdddsho..","..ohmmdmddsho...","..ohhhddsssho...","...oohhsssoo....",".....ooooo......","................","................"]
png(A/"textures/item/wintermark.png",coin, {".":(0,0,0,0),**{k:rgba(v) for k,v in {"o":"775331","h":"e4bb65","m":"bd914c","l":"edce83","L":"fae6aa","d":"9f713e","s":"8c633a"}.items()}})
# Flat, cloth-wrapped gift; readable folded paper and gold ribbon.
gift=["................",".....gg.gg......","....ghg.ghg.....",".....ggggg......","..ooooGooooooo..","..oRRRGGRRRRRo..","..oRrrGGrrrRRo..","..oRrrGGrrrrRo..","..ogggGGgggggo..","..oGGGGGGGGGGo..","..oRrrGGrrrrRo..","..oRrrGGrrrdRo..","..oRrrGGrrddRo..","..oRRRGGRRRRRo..","..oooooooooooo..","................"]
giftpal={".":(0,0,0,0),**{k:rgba(v) for k,v in {"o":"6c302c","R":"b44c3f","r":"a84237","d":"923a32","g":"bc8d46","G":"e4bf71","h":"f0d38c"}.items()}}
png(A/"textures/item/wrapped_gift.png",gift,giftpal)
# Block wrapping paper: manually varied connected folds; ribbon stays intentional.
side=["RRRRRRGGrrrrrrrr","RRRRRRGGrrrrrrrr","RRrrrrGGrrrrrrrr","RRrrrrGGrrrddrrr","rrrrrrGGrrrddrrr","rrrrrrGGrrrrrrrr","ggggggGGgggggggg","GGGGGGGGGGGGGGGG","rrrrrrGGrrrrrrrr","rrrrrrGGrrrrrrrr","rrrrrrGGrrrrrrrr","rrrrrrGGrrrrdddd","rrrddrGGrrrrdddd","rrrddrGGrrrrrrrr","rrrrrrGGrrrrrrrr","ddddddGGdddddddd"]
top=["rrrrrrGGrrrrrrrr","rrrrrrGGrrrrrrrr","rrRRrrGGrrrrrrrr","rrRRrrGGrrrrrrrr","rrrrrgGGgrrrrrrr","rrrrrgGGgrrrrrrr","GGGGGGGGGGGGGGGG","GGGGGGGGGGGGGGGG","rrrrrgGGgrrrrrrr","rrrrrgGGgrrrrrrr","rrrrrrGGrrrddrrr","rrrrrrGGrrrddrrr","rrrrrrGGrrrrrrrr","rrrrrrGGrrrrrrrr","rrrrrrGGrrrrrrrr","rrrrrrGGrrrrrrrr"]
png(A/"textures/block/gift_crate_side.png",side,giftpal)
png(A/"textures/block/gift_crate_top.png",top,giftpal)
write(A/"blockstates/gift_crate.json",{"variants":{"":{"model":mod("block/gift_crate")}}})
write(A/"models/block/gift_crate.json",{"parent":"minecraft:block/cube_column","textures":{"side":mod("block/gift_crate_side"),"end":mod("block/gift_crate_top")}})
write(A/"models/item/gift_crate.json",{"parent":mod("block/gift_crate")})
for n in ["wintermark","wrapped_gift"]:write(A/"models/item"/(n+".json"),{"parent":"minecraft:item/generated","textures":{"layer0":mod("item/"+n)}})
write(D/"loot_table/blocks/gift_crate.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":mod("gift_crate")}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
write(D/"recipe/gift_crate.json",{"type":"minecraft:crafting_shaped","pattern":["PPP","PWP","PPP"],"key":{"P":{"item":"minecraft:paper"},"W":{"item":"minecraft:red_wool"}},"result":{"id":mod("gift_crate"),"count":2}})
# The stone hearth uses the original vanilla masonry palette. Top progress pixels
# are hand-defined, not procedural noise; one glowing inset per completed stage.
for stage in range(6):
    rows=[list("ssssssssssssssss") for _ in range(16)]
    for y in range(1,15):
        for x in range(1,15):rows[y][x]="m" if (y in (1,14) or x in (1,14)) else "d"
    for k,(x,y) in enumerate([(3,3),(10,3),(3,10),(10,10)]):
        for yy in range(y,y+3):
            for xx in range(x,x+3):rows[yy][xx]="g" if stage>k else "s"
    for y in range(6,10):
        for x in range(6,10):rows[y][x]="h" if stage==5 else ("g" if stage==4 else "m")
    png(A/"textures/block"/("hearth_"+str(stage)+".png"),["".join(r) for r in rows],{k:rgba(v) for k,v in {"s":"6f6b65","m":"8a8580","d":"4b4845","g":"cf9f4d","h":"f4d68b"}.items()})
    write(A/"models/block"/("hearth_"+str(stage)+".json"),{"parent":"minecraft:block/cube_column","textures":{"side":"minecraft:block/stone_bricks","end":mod("block/hearth_"+str(stage))}})
write(A/"blockstates/festival_hearth.json",{"variants":{"stage="+str(i):{"model":mod("block/hearth_"+str(i))} for i in range(6)}})
write(A/"models/item/festival_hearth.json",{"parent":mod("block/hearth_0")})
# 16px lantern glass, aged brass frame with contiguous highlights; a real 3D fixture.
lamp_rows=[
 "dddddddddddddddd","dddddddddddddddd","dddddddddddddddd","dddddddddddddddd",
 "ddddhhhmmmdddddd","ddddhggggmdddddd","ddddhglggmdddddd","ddddhglggmdddddd",
 "ddddhggllmdddddd","ddddhggglmdddddd","ddddhggggmdddddd","ddddmmmmmmdddddd",
 "dddddddddddddddd","dddddddddddddddd","dddddddddddddddd","dddddddddddddddd"]
for name,colors in [("festival_lamp_dark",{"d":"514131","h":"9d8054","m":"755c3e","g":"5f5441","l":"83725a"}),
                    ("festival_lamp_lit",{"d":"514131","h":"b39767","m":"8b6b42","g":"d49c45","l":"f0cf80"})]:
    png(A/"textures/block"/(name+".png"),lamp_rows,{k:rgba(v) for k,v in colors.items()})
    def faces(uv,texture):return {face:{"uv":uv,"texture":texture} for face in ("north","south","east","west","up","down")}
    model={"parent":"minecraft:block/block","textures":{"particle":mod("block/"+name),"glass":mod("block/"+name),"metal":"minecraft:block/stripped_spruce_log_top"},"elements":[
        {"from":[4,1,4],"to":[12,11,12],"faces":faces([3,3,11,12],"#glass")},
        {"from":[3,0,3],"to":[13,2,13],"faces":faces([0,0,10,2],"#metal")},
        {"from":[3,10,3],"to":[13,12,13],"faces":faces([0,0,10,2],"#metal")},
        {"from":[6,12,6],"to":[10,14,10],"faces":faces([0,0,4,2],"#metal")}]}
    write(A/"models/block"/(name+".json"),model)
write(A/"blockstates/festival_lamp.json",{"variants":{"lit=false":{"model":mod("block/festival_lamp_dark")},"lit=true":{"model":mod("block/festival_lamp_lit")}}})
write(A/"models/item/festival_lamp.json",{"parent":mod("block/festival_lamp_dark")})
en.update({"block.midnighteve.festival_hearth":"Festival Hearth","block.midnighteve.festival_lamp":"Festival Lamp","festival.midnighteve.0":"Bring 8 cookies for the feast.","festival.midnighteve.1":"Bring 3 lanterns to light the square.","festival.midnighteve.2":"Bring a wrapped gift for the celebration.","festival.midnighteve.3":"Bring 2 wintermarks to complete the preparations.","festival.midnighteve.4":"The square is ready. Return at midnight!","festival.midnighteve.5":"This celebration has already taken place.","festival.midnighteve.midnight":"Midnight! The winter festival has begun!"})
ru.update({"block.midnighteve.festival_hearth":"Праздничный очаг","block.midnighteve.festival_lamp":"Праздничный светильник","festival.midnighteve.0":"Принеси 8 печений для угощения.","festival.midnighteve.1":"Принеси 3 фонаря для площади.","festival.midnighteve.2":"Принеси упакованный подарок к празднику.","festival.midnighteve.3":"Принеси 2 зимние марки для завершения подготовки.","festival.midnighteve.4":"Площадь готова. Возвращайся к полуночи!","festival.midnighteve.5":"Этот праздник уже состоялся.","festival.midnighteve.midnight":"Полночь! Начинается зимний праздник!"})
write(A/"lang/en_us.json",en);write(A/"lang/ru_ru.json",ru)
write(R/"pack.mcmeta",{"pack":{"pack_format":34,"description":"MIDNIGHT Reborn"}})
text(R/"META-INF/neoforge.mods.toml",'''modLoader="javafml"
loaderVersion="[4,)"
license="MIT"
[[mods]]
modId="midnighteve"
version="0.6.0"
displayName="MIDNIGHT: Reborn"
authors="MngkLrd"
description="A winter adventure with native seasonal settlements, merchants and gifts."
[[dependencies.midnighteve]]
modId="neoforge"
type="required"
versionRange="[21.1,)"
ordering="NONE"
side="BOTH"
[[dependencies.midnighteve]]
modId="minecraft"
type="required"
versionRange="[1.21.1,1.21.2)"
ordering="NONE"
side="BOTH"
''')
text(S/"java/dev/mngklrd/midnighteve/MidnightEve.java",'''package dev.mngklrd.midnighteve;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import dev.mngklrd.midnighteve.item.WrappedGift;
import dev.mngklrd.midnighteve.block.FestivalHearth;
import dev.mngklrd.midnighteve.block.FestivalLamp;

@Mod("midnighteve")
public final class MidnightEve {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("midnighteve");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("midnighteve");
    public static final DeferredBlock<Block> GIFT_CRATE = BLOCKS.registerSimpleBlock("gift_crate",
            BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.WOOL));
    public static final DeferredItem<BlockItem> CRATE_ITEM = ITEMS.registerSimpleBlockItem(GIFT_CRATE);
    public static final DeferredItem<Item> WINTERMARK = ITEMS.registerSimpleItem("wintermark", new Item.Properties());
    public static final DeferredItem<WrappedGift> WRAPPED_GIFT = ITEMS.register("wrapped_gift",
        () -> new WrappedGift(new Item.Properties().stacksTo(16)));
    public static final DeferredBlock<FestivalHearth> HEARTH = BLOCKS.register("festival_hearth", () -> new FestivalHearth(BlockBehaviour.Properties.of().strength(3.5F,12.0F).noLootTable().lightLevel(s -> s.getValue(FestivalHearth.STAGE)>=4?12:0)));
    public static final DeferredBlock<FestivalLamp> LAMP = BLOCKS.register("festival_lamp", () -> new FestivalLamp(BlockBehaviour.Properties.of().strength(1.0F).noOcclusion().noLootTable().lightLevel(s -> s.getValue(FestivalLamp.LIT)?15:0)));
    public MidnightEve(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); bus.addListener(this::creative);
    }
    private void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) event.accept(CRATE_ITEM.get());
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) { event.accept(WINTERMARK.get()); event.accept(WRAPPED_GIFT.get()); }
    }
}
''')
text(S/"java/dev/mngklrd/midnighteve/item/WrappedGift.java",'''package dev.mngklrd.midnighteve.item;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class WrappedGift extends Item {
    public WrappedGift(Properties props) { super(props); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held=player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            int roll=server.random.nextInt(12);
            ItemStack reward=roll<4?new ItemStack(Items.COOKIE,12):
                roll<7?new ItemStack(Items.HONEY_BOTTLE,3):
                roll<10?new ItemStack(Items.LANTERN,4):
                roll==10?new ItemStack(Items.GOLD_INGOT,2):new ItemStack(Items.DIAMOND,1);
            if (!player.addItem(reward)) player.drop(reward,false);
            server.playSound(null,player.blockPosition(),SoundEvents.BELL_BLOCK,SoundSource.PLAYERS,0.7F,1.3F);
            server.sendParticles(ParticleTypes.END_ROD,player.getX(),player.getY()+1,player.getZ(),14,0.35,0.6,0.35,0.015);
            player.displayClientMessage(Component.translatable("message.midnighteve.gift"),true);
        }
        return InteractionResultHolder.sidedSuccess(held,level.isClientSide());
    }
}
''')
text(S/"java/dev/mngklrd/midnighteve/block/FestivalLamp.java",'''package dev.mngklrd.midnighteve.block;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
public final class FestivalLamp extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public FestivalLamp(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(LIT,false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(LIT); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return box(3,0,3,13,14,13); }
}
''')
text(S/"java/dev/mngklrd/midnighteve/block/FestivalHearth.java",'''package dev.mngklrd.midnighteve.block;
import dev.mngklrd.midnighteve.MidnightEve;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class FestivalHearth extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage",0,5);
    public FestivalHearth(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(STAGE,0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(STAGE); }
    private void hint(Player p,int stage) { p.displayClientMessage(Component.translatable("festival.midnighteve."+stage),true); }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if (!level.isClientSide) hint(player,state.getValue(STAGE));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit) {
        int stage=state.getValue(STAGE);
        int count=switch(stage){case 0->8;case 1->3;case 2->1;case 3->2;default->0;};
        boolean match=switch(stage){case 0->stack.is(Items.COOKIE);case 1->stack.is(Items.LANTERN);case 2->stack.is(MidnightEve.WRAPPED_GIFT.get());case 3->stack.is(MidnightEve.WINTERMARK.get());default->false;};
        if (!(level instanceof ServerLevel server)) return ItemInteractionResult.SUCCESS;
        if (!match || stack.getCount()<count) { hint(p,stage);return ItemInteractionResult.CONSUME; }
        if (!p.getAbilities().instabuild) stack.shrink(count);
        int next=stage+1;
        server.setBlockAndUpdate(pos,state.setValue(STAGE,next));
        server.playSound(null,pos,SoundEvents.BELL_BLOCK,SoundSource.BLOCKS,0.7F,0.9F+next*0.1F);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER,pos.getX()+0.5,pos.getY()+1.2,pos.getZ()+0.5,12,0.4,0.3,0.4,0.01);
        if(next>=2) light(server,pos,next==4?4:2);
        if(next==4) server.scheduleTick(pos,this,20);
        hint(p,next);
        return ItemInteractionResult.CONSUME;
    }
    private void light(ServerLevel s,BlockPos p,int n) {
        int found=0;
        for(int[] off:new int[][]{{-3,-2},{3,-2},{-3,2},{3,2},{-2,-3},{2,-3},{-2,3},{2,3}}){
            BlockPos q=p.offset(off[0],1,off[1]);BlockState b=s.getBlockState(q);
            if(b.is(MidnightEve.LAMP.get()) && found++<n) s.setBlockAndUpdate(q,b.setValue(FestivalLamp.LIT,true));
        }
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved) {
        super.onPlace(state,level,pos,old,moved);
        if(level instanceof ServerLevel s && state.getValue(STAGE)==4) s.scheduleTick(pos,this,20);
    }
    @Override protected void tick(BlockState state,ServerLevel s,BlockPos p,RandomSource r) {
        if(state.getValue(STAGE)!=4)return;
        long time=Math.floorMod(s.getDayTime(),24000L);
        if(time<17500||time>18500){s.scheduleTick(p,this,20);return;}
        s.setBlockAndUpdate(p,state.setValue(STAGE,5));
        light(s,p,4);
        s.playSound(null,p,SoundEvents.BELL_BLOCK,SoundSource.BLOCKS,2.0F,0.8F);
        s.playSound(null,p,SoundEvents.FIREWORK_ROCKET_BLAST,SoundSource.AMBIENT,1.2F,1.0F);
        s.sendParticles(ParticleTypes.FIREWORK,p.getX()+0.5,p.getY()+6,p.getZ()+0.5,100,2.0,1.6,2.0,0.15);
        s.sendParticles(ParticleTypes.END_ROD,p.getX()+0.5,p.getY()+3,p.getZ()+0.5,60,3,2,3,0.04);
        Block.popResource(s,p.above(),new ItemStack(MidnightEve.WRAPPED_GIFT.get(),3));
        for(var player:s.players()) if(player.distanceToSqr(p.getX()+0.5,p.getY()+0.5,p.getZ()+0.5)<4096) player.displayClientMessage(Component.translatable("festival.midnighteve.midnight"),false);
    }
}
''')
print("PASS: MIDNIGHT 0.6.0 architecture, resource generation and festival progression")
