#!/usr/bin/env python3
"""Frostlands 0.9: reproducible winter terrain, material art and architectural refinement."""
import expand_living as l
import json, math, struct, zlib, collections
from pathlib import Path
g,t,e=l.g,l.t,l.e
S,R,D,A,J=l.S,l.R,l.D,l.A,l.J
write,text,mc,mod=t.write,t.text,t.mc,t.mod
edit=t.edit
# The generated 1.21.1 vanilla router is a pinned input, not an external runtime dependency.
# Original custom functions are layered inside its terrain branch, before cave composition.
settings=json.loads((Path(__file__).parent/"vanilla_overworld_1_21_1.json").read_text())
def op(kind,a,b):return {"type":"minecraft:"+kind,"argument1":a,"argument2":b}
def add(a,b):return op("add",a,b)
def mul(a,b):return op("mul",a,b)
def neg(a):return mul(-1,a)
def clamp(a,lo=0,hi=1):return {"type":"minecraft:clamp","input":a,"min":lo,"max":hi}
def absolute(a):return {"type":"minecraft:abs","argument":a}
def grad(y0,y1,v0,v1):return {"type":"minecraft:y_clamped_gradient","from_y":y0,"to_y":y1,"from_value":v0,"to_value":v1}
def cache(a):return {"type":"minecraft:cache_2d","argument":a}
def noise(n,scale):return {"type":"minecraft:noise","noise":mod(n),"xz_scale":scale,"y_scale":0}
def df(n,d):write(D/"worldgen/density_function"/(n+".json"),d);return mod(n)
for n,octave,amps in [
    ("winter_fold",-9,[1.0,1.0,.5]),
    ("winter_ridge",-7,[1.0,.65,.4]),
    ("winter_shelf",-6,[1.0,.6]),
    ("winter_material",-5,[1.0,.6,.35])]:
    write(D/"worldgen/noise"/(n+".json"),{"firstOctave":octave,"amplitudes":amps})
continents="minecraft:overworld/continents"
erosion="minecraft:overworld/erosion"
ridge="minecraft:overworld/ridges"
land=df("land_mask",cache(clamp(mul(4,add(.13,continents)))))
coast=df("coastal_mask",cache(clamp(add(1,neg(mul(5,absolute(add(.17,continents))))))))
rugged=df("upland_mask",cache(clamp(mul(2.1,add(-.15,neg(erosion))))))
crest=df("crest_mask",cache(clamp(mul(2.3,add(-.35,absolute(ridge))))))
fold=df("fold_field",cache(clamp(noise("winter_fold",.9),-1,1)))
ridges=df("ridge_field",cache(add(1,neg(clamp(absolute(noise("winter_ridge",1.0)))))))
shelf=df("shelf_field",cache(clamp(noise("winter_shelf",.7),-1,1)))
# Broad glacial cuts near coasts, rounded inland shoulders, uplift only in low-erosion
# mountain climate. Continuous masks avoid abrupt walls at biome boundaries.
coastal_cut=mul(coast,add(-.30,mul(.37,fold)))
inland_roll=mul(land,mul(.29,fold))
uplift=mul(mul(land,rugged),mul(crest,add(.24,mul(.56,ridges))))
shelf_roll=mul(mul(land,clamp(add(.35,erosion))),mul(.13,shelf))
offset2d=df("winter_relief",cache(add(add(coastal_cut,inland_roll),add(uplift,shelf_roll))))
# Fade the modification out underground and above the intended terrain interval.
# This preserves the deep cave/ore system and the vanilla world height bounds.
height_mask=df("winter_height_mask",mul(grad(36,64,0,1),grad(174,230,1,0)))
offset=df("winter_terrain_delta",mul(offset2d,height_mask))
cheese=df("winter_sloped_cheese",add("minecraft:overworld/sloped_cheese",offset))
def substitute(obj):
    if isinstance(obj,str):return cheese if obj=="minecraft:overworld/sloped_cheese" else obj
    if isinstance(obj,list):return [substitute(x) for x in obj]
    if isinstance(obj,dict):return {k:substitute(v) for k,v in obj.items()}
    return obj
router=settings["noise_router"]
router["final_density"]=substitute(router["final_density"])
# The preliminary surface estimate needs to agree with the terrain adjustment.
# Keep the original floor and ceiling gradients and add a scaled version of the
# same localized relief, rather than rewriting the cave and aquifer branches.
router["initial_density_without_jaggedness"]=add(router["initial_density_without_jaggedness"],mul(2.4,offset))
# Distinct surface geology, applied only to the uppermost natural stone layer.
# Never rewrite existing chunks or globally alter a vanilla biome.
def condition(test,rule):return {"type":"minecraft:condition","if_true":test,"then_run":rule}
def block(n,**props):
    st={"Name":mc(n)}
    if props:st["Properties"]=props
    return {"type":"minecraft:block","result_state":st}
def seq(*r):return {"type":"minecraft:sequence","sequence":list(r)}
def biome(*names):return {"type":"minecraft:biome","biome_is":[mod(n) for n in names]}
def threshold(lo,hi):return {"type":"minecraft:noise_threshold","noise":mod("winter_material"),"min_threshold":lo,"max_threshold":hi}
def floor(depth=0):return {"type":"minecraft:stone_depth","offset":depth,"add_surface_depth":False,"secondary_depth_range":0,"surface_type":"floor"}
dry={"type":"minecraft:water","offset":0,"surface_depth_multiplier":0,"add_stone_depth":False}
high={"type":"minecraft:y_above","anchor":{"absolute":106},"surface_depth_multiplier":0,"add_stone_depth":False}
rules=seq(
 condition(biome("firlight_woods"),condition(dry,seq(condition(threshold(-1e9,-.18),block("podzol",snowy="false")),condition(threshold(.30,1e9),block("coarse_dirt")),block("grass_block",snowy="false")))),
 condition(biome("peppermint_grove"),condition(dry,seq(condition(threshold(-1e9,-.35),block("podzol",snowy="false")),block("grass_block",snowy="false")))),
 condition(biome("starlit_steppe"),condition(dry,seq(condition(threshold(.45,1e9),block("midnighteve:froststone")),block("grass_block",snowy="false")))),
 condition(biome("crystal_crown"),condition(dry,seq(condition(high,seq(condition(threshold(-1e9,-.26),block("packed_ice")),condition(threshold(.28,1e9),block("calcite")),block("midnighteve:froststone"))),condition(threshold(.25,1e9),block("midnighteve:froststone")),block("grass_block",snowy="false")))),
 condition(biome("mirror_fjords"),seq(condition(dry,seq(condition(threshold(.15,1e9),block("midnighteve:froststone")),condition(threshold(-1e9,-.28),block("gravel")),block("grass_block",snowy="false"))),condition(threshold(.08,1e9),block("gravel")))),
 condition(biome("silent_snowfields"),condition(dry,seq(condition(threshold(.45,1e9),block("coarse_dirt")),condition(threshold(-1e9,-.30),block("snow_block")),block("grass_block",snowy="false"))))
)
# Vanilla bedrock, top-level surface conditions and deepslate remain in original order.
top=settings["surface_rule"]["sequence"][1]["then_run"]["sequence"]
top.insert(0,condition(floor(),rules))
write(D/"worldgen/noise_settings/frostlands.json",settings)
preset=D/"worldgen/world_preset/endless_eve.json"
p=json.loads(preset.read_text());p["dimensions"]["minecraft:overworld"]["generator"]["settings"]=mod("frostlands");write(preset,p)
# New stone can be mined and processed; useful as an architectural material.
# Hand-defined mineral seams: broken, offset clusters, no outline grid or random speckles.
stone=["mmhhmmssssmmmlmm","mhhmmmssmmmllmmm","mhhmmmmsmmllhmmm","smmmmmmmmllhhmms","ssmmmhhmmmhmmsss","mssmmhhmmmmmsssm","mmssmmmhmmmmmsmm","mmmssmmmmmmmmmmm","mmmmmmmssmmhmmmm","hmmmmmmssmhhmmmh","hhmmmmmmsmhhmmhh","mmhmmssmmmmmmmmm","mmmmmssmmmmmmsss","mmmmmmmmhhmmsssm","mmlmmmhhhmmmmmsm","mllmmmhhmmmmmmmm"]
brick=["ssssssssssssssss","sllhhhmmslhhhmms","shhmmmmmslhhmmms","shmmmssmsmmmmmmm","smmmmssmsmmmhhms","smmmmsmmsmmhhhms","smmmmmmssmmmmsss","ssssssssssssssss","hhmsllhhhmslhhhm","hmmslhhmmmslhhmm","mmmsmmmmmmsmmmmm","mmmsmmssmmsmmmmm","mmmsmmssmmsmmmhh","msssmmmmmmssmmhm","mmssssmmmmmsmmms","ssssssssssssssss"]
palette={"m":g.rgba("9ba8a9"),"h":g.rgba("aab7b6"),"l":g.rgba("b8c4bf"),"s":g.rgba("859598")}
for n,rows in [("froststone",stone),("froststone_bricks",brick)]:
    g.png(A/"textures/block"/(n+".png"),rows,palette)
    write(A/"blockstates"/(n+".json"),{"variants":{"":{"model":mod("block/"+n)}}})
    write(A/"models/block"/(n+".json"),{"parent":"minecraft:block/cube_all","textures":{"all":mod("block/"+n)}})
    write(A/"models/item"/(n+".json"),{"parent":mod("block/"+n)})
    write(D/"loot_table/blocks"/(n+".json"),{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":mod(n)}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
write(R/"data/minecraft/tags/block/mineable/pickaxe.json",{"replace":False,"values":[mod("froststone"),mod("froststone_bricks")]})
write(R/"data/minecraft/tags/block/needs_stone_tool.json",{"replace":False,"values":[mod("froststone"),mod("froststone_bricks")]})
t.recipe("froststone_bricks",["SS","SS"],{"S":"midnighteve:froststone"},4)
write(D/"recipe/froststone_bricks_stonecutting.json",{"type":"minecraft:stonecutting","ingredient":{"item":mod("froststone")},"result":{"id":mod("froststone_bricks"),"count":1}})
text(J/"world/FrostMaterials.java",'''package dev.mngklrd.midnighteve.world;
import dev.mngklrd.midnighteve.MidnightEve;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;
public final class FrostMaterials {
    public static final DeferredBlock<Block> STONE=MidnightEve.BLOCKS.registerSimpleBlock("froststone",BlockBehaviour.Properties.of().strength(1.5F,6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<Block> BRICKS=MidnightEve.BLOCKS.registerSimpleBlock("froststone_bricks",BlockBehaviour.Properties.of().strength(1.5F,6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredItem<BlockItem> STONE_ITEM=MidnightEve.ITEMS.registerSimpleBlockItem(STONE);
    public static final DeferredItem<BlockItem> BRICKS_ITEM=MidnightEve.ITEMS.registerSimpleBlockItem(BRICKS);
    public static void init(IEventBus bus){bus.addListener(FrostMaterials::creative);}
    private static void creative(BuildCreativeModeTabContentsEvent e){
        if(e.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)){e.accept(STONE_ITEM.get());e.accept(BRICKS_ITEM.get());}
    }
}
''')
edit(J/"MidnightEve.java","        dev.mngklrd.midnighteve.living.LivingContent.init(bus);","        dev.mngklrd.midnighteve.living.LivingContent.init(bus);\n        dev.mngklrd.midnighteve.world.FrostMaterials.init(bus);")
# Original 16x16 texture revision: connected clusters and a material-specific ramp.
# Item sprites are centered vertically and horizontally at native resolution.
def sprite(name,rows,colors,kind="item"):
    assert len(rows)<=16 and all(len(r)<=16 for r in rows),(name,rows)
    w=max(map(len,rows));y=(16-len(rows))//2
    rows=["."*16]*y+[r.center(w,".").center(16,".") for r in rows]+["."*16]*(16-len(rows)-y)
    g.png(A/"textures"/kind/(name+".png"),rows,{".":(0,0,0,0),**{k:g.rgba(v) for k,v in colors.items()}})
sprite("gingerbread",[
"....oooo....","...ohhho....","...ohmho....","....hho.....","..oohhooo...","ohhhmmhhho..",
".oohhhooo...","...hMho.....","...hLho.....","...hMho.....","...hho......","..oh.oh.....","..oh.oh.....","..oo.oo....."],
{"o":"75492f","h":"b7804b","m":"a36a3b","M":"d8b17a","L":"ead6a9"})
sprite("hot_cocoa",[
"...s..s.....","...s.s......","..s..s......",".ooooooo....",".ohhhhhho...","ohhhmmhhoo..",
"ohhmmmhhoho.","ohhmmmhooho.","ohhmmmhhoo..","ohhdddho....",".ohddho.....","..oooo......"],
{"s":"c4ceca","o":"674e3e","h":"bb946b","m":"8d6044","d":"77503a"})
sprite("winter_journal",[
"..ooooooo...","..ohhhhmmo..","..ohllllmo..","..ohllllmo..","..ohlgllmo..","..ohggglmo..",
"..ohlgglmo..","..ohlgllmo..","..ohllllmo..","..ohllllmo..","..ohmmmmoo..","..oooooooo.."],
{"o":"5d4433","h":"b49063","m":"96714b","l":"d5c39b","g":"73875a"})
sprite("sealed_parcel",[
"....oo......","...ohhoo....","..ohhhggoo..",".ohhhggmmmo.","ohhhggmmmddo","ohhgGgmmmddo",
"ohhhggmmmddo","ohhhggmmmddo","ohhhggmmmddo",".ohhggmmmdo.","..oooooooo.."],
{"o":"795b40","h":"bc9c72","m":"ad8961","d":"967451","g":"a65b4a","G":"c68165"})
# Rebuild decoration models with exact-sized UV windows and explicit orientation.
# Unlike the previous model, every material sample is chosen to match the visible face.
def elem(lo,hi,tex,uvs=None,rotation=None):
    dx,dy,dz=[hi[i]-lo[i] for i in range(3)]
    fs={}
    for f,w,h in (("north",dx,dy),("south",dx,dy),("east",dz,dy),("west",dz,dy),("up",dx,dz),("down",dx,dz)):
        x,y=(uvs or {}).get(f,(0,0))
        assert x+w<=16 and y+h<=16
        fs[f]={"texture":"#"+tex,"uv":[x,y,x+w,y+h]}
    o={"from":lo,"to":hi,"faces":fs}
    if rotation:o["rotation"]=rotation
    return o
def model(n,els,tex):
    write(A/"models/block"/(n+".json"),{"parent":"minecraft:block/block","textures":{"particle":mod("block/"+n),**tex},"elements":els})
orn=[
"rrrrrrrrrrrrrrrr","rrhhhhhrrrrrrrrr","rhhhhhrrrrrrrrrr","rhhllhrrrrrrrrrr",
"rhhllhrrrrrrrrrr","rrhhhrrrrrrrrrrr","rrrrrrrrrrrrrrrr","rrrrrrrrrrrrrrrr",
"rrrrrrrrrrrrrrrr","rrrrrrrrrrrrrrrr","rrrrrrrrrrrrrrrr","rrrrrrrrrrrrrrrr",
"rrrrrrddrrrrrrrr","rrrrrdddrrrrrrrr","rrrrrrddrrrrrrrr","rrrrrrrrrrrrrrrr"]
g.png(A/"textures/block/ornament.png",orn,{k:g.rgba(v) for k,v in {"r":"a74e46","h":"c56659","l":"dda18c","d":"883d38"}.items()})
model("ornament",[elem([4,1,4],[12,9,12],"red",{"north":(0,0),"south":(8,8),"east":(8,4),"west":(0,4),"up":(0,0),"down":(8,8)}),elem([6,9,6],[10,11,10],"metal")],{"red":mod("block/ornament"),"metal":"minecraft:block/gold_block"})
wax=[
"hhhhhhhhhhhhhhhh","hhllhhhhhhhhhhhh","hhllhhhhhhhhhhhh","hhhhhhhhhhhhhhhh",
"hhhhhhhhhhhhhhhh","hhhhhhhmmhhhhhhh","hhhhhhhmmhhhhhhh","hhhhhhhhhhhhhhhh",
"hhhhhhhhhhhhhhhh","hhhhhhhhhhhhhhhh","hhhhhhhhhhhhhhhh","hhhhhhhhhhhhhhhh",
"hhhhhhhhhhhhhhhh","hhhhhhhhhhhhhhhh","hhhhhhhhhhhhhhhh","hhhhhhhhhhhhhhhh"]
g.png(A/"textures/block/candle_cluster.png",wax,{k:g.rgba(v) for k,v in {"h":"d7c196","l":"e9d7b1","m":"c0a77a"}.items()})
model("candle_cluster",[elem([3,0,3],[13,1,13],"metal"),elem([4,1,5],[7,9,8],"wax"),elem([9,1,5],[12,6,8],"wax"),elem([7,1,10],[10,11,13],"wax"),elem([5,9,6],[6,11,7],"flame"),elem([10,6,6],[11,8,7],"flame"),elem([8,11,11],[9,13,12],"flame")],{"wax":mod("block/candle_cluster"),"metal":"minecraft:block/copper_block","flame":"minecraft:block/honeycomb_block"})
# Garland shows deliberate short needle groups and berries. One long body and two hanging ends.
fir=[
"gggggggggggggggg","gghhhgggghhggggg","ghhhhggghhhggggg","gggggggggggddggg",
"gggddgggggdddggg","gggddggggggggggg","gggggrrggggggggg","gggggrrggghhhggg",
"ggggggggghhhgggg","gghhgggggggggggg","ghhhggddgggggggg","ggggggddgggggggg",
"gggggggggggggggg","gggggggggggggggg","gggggggggggggggg","gggggggggggggggg"]
g.png(A/"textures/block/garland.png",fir,{k:g.rgba(v) for k,v in {"g":"4f6848","h":"688254","d":"3b523a","r":"a64f43"}.items()})
model("garland",[elem([0,3,6],[16,7,10],"fir",{"north":(0,0),"south":(0,6),"east":(0,8),"west":(4,8),"up":(0,2),"down":(0,10)}),elem([1,1,5],[5,5,11],"fir",{"north":(0,4),"south":(4,4)}),elem([11,1,5],[15,5,11],"fir",{"north":(8,4),"south":(10,4)}),elem([7,0,7],[9,3,9],"metal")],{"fir":mod("block/garland"),"metal":"minecraft:block/copper_block"})
# Star uses one central body and two justified rotated elements, without stair-step modeling.
gold=[
"mmmmmmmmmmmmmmmm","mmhhhhmmmmmmmmmm","mhhhhhmmmmmmmmmm","mhhllhmmmmmmmmmm",
"mmhhmmmmmmmmmmmm","mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm",
"mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm",
"mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm"]
g.png(A/"textures/block/tree_star.png",gold,{k:g.rgba(v) for k,v in {"m":"bb9354","h":"ddbc79","l":"efd7a2"}.items()})
model("tree_star",[elem([7,0,7],[9,4,9],"metal"),elem([6,4,7],[10,14,9],"gold"),elem([6,4,7],[10,14,9],"gold",rotation={"origin":[8,9,8],"axis":"z","angle":45,"rescale":False}),elem([6,4,7],[10,14,9],"gold",rotation={"origin":[8,9,8],"axis":"z","angle":-45,"rescale":False})],{"gold":mod("block/tree_star"),"metal":"minecraft:block/gold_block"})
# Improved native building templates: mineral foundations on highland monuments,
# no wholesale replacement of wood or changing doorway/roof coordinates.
# Read/write the generated NBT via a minimal typed NBT parser; modify only palette
# references in designated structures and explicitly add roof snow on supported tops.
import gzip
def read_nbt(blob):
    off=0
    def take(n):
        nonlocal off
        r=blob[off:off+n];off+=n
        if len(r)!=n:raise ValueError("truncated NBT")
        return r
    def num(fmt):return struct.unpack(fmt,take(struct.calcsize(fmt)))[0]
    def utf():return take(num(">H")).decode("utf8")
    def val(k):
        if k==1:return num(">b")
        if k==3:return num(">i")
        if k==5:return num(">f")
        if k==6:return num(">d")
        if k==8:return utf()
        if k==9:
            q=num(">b");n=num(">i");return (q,[val(q) for _ in range(n)])
        if k==10:
            d={}
            while True:
                q=num(">b")
                if q==0:return d
                name=utf();d[name]=(q,val(q))
        raise ValueError("Unsupported type "+str(k))
    assert num(">b")==10
    utf();root=val(10);assert off==len(blob);return root
def rewrite_template(path,stone_trim):
    root=read_nbt(gzip.decompress(path.read_bytes()))
    palette=root["palette"][1][1];blocks=root["blocks"][1][1];size=root["size"][1][1]
    idx={tuple(b["pos"][1][1]):b for b in blocks}
    # Replace a subset of native masonry with related native material (same full-cube geometry).
    if stone_trim:
        for b in palette:
            if b["Name"][1]=="minecraft:stone_bricks":b["Name"]=g.Str(mod("froststone_bricks"))
    def add_palette(name,props):
        entry={"Name":g.Str(name),"Properties":g.C({k:g.Str(v) for k,v in props.items()})}
        if entry not in palette:palette.append(entry)
        return palette.index(entry)
    snow=add_palette("minecraft:snow",{"layers":"2"})
    tops={}
    for (x,y,z),b in idx.items():
        name=palette[b["state"][1]]["Name"][1]
        if name=="minecraft:air":continue
        if y>tops.get((x,z),(-1,""))[0]:tops[x,z]=(y,name)
    # Snow only on full roof blocks high above the floor, not on doors, paths,
    # transparent glass, lamps, foliage or anything with an unknown support shape.
    roofs={"minecraft:spruce_planks","minecraft:dark_oak_planks","minecraft:stone_bricks","minecraft:polished_andesite","minecraft:oxidized_copper","minecraft:weathered_copper",mod("froststone_bricks")}
    added=0
    for (x,z),(y,name) in sorted(tops.items()):
        if y<9 or y+1>=size[1] or name not in roofs:continue
        if ((x//3+z//4)%3)==1:continue
        pos=(x,y+1,z)
        if pos in idx and palette[idx[pos]["state"][1]]["Name"][1]!="minecraft:air":continue
        if pos in idx:idx[pos]["state"]=g.I(snow)
        else:blocks.append({"pos":g.L(3,list(pos)),"state":g.I(snow)})
        added+=1
    root["blocks"]=g.L(10,blocks)
    path.write_bytes(gzip.compress(b"\x0a\x00\x00"+g.payload(g.C(root)),mtime=0))
    return added
snow_total=0
for p in sorted((D/"structure").glob("*.nbt")):
    snow_total+=rewrite_template(p,p.stem in ("mountain_refuge","northwatch","stargazer_observatory","solstice_shrine"))
print("PASS: architectural material trim and supported roof snow:",snow_total)
# Expanded art audit: check actual PNG header, palette size, and UV dimensions
# for the explicitly revised models. Artistic quality is still manual.
for p in (A/"textures").rglob("*.png"):
    b=p.read_bytes();assert b[:8]==b"\x89PNG\r\n\x1a\n",p
    w,h=struct.unpack(">II",b[16:24])
    assert (w,h)==((64,32) if p.name=="hearthling.png" else (16,16)),(p,w,h)
for n in ("ornament","candle_cluster","garland","tree_star"):
    o=json.loads((A/"models/block"/(n+".json")).read_text())
    for e in o["elements"]:
        dx,dy,dz=[e["to"][i]-e["from"][i] for i in range(3)]
        for side,face in e["faces"].items():
            u,v,U,V=face["uv"];expected={"north":(dx,dy),"south":(dx,dy),"east":(dz,dy),"west":(dz,dy),"up":(dx,dz),"down":(dx,dz)}[side]
            assert (U-u,V-v)==expected and 0<=u<=U<=16 and 0<=v<=V<=16,(n,side,face)
g.en.update({"block.midnighteve.froststone":"Froststone","block.midnighteve.froststone_bricks":"Froststone Bricks","generator.midnighteve.endless_eve":"MIDNIGHT: Frostlands"})
g.ru.update({"block.midnighteve.froststone":"Морозный камень","block.midnighteve.froststone_bricks":"Кирпичи из морозного камня","generator.midnighteve.endless_eve":"MIDNIGHT: Frostlands"})
for n,lang in [("en_us",g.en),("ru_ru",g.ru)]:write(A/"lang"/(n+".json"),lang)
edit(R/"META-INF/neoforge.mods.toml",'version="0.8.0"','version="0.9.0"')
for p in R.rglob("*.json"):json.loads(p.read_text(encoding="utf8"))
print("PASS: independent Frostlands router, 11 density functions, six material rules, two building materials, revised native art")
