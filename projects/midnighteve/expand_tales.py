#!/usr/bin/env python3
"""MIDNIGHT 0.7 content layer. Authoring sources, not runtime Python."""
import generate_reborn as g
from pathlib import Path
import json
S,R,D,A=g.S,g.R,g.D,g.A
T=g.Template
write,text,mc,mod=g.write,g.text,g.mc,g.mod
def edit(p,old,new):
    s=p.read_text(encoding="utf8")
    assert old in s, (str(p),old[:100])
    p.write_text(s.replace(old,new,1),encoding="utf8")
def add_json(p,fn):
    d=json.loads(p.read_text(encoding="utf8"));fn(d);write(p,d)
J=S/"java/dev/mngklrd/midnighteve"

# Additional structures are genuinely different footprints and functions.
# All templates use the game's native palette and entities.
def lamp(t,x,z,y=4):
    t.put(x,y,z,"stone_brick_wall",up=True,north="none",south="none",east="none",west="none",waterlogged=False)
    t.put(x,y+1,z,"lantern",hanging=False,waterlogged=False)
def snow(t,x,z,y):
    t.put(x,y,z,"snow",layers=2)
def stall(t,x,z,kind):
    t.fill(x,0,z,x+6,2,z+4,"cobblestone")
    t.fill(x,3,z,x+6,3,z+4,"spruce_planks")
    for dx in (0,6):
      for dz in (0,4):t.fill(x+dx,4,z+dz,x+dx,7,z+dz,"stripped_spruce_log",axis="y")
    for dx in range(7):
      for dz in range(5):
        t.put(x+dx,8,z+dz,"red_wool" if dx%2==0 else "white_wool")
        if dz==0:t.put(x+dx,7,z+dz,"red_wool" if dx%2==0 else "white_wool")
    for dx in range(1,6):
        t.put(x+dx,4,z+3,"spruce_planks")
    t.put(x+1,5,z+3,kind)
    t.put(x+5,5,z+3,"midnighteve:ornament")
    t.put(x+3,7,z+2,"lantern",hanging=True,waterlogged=False)
def register(n,allowed,spacing,sep,salt,ru):
    write(D/"tags/worldgen/biome"/("has_structure/"+n+".json"),{"replace":False,"values":[mod(b) for b in allowed]})
    write(D/"worldgen/template_pool"/(n+".json"),{"fallback":"minecraft:empty","elements":[{"weight":1,"element":{"element_type":"minecraft:single_pool_element","location":mod(n),"processors":"minecraft:empty","projection":"rigid"}}]})
    write(D/"worldgen/structure"/(n+".json"),{"type":"minecraft:jigsaw","biomes":"#"+mod("has_structure/"+n),"step":"surface_structures","spawn_overrides":{},"terrain_adaptation":"beard_box","start_pool":mod(n),"size":0,"start_height":{"absolute":-3},"project_start_to_heightmap":"WORLD_SURFACE_WG","max_distance_from_center":80,"use_expansion_hack":False})
    write(D/"worldgen/structure_set"/(n+".json"),{"structures":[{"structure":mod(n),"weight":1}],"placement":{"type":"minecraft:random_spread","spacing":spacing,"separation":sep,"salt":salt}})
    g.en["structure.midnighteve."+n]=n.replace("_"," ").title()
    g.ru["structure.midnighteve."+n]=ru

# A small hamlet: actual houses, two shops, enclosed square, fenced garden.
hamlet=T(55,23,45)
g.path(hamlet,7,19,47,25);g.path(hamlet,25,3,31,41)
g.path(hamlet,8,3,25,5);g.path(hamlet,31,3,47,5)
g.cabin(hamlet,4,7,11,10,"Baker","white_terracotta","dark_oak","farmer")
g.cabin(hamlet,19,7,9,10,"Chandler","spruce_planks","spruce","cleric")
g.cabin(hamlet,39,7,11,10,"Giftkeeper","red_terracotta","dark_oak","librarian")
g.cabin(hamlet,5,29,13,11,"Woodworker","spruce_planks","dark_oak","toolsmith")
g.cabin(hamlet,38,29,11,11,"Postmaster","white_terracotta","spruce","librarian")
g.path(hamlet,8,26,11,27);g.path(hamlet,42,26,44,27)
g.path(hamlet,7,5,10,6);g.path(hamlet,22,5,24,6);g.path(hamlet,43,5,45,6)
g.tree(hamlet,28,22)
g.festival(hamlet,28,28)
for x,z in ((6,20),(48,20),(20,26),(35,26),(26,37),(31,37)):lamp(hamlet,x,z)
for x in range(21,25):
    for z in range(32,39):
        hamlet.put(x,3,z,"farmland",moisture=7)
        hamlet.put(x,4,z,"wheat",age=(x+z)%3+5)
hamlet.fill(25,3,32,25,3,38,"water",level=0)
for x in range(20,27):
    hamlet.put(x,4,31,"spruce_fence",north=False,south=False,east=True,west=True,waterlogged=False)
    hamlet.put(x,4,39,"spruce_fence",north=False,south=False,east=True,west=True,waterlogged=False)
hamlet.save("everlight_hamlet")
register("everlight_hamlet",["peppermint_grove","starlit_steppe"],49,17,712023,"Деревня вечных огней")

# Independent outdoor fair: multiple awnings, central skating pond and refreshment hall.
fair=T(43,22,39)
g.path(fair,6,7,36,31)
for x,z,kind in [(5,7,"midnighteve:gingerbread"),(5,24,"midnighteve:gift_crate"),(31,7,"midnighteve:candle_cluster"),(31,24,"midnighteve:garland")]:
    # Only block items as fixtures (the food stall has a cake instead).
    stall(fair,x,z,"cake" if kind=="midnighteve:gingerbread" else kind)
fair.fill(15,0,12,27,2,26,"stone")
for x in range(15,28):
 for z in range(12,27):
    if ((x-21)/6.0)**2+((z-19)/7.0)**2<=1:
        fair.put(x,3,z,"packed_ice")
        fair.put(x,4,z,"air")
        fair.put(x,5,z,"air")
for x,z in ((14,11),(28,11),(14,27),(28,27)):lamp(fair,x,z)
g.festival(fair,21,31)
fair.resident(8,4,9,"Baker","farmer")
fair.resident(34,4,9,"Chandler","cleric")
fair.resident(8,4,26,"Giftkeeper","librarian")
fair.resident(34,4,26,"Postmaster","librarian")
fair.save("lantern_fair")
register("lantern_fair",["starlit_steppe"],42,13,712024,"Фонарная ярмарка")

# Mountain refuge: closed stone base, distinct cross-gable roof and sheltered fire.
refuge=T(29,22,27)
g.path(refuge,8,3,20,7)
g.cabin(refuge,7,9,15,13,"Ranger","stone_bricks","spruce","butcher")
refuge.fill(3,0,8,5,3,20,"cobblestone")
for z in range(8,21):
    refuge.put(4,4,z,"spruce_planks")
for z in (8,14,20):
    refuge.fill(3,5,z,3,8,z,"stripped_spruce_log",axis="y")
    refuge.fill(5,5,z,5,8,z,"stripped_spruce_log",axis="y")
for x in range(2,7):
    for z in range(7,22):refuge.put(x,9+(2-abs(x-4)),z,"spruce_planks")
refuge.put(4,5,11,"campfire",lit=True,signal_fire=False,waterlogged=False,facing="north")
refuge.put(4,5,17,"barrel",facing="up",open=False)
lamp(refuge,10,6);lamp(refuge,18,6)
refuge.save("mountain_refuge")
register("mountain_refuge",["crystal_crown","silent_snowfields"],37,12,712025,"Горное убежище")

# Separate longhouse: timber great hall with hearth and dining benches.
longhouse=T(31,24,37)
g.path(longhouse,11,3,19,6)
g.cabin(longhouse,6,8,19,24,"Baker","spruce_planks","dark_oak","farmer")
# original cabin ridge y19 fits the 24-high template; build a genuinely long dining room.
for z in (14,19,24):
    for x in range(11,20):
        longhouse.put(x,4,z,"spruce_fence",north=False,south=False,east=False,west=False,waterlogged=False)
        longhouse.put(x,5,z,"spruce_pressure_plate",powered=False)
        longhouse.put(x,4,z-1,"spruce_stairs",facing="south",half="bottom",shape="straight",waterlogged=False)
        longhouse.put(x,4,z+1,"spruce_stairs",facing="north",half="bottom",shape="straight",waterlogged=False)
    longhouse.put(15,6,z,"midnighteve:candle_cluster")
for z in (12,18,24,29):longhouse.put(15,8,z,"lantern",hanging=True,waterlogged=False)
longhouse.resident(20,4,14,"Giftkeeper","librarian")
longhouse.save("yuletide_longhouse")
register("yuletide_longhouse",["firlight_woods","peppermint_grove"],40,14,712026,"Праздничный длинный дом")

# Frozen lake rest stop, built on a timber platform with a tiny fish shop.
icehouse=T(29,19,23)
icehouse.fill(3,0,4,25,2,18,"packed_ice")
icehouse.fill(4,3,5,24,3,17,"spruce_planks")
g.cabin(icehouse,6,7,11,9,"Fisher","stripped_spruce_wood","spruce","fisherman")
for z in (6,11,16):
    for x in (20,23):
        icehouse.fill(x,4,z,x,7,z,"stripped_spruce_log",axis="y")
for x in range(19,25):
 for z in range(5,18):icehouse.put(x,8,z,"spruce_planks")
for z in (7,12,16):icehouse.put(22,4,z,"barrel",facing="up",open=False)
lamp(icehouse,18,6);lamp(icehouse,18,16)
icehouse.save("icefish_station")
register("icefish_station",["mirror_fjords"],36,11,712027,"Ледовая рыбацкая станция")

# Authored 16px sprites. No random colors, pixel scaling, AA or generated noise.
def sprite(name,shape,pal,kind="item"):
    rows=shape.strip().splitlines()
    assert all(len(row)<=16 for row in rows) and len(rows)<=16,(name,rows)
    rows=[row.center(16,".") for row in rows]
    while len(rows)<16: rows.insert(0,".") if False else rows.append("."*16)
    g.png(A/"textures"/kind/(name+".png"),rows,{".":(0,0,0,0),**{k:g.rgba(v) for k,v in pal.items()}})
def item(name,en,ru,shape,palette):
    sprite(name,shape,palette)
    write(A/"models/item"/(name+".json"),{"parent":"minecraft:item/generated","textures":{"layer0":mod("item/"+name)}})
    g.en["item.midnighteve."+name]=en;g.ru["item.midnighteve."+name]=ru
def recipe(name,pattern,keys,count=1):
    write(D/"recipe"/(name+".json"),{"type":"minecraft:crafting_shaped","pattern":pattern,"key":{k:{"item":mc(v)} for k,v in keys.items()},"result":{"id":mod(name),"count":count}})
item("gingerbread","Gingerbread","Имбирное печенье","""
..oooo..
.ohhhho.
.ohmMho.
..ohho..
oooMMooo
ohhmmhho
.ohLLho.
..ohho..
..ohho..
.oh..ho.
.oo..oo.
""",{"o":"72442d","h":"b77a45","m":"9a6037","M":"e3b879","L":"e9d8b8"})
item("hot_cocoa","Hot Cocoa","Горячее какао","""
...ss.ss...
...s..s....
..oooooooo.
..ollllooo.
..ohmmhoho.
..ohmmhoho.
..ohmdhoho.
..ohmdhoo..
..ohddo....
..ooooo....
""",{"s":"b7c0bf","o":"644533","l":"d6c4a2","h":"af7955","m":"7d5036","d":"906044"})
item("winter_journal","Winter Journal","Зимний журнал","""
..oooooooo..
..ohhhhmmo..
..ohllllmo..
..ohllg lmo.
..ohllllmo..
..ohlgllmo..
..ohllllmo..
..ohllllmo..
..ohmmmmmo..
..oooooooo..
""".replace(" ","l"),{"o":"553b2a","h":"ae8150","m":"8d623b","l":"d9c69a","g":"779069"})
item("sealed_parcel","Sealed Parcel","Запечатанная посылка","""
..oooooooo..
..ohhhghho..
..ohhhghho..
..ohmmgmmo..
..ogggGggo..
..ogggGggo..
..ohmmgmmo..
..ohmmgmmo..
..ohhdgdho..
..oooooooo..
""",{"o":"73563b","h":"bb9866","m":"ad8657","d":"987047","g":"9c5848","G":"bd7461"})
recipe("gingerbread",["WSW"," E "],{"W":"wheat","S":"sugar","E":"egg"},4)
recipe("hot_cocoa",[" C "," S "," B "],{"C":"cocoa_beans","S":"sugar","B":"glass_bottle"})
recipe("winter_journal",[" P "," B "," S "],{"P":"paper","B":"book","S":"spruce_sapling"})
recipe("sealed_parcel",["PPP","PGP"," S "],{"P":"paper","G":"midnighteve:wrapped_gift","S":"string"})
recipe("wrapped_gift",["PPP","PWP"," S "],{"P":"paper","W":"red_wool","S":"string"})
# Carefully authored small 3D fixtures; shapes are integer pixel geometry.
def decor(name,en,ru,shape,palette,elements,light,craft):
    sprite(name,shape,palette,"block")
    faces={}
    for face in ("north","south","east","west","up","down"):
        faces[face]={"texture":"#surface"}
    models=[]
    for lo,hi,tex in elements:
        dx,dy,dz=[hi[i]-lo[i] for i in range(3)]
        # Each side uses a matching 1:1 texel footprint.
        ff={}
        for face,w,h in (("north",dx,dy),("south",dx,dy),("east",dz,dy),("west",dz,dy),("up",dx,dz),("down",dx,dz)):
            ff[face]={"texture":"#"+tex,"uv":[0,0,w,h]}
        models.append({"from":lo,"to":hi,"faces":ff})
    write(A/"models/block"/(name+".json"),{"parent":"minecraft:block/block","textures":{"particle":mod("block/"+name),"surface":mod("block/"+name),"brass":"minecraft:block/gold_block","leaf":"minecraft:block/moss_block","wax":"minecraft:block/honeycomb_block"},"elements":models})
    write(A/"models/item"/(name+".json"),{"parent":mod("block/"+name)})
    write(A/"blockstates"/(name+".json"),{"variants":{"":{"model":mod("block/"+name)}}})
    write(D/"loot_table/blocks"/(name+".json"),{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":mod(name)}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
    g.en["block.midnighteve."+name]=en;g.ru["block.midnighteve."+name]=ru
    recipe(name,*craft)
decor("ornament","Glass Ornament","Ёлочная игрушка","""
rrrrrrrrrrrrrrrr
rrrhhhhrrrrrrrrr
rrhhllhhrrrrrrrr
rrhllhhrrrrrrrrr
rrhhhhrrrrrrrrrr
rrrrrrrrrrddddrr
rrrrrrrrrrddddrr
rrrrrrrrrrrrrrrr
rrrrrrrrrrrrrrrr
rrrrrrrrrrrrrrrr
rrrrrrrrrrrrrrrr
rrrrrrrrrrrrrrrr
rrrrrrrrrrrrrrrr
rrrrrrrrrrrrrrrr
rrrrrrrrrrrrrrrr
rrrrrrrrrrrrrrrr
""",{"r":"ab473e","h":"cb6454","l":"e2a18a","d":"863b35"},
 [([5,1,5],[11,9,11],"surface"),([6,0,6],[10,1,10],"surface"),([7,9,7],[9,11,9],"brass")],0,([" G "," R "," I "],{"G":"gold_nugget","R":"red_dye","I":"glass"},2))
decor("candle_cluster","Candle Cluster","Праздничные свечи","""
hhhhhhhhhhhhhhhh
hhhhhllllhhhhhhh
hhhhllhhhmmmmhhh
hhhhhhhhmmmmhhhh
hhhhhhhhhhhhhhhh
hhhhhhhhhhhhhhhh
hhhhhhhhhhhhhhhh
hhhhhhhhhhhhhhhh
hhhhhhhhhhhhhhhh
hhhhhhhhhhhhhhhh
hhhhhhhhhhhhhhhh
hhhhhhhhhhhhhhhh
hhhhhhhhhhhhhhhh
hhhhhhhhhhhhhhhh
hhhhhhhhhhhhhhhh
hhhhhhhhhhhhhhhh
""",{"h":"d4bd8b","l":"eee0b8","m":"b79b66"},
 [([3,0,3],[13,1,13],"brass"),([4,1,5],[7,9,8],"surface"),([9,1,5],[12,6,8],"surface"),([7,1,10],[10,11,13],"surface"),([5,9,6],[6,11,7],"brass"),([10,6,6],[11,8,7],"brass"),([8,11,11],[9,13,12],"brass")],10,([" C ","CCC"," N "],{"C":"candle","N":"gold_nugget"},1))
decor("garland","Fir Garland","Хвойная гирлянда","""
gggggggggggggggg
gghhgggggghhgggg
ghhhggggghhhgggg
ggggggrrgggggggg
ggggggrrgggggggg
ggddgggggghhgggg
ggddgggggghhgggg
gggggggggggggggg
gggggggggggggggg
gggggggggggggggg
gggggggggggggggg
gggggggggggggggg
gggggggggggggggg
gggggggggggggggg
gggggggggggggggg
gggggggggggggggg
""",{"g":"496344","h":"66814f","d":"364e38","r":"a5483d"},
 [([0,3,6],[16,7,10],"surface"),([1,1,5],[5,5,11],"leaf"),([11,1,5],[15,5,11],"leaf"),([7,0,7],[9,3,9],"brass")],0,(["LLL","LSL"," R "],{"L":"spruce_leaves","S":"string","R":"red_wool"},2))
decor("tree_star","Tree Star","Верхушка-звезда","""
mmmmmmmmmmmmmmmm
mmlhhhmmmmmmmmmm
mlhhhhmmmmmmmmmm
mhhhhmmmmmmmmmmm
mmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmm
mmmmmmmmmmmmmmmm
""",{"m":"be9350","h":"dfbd72","l":"f4d994"},
 [([6,0,6],[10,3,10],"brass"),([6,3,7],[10,13,9],"surface"),([3,7,7],[13,10,9],"surface"),([5,5,7],[11,11,9],"surface"),([7,13,7],[9,15,9],"surface")],8,([" N ","NGN"," N "],{"N":"gold_nugget","G":"glowstone_dust"},1))
# Keep Java registrations in a dedicated module; all content can be crafted.
p=J/"MidnightEve.java"
edit(p,"    private static final DeferredRegister.Blocks BLOCKS","    public static final DeferredRegister.Blocks BLOCKS")
edit(p,"    private static final DeferredRegister.Items ITEMS","    public static final DeferredRegister.Items ITEMS")
edit(p,"        BLOCKS.register(bus); ITEMS.register(bus); bus.addListener(this::creative);","        dev.mngklrd.midnighteve.content.TalesContent.init(bus);\n        BLOCKS.register(bus); ITEMS.register(bus); bus.addListener(this::creative);")
text(J/"content/TalesContent.java",'''package dev.mngklrd.midnighteve.content;
import dev.mngklrd.midnighteve.MidnightEve;
import dev.mngklrd.midnighteve.item.WinterJournal;
import dev.mngklrd.midnighteve.quest.WinterQuests;
import dev.mngklrd.midnighteve.world.WinterLandformFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;
import java.util.function.Supplier;

public final class TalesContent {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,"midnighteve");
    public static final Supplier<Feature<?>> LANDFORM=FEATURES.register("winter_landform",()->new WinterLandformFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredItem<Item> GINGERBREAD=MidnightEve.ITEMS.registerSimpleItem("gingerbread",new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.5F).build()));
    public static final DeferredItem<Item> COCOA=MidnightEve.ITEMS.registerSimpleItem("hot_cocoa",new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.65F).alwaysEdible().build()));
    public static final DeferredItem<WinterJournal> JOURNAL=MidnightEve.ITEMS.register("winter_journal",()->new WinterJournal(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PARCEL=MidnightEve.ITEMS.registerSimpleItem("sealed_parcel",new Item.Properties().stacksTo(16));
    public static final DeferredBlock<DecorBlock> ORNAMENT=decor("ornament",0,5,0,5,11,11,11);
    public static final DeferredBlock<DecorBlock> CANDLES=decor("candle_cluster",10,3,0,3,13,13,13);
    public static final DeferredBlock<DecorBlock> GARLAND=decor("garland",0,0,0,5,16,7,11);
    public static final DeferredBlock<DecorBlock> STAR=decor("tree_star",8,3,0,6,13,15,10);
    public static final DeferredItem<BlockItem> ORNAMENT_ITEM=MidnightEve.ITEMS.registerSimpleBlockItem(ORNAMENT);
    public static final DeferredItem<BlockItem> CANDLES_ITEM=MidnightEve.ITEMS.registerSimpleBlockItem(CANDLES);
    public static final DeferredItem<BlockItem> GARLAND_ITEM=MidnightEve.ITEMS.registerSimpleBlockItem(GARLAND);
    public static final DeferredItem<BlockItem> STAR_ITEM=MidnightEve.ITEMS.registerSimpleBlockItem(STAR);
    private static DeferredBlock<DecorBlock> decor(String n,int light,int a,int b,int c,int x,int y,int z){
        return MidnightEve.BLOCKS.register(n,()->new DecorBlock(BlockBehaviour.Properties.of().strength(0.3F).noOcclusion().noCollission().sound(SoundType.WOOD).lightLevel(s->light),a,b,c,x,y,z));
    }
    public static void init(IEventBus bus){
        FEATURES.register(bus);
        bus.addListener(TalesContent::creative);
        NeoForge.EVENT_BUS.addListener(WinterQuests::interact);
        NeoForge.EVENT_BUS.addListener(WinterQuests::interactSpecific);
        NeoForge.EVENT_BUS.addListener(WinterQuests::clonePlayer);
    }
    private static void creative(BuildCreativeModeTabContentsEvent e){
        if(e.getTabKey().equals(CreativeModeTabs.FOOD_AND_DRINKS)){e.accept(GINGERBREAD.get());e.accept(COCOA.get());}
        if(e.getTabKey().equals(CreativeModeTabs.INGREDIENTS)){e.accept(JOURNAL.get());e.accept(PARCEL.get());}
        if(e.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)){e.accept(ORNAMENT_ITEM.get());e.accept(CANDLES_ITEM.get());e.accept(GARLAND_ITEM.get());e.accept(STAR_ITEM.get());}
    }
}
''')
text(J/"content/DecorBlock.java",'''package dev.mngklrd.midnighteve.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
public final class DecorBlock extends Block {
    private final VoxelShape shape;
    public DecorBlock(Properties p,int a,int b,int c,int x,int y,int z){super(p);shape=box(a,b,c,x,y,z);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return shape;}
}
''')
# Three authored requests per profession. Stable data keys and item IDs, no display-name dependency for new residents.
# Legacy 0.6 NPCs are recognised only when tagged by this mod, then matched by their original authored name.
quests={
 "baker":[("wheat",16,2),("midnighteve:gingerbread",8,3),("midnighteve:hot_cocoa",3,4)],
 "giftkeeper":[("paper",16,2),("midnighteve:wrapped_gift",2,3),("midnighteve:sealed_parcel",2,4)],
 "woodworker":[("spruce_log",20,2),("midnighteve:garland",4,3),("midnighteve:tree_star",2,4)],
 "ranger":[("coal",12,2),("leather",6,3),("midnighteve:hot_cocoa",3,4)],
 "postmaster":[("paper",24,2),("midnighteve:sealed_parcel",2,3),("midnighteve:sealed_parcel",4,5)],
 "chandler":[("honeycomb",8,2),("midnighteve:candle_cluster",2,3),("midnighteve:ornament",4,4)],
 "fisher":[("cod",12,2),("salmon",10,3),("cooked_cod",16,4)]}
role_ru={"baker":"Пекарь","giftkeeper":"Хранитель подарков","woodworker":"Плотник","ranger":"Егерь","postmaster":"Почтальон","chandler":"Свечник","fisher":"Рыбак"}
# Tag future and existing templates directly by adding role tags at NBT creation time
# for newly emitted templates; fallback handles base templates.
rows=[]
for role,items in quests.items():
    rows.append('        Map.entry("'+role+'",List.of('+','.join('new Request("'+mc(id)+'",'+str(n)+','+str(reward)+')' for id,n,reward in items)+'))')
    g.en["quest.midnighteve.role."+role]=role.title()
    g.ru["quest.midnighteve.role."+role]=role_ru[role]
table=",\n".join(rows)
text(J/"quest/WinterQuests.java",'''package dev.mngklrd.midnighteve.quest;
import dev.mngklrd.midnighteve.MidnightEve;
import dev.mngklrd.midnighteve.content.TalesContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.*;

public final class WinterQuests {
    private static final String KEY="midnighteve_quests_v1";
    public record Request(String item,int count,int reward) {
        Item required(){return BuiltInRegistries.ITEM.get(ResourceLocation.parse(item));}
    }
    private static final Map<String,List<Request>> REQUESTS=Map.ofEntries(
'''+table+'''
    );
    private static final List<String> ROLES=List.of("baker","giftkeeper","woodworker","ranger","postmaster","chandler","fisher");
    private static CompoundTag data(Player p){
        CompoundTag root=p.getPersistentData();
        if(!root.contains(KEY))root.put(KEY,new CompoundTag());
        return root.getCompound(KEY);
    }
    public static void clonePlayer(PlayerEvent.Clone e){
        if(e.getOriginal().getPersistentData().contains(KEY))
            e.getEntity().getPersistentData().put(KEY,e.getOriginal().getPersistentData().getCompound(KEY).copy());
    }
    private static String role(Entity entity){
        if(!(entity instanceof Villager v)||!v.getTags().contains("midnighteve_resident"))return "";
        for(String role:ROLES)if(v.getTags().contains("midnighteve_role_"+role))return role;
        if(v.hasCustomName()){
            String n=v.getCustomName().getString().toLowerCase(Locale.ROOT);
            if(REQUESTS.containsKey(n))return n;
        }
        return "";
    }
    public static void interact(PlayerInteractEvent.EntityInteract e){
        if(handle(e.getEntity(),e.getTarget(),e.getHand())){
            e.setCancellationResult(InteractionResult.sidedSuccess(e.getEntity().level().isClientSide));e.setCanceled(true);
        }
    }
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific e){
        if(handle(e.getEntity(),e.getTarget(),e.getHand())){
            e.setCancellationResult(InteractionResult.sidedSuccess(e.getEntity().level().isClientSide));e.setCanceled(true);
        }
    }
    private static boolean handle(Player player,Entity entity,InteractionHand hand){
        String role=role(entity);
        if(role.isEmpty()||hand!=InteractionHand.MAIN_HAND||!player.isShiftKeyDown())return false;
        if(!(player instanceof ServerPlayer p))return true;
        CompoundTag state=data(p);int n=Math.max(0,Math.min(3,state.getInt(role)));
        if(n==3){p.displayClientMessage(Component.translatable("quest.midnighteve.done",Component.translatable("quest.midnighteve.role."+role)),false);return true;}
        Request q=REQUESTS.get(role).get(n); Item required=q.required();
        ItemStack stack=p.getMainHandItem();
        if(required==Items.AIR||!stack.is(required)||stack.getCount()<q.count()){
            hint(p,role,n,q);return true;
        }
        if(!p.getAbilities().instabuild)stack.shrink(q.count());
        // Set persisted progress before awarding items; the second event cannot replay the same stage.
        state.putInt(role,n+1);
        state.putInt("reputation",state.getInt("reputation")+q.reward());
        give(p,new ItemStack(MidnightEve.WINTERMARK.get(),q.reward()));
        if(n==2)give(p,new ItemStack(MidnightEve.WRAPPED_GIFT.get(),2));
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.BELL_BLOCK,SoundSource.PLAYERS,0.5F,1.35F);
        p.displayClientMessage(Component.translatable("quest.midnighteve.complete",Component.translatable("quest.midnighteve.role."+role),n+1,q.reward()),false);
        if(total(state)==21&&!state.getBoolean("champion")){
            state.putBoolean("champion",true);
            give(p,new ItemStack(TalesContent.STAR_ITEM.get(),3));give(p,new ItemStack(MidnightEve.WRAPPED_GIFT.get(),6));
            p.displayClientMessage(Component.translatable("quest.midnighteve.champion"),false);
        }else if(n<2)hint(p,role,n+1,REQUESTS.get(role).get(n+1));
        return true;
    }
    private static void hint(ServerPlayer p,String role,int n,Request q){
        p.displayClientMessage(Component.translatable("quest.midnighteve.offer",Component.translatable("quest.midnighteve.role."+role),n+1,q.count(),new ItemStack(q.required()).getHoverName(),q.reward()),false);
    }
    private static void give(Player p,ItemStack stack){if(!p.addItem(stack))p.drop(stack,false);}
    private static int total(CompoundTag state){int n=0;for(String role:ROLES)n+=Math.max(0,Math.min(3,state.getInt(role)));return n;}
    public static void journal(ServerPlayer p){
        CompoundTag state=data(p);
        p.sendSystemMessage(Component.translatable("quest.midnighteve.journal",total(state),21,state.getInt("reputation")));
        for(String role:ROLES){
            int n=Math.max(0,Math.min(3,state.getInt(role)));
            if(n==3)p.sendSystemMessage(Component.translatable("quest.midnighteve.done",Component.translatable("quest.midnighteve.role."+role)));
            else{Request q=REQUESTS.get(role).get(n);p.sendSystemMessage(Component.translatable("quest.midnighteve.offer",Component.translatable("quest.midnighteve.role."+role),n+1,q.count(),new ItemStack(q.required()).getHoverName(),q.reward()));}
        }
        p.sendSystemMessage(Component.translatable("quest.midnighteve.instructions"));
    }
}
''')
text(J/"item/WinterJournal.java",'''package dev.mngklrd.midnighteve.item;
import dev.mngklrd.midnighteve.quest.WinterQuests;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class WinterJournal extends Item {
    public WinterJournal(Properties p){super(p);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer p)WinterQuests.journal(p);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
''')
g.en.update({"quest.midnighteve.offer":"%s — request %s/3: deliver %s × %s for %s wintermarks.","quest.midnighteve.done":"%s — all requests completed.","quest.midnighteve.complete":"%s: request %s/3 completed! +%s wintermarks.","quest.midnighteve.champion":"Guardian of Winter: you helped every resident profession!","quest.midnighteve.journal":"Winter Journal — %s/%s requests · reputation %s","quest.midnighteve.instructions":"Sneak + right-click a resident with an empty hand to ask; hold the required items to deliver. Normal right-click opens trade."})
g.ru.update({"quest.midnighteve.offer":"%s — поручение %s/3: принеси %s × %s. Награда: %s зимних марок.","quest.midnighteve.done":"%s — все поручения выполнены.","quest.midnighteve.complete":"%s: поручение %s/3 выполнено! +%s зимних марок.","quest.midnighteve.champion":"Хранитель зимы: ты помог всем профессиям жителей!","quest.midnighteve.journal":"Зимний журнал — %s/%s поручений · репутация %s","quest.midnighteve.instructions":"Shift + ПКМ по жителю пустой рукой показывает поручение. Возьми нужные предметы и повтори. Обычный ПКМ открывает торговлю."})

# Native local landforms: terrain-safe bounded decoration inside the generating chunk.
# No chunk scanning, no player-tick world writes, no overwriting structures.
text(J/"world/WinterLandformFeature.java",'''package dev.mngklrd.midnighteve.world;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class WinterLandformFeature extends Feature<NoneFeatureConfiguration>{
    public WinterLandformFeature(Codec<NoneFeatureConfiguration> c){super(c);}
    private static boolean ground(BlockState b){return b.is(Blocks.GRASS_BLOCK)||b.is(Blocks.DIRT)||b.is(Blocks.PODZOL)||b.is(Blocks.COARSE_DIRT)||b.is(Blocks.STONE)||b.is(Blocks.SNOW_BLOCK)||b.is(Blocks.PACKED_ICE)||b.is(Blocks.ICE)||b.is(Blocks.GRAVEL);}
    private static boolean clear(BlockState b){return b.isAir()||b.is(Blocks.SNOW);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        WorldGenLevel l=c.level();RandomSource r=c.random();BlockPos o=c.origin();
        int cx=(o.getX()&~15)+5+r.nextInt(6),cz=(o.getZ()&~15)+5+r.nextInt(6);
        int cy=l.getHeight(Heightmap.Types.WORLD_SURFACE_WG,cx,cz);
        BlockPos center=new BlockPos(cx,cy,cz);
        String biome=l.getBiome(center).unwrapKey().map(k->k.location().getPath()).orElse("");
        if(!l.getBiome(center).unwrapKey().map(k->k.location().getNamespace().equals("midnighteve")).orElse(false))return false;
        int rad=biome.equals("crystal_crown")?3:2;
        int changed=0;
        for(int x=-rad;x<=rad;x++)for(int z=-rad;z<=rad;z++){
            if(x*x+z*z>rad*rad+1)continue;
            int y=l.getHeight(Heightmap.Types.WORLD_SURFACE_WG,cx+x,cz+z);
            if(Math.abs(y-cy)>3||y<64||y>l.getMaxBuildHeight()-9)continue;
            BlockPos p=new BlockPos(cx+x,y,cz+z);
            if(!ground(l.getBlockState(p.below()))||!clear(l.getBlockState(p))||!clear(l.getBlockState(p.above())))continue;
            int dist=Math.abs(x)+Math.abs(z);
            int h=0; BlockState material=Blocks.SNOW_BLOCK.defaultBlockState();
            switch(biome){
                case "crystal_crown" -> {h=Math.max(0,5-dist+r.nextInt(2));material=dist<2?Blocks.PACKED_ICE.defaultBlockState():Blocks.CALCITE.defaultBlockState();}
                case "mirror_fjords" -> {h=Math.max(0,3-dist);material=Blocks.PACKED_ICE.defaultBlockState();}
                case "silent_snowfields" -> {h=Math.max(0,3-dist+r.nextInt(2));}
                case "starlit_steppe" -> {h=dist==0?2:dist<3?1:0;material=Blocks.CALCITE.defaultBlockState();}
                case "firlight_woods" -> {if(dist>2)continue;setBlock(l,p.below(),Blocks.PODZOL.defaultBlockState());changed++;continue;}
                case "peppermint_grove" -> {if(dist>2)continue;h=dist==0?2:1;material=dist==0?Blocks.CALCITE.defaultBlockState():Blocks.SNOW_BLOCK.defaultBlockState();}
                default -> {continue;}
            }
            for(int dy=0;dy<h;dy++){
                BlockPos q=p.above(dy);
                if(!clear(l.getBlockState(q)))break;
                setBlock(l,q,material);changed++;
            }
        }
        return changed>0;
    }
}
''')
write(D/"worldgen/configured_feature/winter_landform.json",{"type":mod("winter_landform"),"config":{}})
write(D/"worldgen/placed_feature/winter_landform.json",{"feature":mod("winter_landform"),"placement":[{"type":"minecraft:rarity_filter","chance":5},{"type":"minecraft:in_square"},{"type":"minecraft:heightmap","heightmap":"WORLD_SURFACE_WG"},{"type":"minecraft:biome"}]})
for n in g.names:
    add_json(D/"worldgen/biome"/(n+".json"),lambda b:b["features"][9].append(mod("winter_landform")))
# Add vanilla music events known to exist, no client-only calls or unlicensed assets.
# Avoid changing the base world preset and generation registries on old saves.
# Rebalance gift drops to avoid cheap diamond conversion loop via craftable gift.
p=J/"item/WrappedGift.java"
edit(p,"roll==10?new ItemStack(Items.GOLD_INGOT,2):new ItemStack(Items.DIAMOND,1);","roll==10?new ItemStack(Items.GOLD_NUGGET,5):new ItemStack(TalesContent.ORNAMENT_ITEM.get(),2);")
edit(p,"import net.minecraft.core.particles.ParticleTypes;","import net.minecraft.core.particles.ParticleTypes;\nimport dev.mngklrd.midnighteve.content.TalesContent;")
# Actual progression unlocks a whole set of craftable holiday goods without making old festivals incompatible.
p=J/"block/FestivalHearth.java"
edit(p,"Block.popResource(s,p.above(),new ItemStack(MidnightEve.WRAPPED_GIFT.get(),3));","Block.popResource(s,p.above(),new ItemStack(MidnightEve.WRAPPED_GIFT.get(),3));\n        Block.popResource(s,p.above(),new ItemStack(dev.mngklrd.midnighteve.content.TalesContent.GINGERBREAD.get(),8));\n        Block.popResource(s,p.above(),new ItemStack(dev.mngklrd.midnighteve.content.TalesContent.STAR_ITEM.get(),1));")
# Version and translation resources written only after all extensions are assembled.
edit(R/"META-INF/neoforge.mods.toml",'version="0.6.0"','version="0.7.0"')
for n,lang in [("en_us",g.en),("ru_ru",g.ru)]:write(A/"lang"/(n+".json"),lang)
# Every referenced native MIDNIGHT structure is backed by a real template.
for p in (D/"worldgen/structure").glob("*.json"):
    assert (D/"structure"/(p.stem+".nbt")).is_file(),p
for p in R.rglob("*.json"):json.loads(p.read_text(encoding="utf8"))
print("PASS: 0.7 Winter Tales generated",len(list((D/"worldgen/structure").glob("*.json"))),"structure types,",sum(map(len,quests.values())),"quests, 8 craftable items/decorations")
