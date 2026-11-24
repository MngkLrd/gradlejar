#!/usr/bin/env python3
"""Evergreen: native renewable plants, brewing, a usable bell and licensed audio."""
import expand_winterfolk as w
import json
f,g,t=w.f,w.g,w.t
S,R,D,A,J=w.S,w.R,w.D,w.A,w.J
write,text,mc,mod,edit=w.write,w.text,w.mc,w.mod,w.edit

text(J/"evergreen/EvergreenContent.java",'''package dev.mngklrd.midnighteve.evergreen;
import dev.mngklrd.midnighteve.MidnightEve;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;
import java.util.function.Supplier;
public final class EvergreenContent {
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,"midnighteve");
    public static final Supplier<SoundEvent> BELL=SOUNDS.register("celebration.bell",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("midnighteve","celebration.bell")));
    public static final Supplier<SoundEvent> CHIME=SOUNDS.register("celebration.chime",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("midnighteve","celebration.chime")));
    public static final DeferredBlock<WinterBush> FROSTBERRY=MidnightEve.BLOCKS.register("frostberry_bush",()->new WinterBush(BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().sound(SoundType.SWEET_BERRY_BUSH)));
    public static final DeferredBlock<WinterBush> HOLLY=MidnightEve.BLOCKS.register("holly_bush",()->new WinterBush(BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().sound(SoundType.SWEET_BERRY_BUSH)));
    public static final DeferredItem<Item> BERRIES=MidnightEve.ITEMS.register("frostberries",()->new ItemNameBlockItem(FROSTBERRY.get(),new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(.3F).build())));
    public static final DeferredItem<Item> SPRIG=MidnightEve.ITEMS.register("holly_sprig",()->new ItemNameBlockItem(HOLLY.get(),new Item.Properties()));
    public static final DeferredItem<Item> TEA=MidnightEve.ITEMS.register("frostberry_tea",()->new WinterTea(new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(3).saturationModifier(.4F).alwaysEdible().usingConvertsTo(Items.GLASS_BOTTLE).build())));
    public static final DeferredBlock<CelebrationBell> FESTIVAL_BELL=MidnightEve.BLOCKS.register("celebration_bell",()->new CelebrationBell(BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredItem<BlockItem> BELL_ITEM=MidnightEve.ITEMS.registerSimpleBlockItem(FESTIVAL_BELL);
    public static void init(IEventBus bus){SOUNDS.register(bus);bus.addListener(EvergreenContent::creative);}
    private static void creative(BuildCreativeModeTabContentsEvent e){
        if(e.getTabKey().equals(CreativeModeTabs.FOOD_AND_DRINKS)){e.accept(BERRIES.get());e.accept(TEA.get());}
        if(e.getTabKey().equals(CreativeModeTabs.INGREDIENTS))e.accept(SPRIG.get());
        if(e.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS))e.accept(BELL_ITEM.get());
    }
}
''')
text(J/"evergreen/WinterBush.java",'''package dev.mngklrd.midnighteve.evergreen;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public final class WinterBush extends BushBlock implements BonemealableBlock {
    public static final MapCodec<WinterBush> CODEC=simpleCodec(WinterBush::new);
    public static final IntegerProperty AGE=BlockStateProperties.AGE_3;
    public WinterBush(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(AGE,0));}
    @Override public MapCodec<WinterBush> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(AGE);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return switch(s.getValue(AGE)){case 0->box(5,0,5,11,7,11);case 1->box(3,0,3,13,11,13);default->box(2,0,2,14,15,14);};
    }
    @Override protected boolean isRandomlyTicking(BlockState s){return s.getValue(AGE)<3;}
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
        int age=s.getValue(AGE);
        if(age<3&&l.getRawBrightness(p.above(),0)>=9&&r.nextInt(5)==0)l.setBlock(p,s.setValue(AGE,age+1),2);
    }
    @Override public boolean isValidBonemealTarget(LevelReader l,BlockPos p,BlockState s){return s.getValue(AGE)<3;}
    @Override public boolean isBonemealSuccess(Level l,RandomSource r,BlockPos p,BlockState s){return true;}
    @Override public void performBonemeal(ServerLevel l,RandomSource r,BlockPos p,BlockState s){l.setBlock(p,s.setValue(AGE,Math.min(3,s.getValue(AGE)+1)),2);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(s.getValue(AGE)!=3)return InteractionResult.PASS;
        if(l instanceof ServerLevel server){
            Item item=s.is(EvergreenContent.HOLLY.get())?EvergreenContent.SPRIG.get():EvergreenContent.BERRIES.get();
            Block.popResource(server,p,new ItemStack(item,2+server.random.nextInt(2)));
            server.setBlock(p,s.setValue(AGE,1),2);
            server.playSound(null,p,SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,SoundSource.BLOCKS,.8F,1F);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
''')
text(J/"evergreen/WinterTea.java",'''package dev.mngklrd.midnighteve.evergreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class WinterTea extends Item {
    public WinterTea(Properties p){super(p);}
    @Override public UseAnim getUseAnimation(ItemStack s){return UseAnim.DRINK;}
    @Override public ItemStack finishUsingItem(ItemStack s,Level l,LivingEntity living){
        ItemStack result=super.finishUsingItem(s,l,living);
        if(!l.isClientSide)living.setTicksFrozen(0);
        return result;
    }
}
''')
text(J/"evergreen/CelebrationBell.java",'''package dev.mngklrd.midnighteve.evergreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public final class CelebrationBell extends Block {
    public static final BooleanProperty RINGING=BooleanProperty.create("ringing");
    public CelebrationBell(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(RINGING,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(RINGING);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(2,0,3,14,16,13);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(l instanceof ServerLevel server&&!s.getValue(RINGING)){
            server.setBlock(p,s.setValue(RINGING,true),3);server.scheduleTick(p,this,40);
            server.playSound(null,p,EvergreenContent.BELL.get(),SoundSource.BLOCKS,.9F,1F);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){if(s.getValue(RINGING))l.setBlock(p,s.setValue(RINGING,false),3);}
}
''')
edit(J/"MidnightEve.java","        dev.mngklrd.midnighteve.winterfolk.FolkContent.init(bus);","        dev.mngklrd.midnighteve.winterfolk.FolkContent.init(bus);\n        dev.mngklrd.midnighteve.evergreen.EvergreenContent.init(bus);")
# Hand-authored silhouettes. Each successive growth stage adds real branches,
# leaves, and fruit clusters rather than scaling the same finished sprite.
shapes=[
["................","................","................","................","................","................","................","................",".......h........","......hgh.......",".......gg.......",".....hgsgg......","......gsg.......",".......w........",".......w........",".......d........"],
["................","................","................","................","......hh........",".....hggg.......",".......gg.......","...hh..g..hh....","..hggg.g.hggg...","...ggggggggg....","....sggggs......",".....sggs.......","......ww........",".......w........",".......w........",".......d........"],
["................",".......hh.......","......hggg......","....hhgggg......","...hggggs..hh...","....ggsgghggg...","..hh..sgggggs...","..hggggggggg....","...ggggsggs.hh..","...sggggggghggg.","....sgggggggs...",".....sggggs.....","......www.......","......dwd.......",".......w........",".......d........"],
["................",".......hh.......","......hggg......","....hhgggg......","...hggggs..hh...","....ggsgghggg...","..hh..sgggggs...","..hggggggggg....","...ggggsggs.hh..","...sggggggghggg.","....sgggggggs...",".....sggggs.....","......www.......","......dwd.......",".......w........",".......d........"]]
for name,colors,fruit in [
 ("frostberry_bush",{"h":"839b89","g":"567866","s":"3e6156","w":"8b7056","d":"665343","r":"a7c7d3","l":"d2e3e6","b":"799eae"},[(3,6),(7,4),(8,10),(10,6)]),
 ("holly_bush",{"h":"83945a","g":"526b40","s":"3c5234","w":"927054","d":"6d5340","r":"ae4e42","l":"d5765d","b":"853a38"},[(4,5),(7,9),(9,4),(10,11)])]:
    for age,rows in enumerate(shapes):
        rows=[list(r) for r in rows]
        if age==3:
            for y,x in fruit:
                for dx,dy,k in [(0,0,"l"),(1,0,"r"),(0,1,"r"),(1,1,"b")]:rows[y+dy][x+dx]=k
        texture=name+"_"+str(age)
        assert all(len(r)==16 for r in rows),(name,age)
        g.png(A/"textures/block"/(texture+".png"),["".join(r) for r in rows],{".":(0,0,0,0),**{k:g.rgba(v) for k,v in colors.items()}})
        write(A/"models/block"/(texture+".json"),{"parent":"minecraft:block/cross","render_type":"minecraft:cutout","textures":{"cross":mod("block/"+texture)}})
    write(A/"blockstates"/(name+".json"),{"variants":{"age="+str(age):{"model":mod("block/"+name+"_"+str(age))} for age in range(4)}})
    item="frostberries" if name=="frostberry_bush" else "holly_sprig"
    write(D/"loot_table/blocks"/(name+".json"),{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":mod(item)}],"conditions":[{"condition":"minecraft:survives_explosion"}]},{"rolls":1,"entries":[{"type":"minecraft:item","name":mod(item),"functions":[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":1,"max":2}}]}],"conditions":[{"condition":"minecraft:block_state_property","block":mod(name),"properties":{"age":"3"}},{"condition":"minecraft:survives_explosion"}]}]})
    # Native random patch and placement. Survival predicate keeps plants on dirt-like blocks.
    write(D/"worldgen/configured_feature"/(name+".json"),{"type":"minecraft:random_patch","config":{"tries":20,"xz_spread":4,"y_spread":2,"feature":{"feature":{"type":"minecraft:simple_block","config":{"to_place":{"type":"minecraft:simple_state_provider","state":{"Name":mod(name),"Properties":{"age":"3"}}}}},"placement":[{"type":"minecraft:block_predicate_filter","predicate":{"type":"minecraft:all_of","predicates":[{"type":"minecraft:matching_blocks","blocks":["minecraft:air","minecraft:snow"]},{"type":"minecraft:would_survive","state":{"Name":mod(name),"Properties":{"age":"3"}}}]}}]}}})
    write(D/"worldgen/placed_feature"/(name+".json"),{"feature":mod(name),"placement":[{"type":"minecraft:rarity_filter","chance":5 if name=="holly_bush" else 3},{"type":"minecraft:in_square"},{"type":"minecraft:heightmap","heightmap":"MOTION_BLOCKING_NO_LEAVES"},{"type":"minecraft:biome"}]})
    for biome in (("firlight_woods","peppermint_grove","starlit_steppe") if name=="holly_bush" else ("firlight_woods","silent_snowfields","peppermint_grove")):
        t.add_json(D/"worldgen/biome"/(biome+".json"),lambda b:b["features"][9].append(mod(name)))

f.sprite("frostberries",["....gg......","...gghg.....","....g.......","..bbg.bbb...","..blrrbllb..",".blrrbrlrb..",".brrb.brrb..","..bb...bb...","....bbb.....","...bllrb....","...brrrb....","....bbb....."],{"g":"526e5c","h":"819782","b":"779aa9","r":"a4c3d0","l":"d4e4e8"})
f.sprite("holly_sprig",[".......h....","......hg....","...h.hgg....","..hggggs....","...gggs.hh..","....ggghggg.","..hhgggggs..","..hggsrr....","...gsrlrb...","....rrbrb...","....bbwb....","......w....."],{"h":"81975b","g":"536b42","s":"3f5435","r":"b65243","l":"dc8064","b":"843a35","w":"8a6c51"})
f.sprite("frostberry_tea",["....ss......","....s.s.....","...s.s......","....ooo.....","...oggho....","...ohhho....","..ogllhgo...","..ogllhgo...","..oghhhgo...","..ogmmhgo...","..ogmmhgo...","...oggo.....","....oo......"],{"s":"bfc9c5","o":"526b72","g":"92abb0","h":"b6d1d1","l":"d8e7df","m":"779f99"})
for n in ("frostberries","holly_sprig","frostberry_tea"):
    write(A/"models/item"/(n+".json"),{"parent":"minecraft:item/generated","textures":{"layer0":mod("item/"+n)}})
write(D/"recipe/frostberry_tea.json",{"type":"minecraft:crafting_shapeless","ingredients":[{"item":mod("frostberries")},{"item":mod("frostberries")},{"item":"minecraft:sugar"},{"item":"minecraft:glass_bottle"}],"result":{"id":mod("frostberry_tea")}})
# Alternative garland recipe, renewable holly plus spruce.
write(D/"recipe/garland_holly.json",{"type":"minecraft:crafting_shaped","pattern":["HHH","SWS"],"key":{"H":{"item":mod("holly_sprig")},"S":{"item":"minecraft:spruce_leaves"},"W":{"item":"minecraft:string"}},"result":{"id":mod("garland"),"count":2}})
# Bell: native-sized wood frame, tapered bronze body, clapper. 1:1 face UVs.
bronze=["mmmmmmmmmmmmmmmm","mhhhhmmmmmmmmmmm","mhhhhmmmmmmmmmmm","mhhllhmmmmmmmmmm","mmhhhhmmmmmmmmmm","mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm","mmmmmmmmmmmmmmmm","ssssssssssssssss","smmmmmmmmmmmmmm s".replace(" ",""),"ssssssssssssssss","ssssssssssssssss"]
g.png(A/"textures/block/celebration_bell.png",bronze,{k:g.rgba(v) for k,v in {"m":"b48b4b","h":"d2ae67","l":"e8ca88","s":"8b6539"}.items()})
frame=[f.elem([2,0,4],[4,16,6],"wood"),f.elem([12,0,4],[14,16,6],"wood"),f.elem([2,14,4],[14,16,12],"wood"),f.elem([1,0,3],[15,2,13],"wood")]
def bell_geometry(tilt):
    rot={"origin":[8,13,8],"axis":"x","angle":tilt,"rescale":False} if tilt else None
    return [f.elem([7,12,7],[9,14,9],"metal",rotation=rot),
            f.elem([5,7,5],[11,12,11],"metal",{"north":(0,0),"south":(8,7),"east":(3,2),"west":(6,5),"up":(1,0),"down":(8,6)},rotation=rot),
            f.elem([4,5,4],[12,7,12],"metal",rotation=rot),
            f.elem([3,4,3],[13,5,13],"rim",{"north":(0,12),"south":(0,12),"east":(0,12),"west":(0,12),"up":(0,3),"down":(0,6)},rotation=rot),
            f.elem([7,2,7],[9,5,9],"dark",rotation=rot)]
for ready in (False,True):
    n="celebration_bell_"+("ringing" if ready else "idle")
    write(A/"models/block"/(n+".json"),{"parent":"minecraft:block/block","textures":{"particle":mod("block/celebration_bell"),"wood":"minecraft:block/spruce_planks","metal":mod("block/celebration_bell"),"rim":mod("block/celebration_bell"),"dark":"minecraft:block/exposed_copper"},"elements":frame+bell_geometry(22.5 if ready else 0)})
write(A/"blockstates/celebration_bell.json",{"variants":{"ringing=false":{"model":mod("block/celebration_bell_idle")},"ringing=true":{"model":mod("block/celebration_bell_ringing")}}})
write(A/"models/item/celebration_bell.json",{"parent":mod("block/celebration_bell_idle")})
write(D/"loot_table/blocks/celebration_bell.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":mod("celebration_bell")}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
t.recipe("celebration_bell",["SCS"," C "," G "],{"S":"spruce_planks","C":"copper_ingot","G":"gold_ingot"})
# Third party sounds are downloaded only by the authoring CI, not at runtime.
# The packaged notice preserves exact provenance and license.
p=A/"sounds.json";sounds=json.loads(p.read_text()) if p.exists() else {}
sounds.update({"celebration.bell":{"subtitle":"subtitles.midnighteve.celebration.bell","sounds":[{"name":mod("celebration/bell"),"volume":.8}]},"celebration.chime":{"subtitle":"subtitles.midnighteve.celebration.chime","sounds":[{"name":mod("celebration/chime"),"volume":.7}]}})
write(p,sounds)
text(R/"CREDITS.md","""# MIDNIGHT audio credits

Original MIDNIGHT compositions: lanterns_at_dusk and snow_over_the_fjords, retained unchanged.

Kenney, Impact Sounds, impactBell heavy 001 (celebration/bell.ogg).
https://kenney.nl/assets/impact-sounds

Kenney, Music Jingles, jingles PIZZI07 (celebration/chime.ogg).
https://kenney.nl/assets/music-jingles

License: Creative Commons CC0 1.0 Universal.
https://creativecommons.org/publicdomain/zero/1.0/

Unmodified Ogg Vorbis files mirrored by Mcamento8/open-game-sfx-index.
https://github.com/Mcamento8/open-game-sfx-index
SHA-256 verified at build. No endorsement implied.
""")
# Celebration feedback has distinct sounds instead of reusing the same vanilla bell everywhere.
edit(J/"winterfolk/DailyMarket.java","SoundEvents.BELL_BLOCK,SoundSource.PLAYERS,.5F,1.25F","dev.mngklrd.midnighteve.evergreen.EvergreenContent.CHIME.get(),SoundSource.PLAYERS,.65F,1F")
edit(J/"block/FestivalHearth.java","SoundEvents.BELL_BLOCK,SoundSource.BLOCKS,2.0F,0.8F","dev.mngklrd.midnighteve.evergreen.EvergreenContent.BELL.get(),SoundSource.BLOCKS,1.2F,0.9F")
# Native structure decorations: bells in pre-authored clear exterior spaces.
# Add them only to the four existing festival locations; retain all doors and floor blocks.
import gzip
added=0
for file in (D/"structure").glob("*.nbt"):
    if file.stem not in ("frostmarket","everlight_hamlet","lantern_fair","candle_hall"):continue
    root=f.read_nbt(gzip.decompress(file.read_bytes()));palette=root["palette"][1][1];blocks=root["blocks"][1][1]
    idx={tuple(b["pos"][1][1]):b for b in blocks}
    def name(p):
        b=idx.get(p);return palette[b["state"][1]]["Name"][1] if b else ""
    entry={"Name":g.Str(mod("celebration_bell")),"Properties":g.C({"ringing":g.Str("false")})}
    if entry not in palette:palette.append(entry)
    for (x,y,z),b in list(idx.items()):
        if name((x,y,z))!=mod("festival_hearth"):continue
        for dx,dz in ((-4,0),(4,0),(0,-4),(0,4)):
            p=(x+dx,y,z+dz)
            if name(p)=="minecraft:air" and name((p[0],y+1,p[2]))=="minecraft:air" and name((p[0],y-1,p[2])) in ("minecraft:stone_bricks","minecraft:cobblestone","minecraft:spruce_planks","minecraft:dark_oak_planks","minecraft:grass_block","minecraft:dirt_path","minecraft:polished_andesite"):
                idx[p]["state"]=g.I(palette.index(entry));added+=1;break
    root["blocks"]=g.L(10,blocks)
    file.write_bytes(gzip.compress(b"\x0a\x00\x00"+g.payload(g.C(root)),mtime=0))
print("PASS: inserted celebration bells into clear festival locations:",added)
# Keep all original story orders, offer renewable produce in daily market.
edit(J/"winterfolk/DailyMarket.java",'new Order("midnighteve:grouse_egg",4,3))','new Order("midnighteve:grouse_egg",4,3),new Order("midnighteve:frostberries",12,2))')
edit(J/"winterfolk/DailyMarket.java",'new Order("midnighteve:ornament",4,3))','new Order("midnighteve:ornament",4,3),new Order("midnighteve:holly_sprig",8,2))')
g.en.update({"block.midnighteve.frostberry_bush":"Frostberry Bush","block.midnighteve.holly_bush":"Holly Bush","item.midnighteve.frostberries":"Frostberries","item.midnighteve.holly_sprig":"Holly Sprig","item.midnighteve.frostberry_tea":"Frostberry Tea","block.midnighteve.celebration_bell":"Celebration Bell","subtitles.midnighteve.celebration.bell":"Celebration bell rings","subtitles.midnighteve.celebration.chime":"Market chime"})
g.ru.update({"block.midnighteve.frostberry_bush":"Куст морозной ягоды","block.midnighteve.holly_bush":"Куст остролиста","item.midnighteve.frostberries":"Морозные ягоды","item.midnighteve.holly_sprig":"Веточка остролиста","item.midnighteve.frostberry_tea":"Чай из морозной ягоды","block.midnighteve.celebration_bell":"Праздничный колокол","subtitles.midnighteve.celebration.bell":"Звон праздничного колокола","subtitles.midnighteve.celebration.chime":"Рыночный звон"})
for name,lang in [("en_us",g.en),("ru_ru",g.ru)]:write(A/"lang"/(name+".json"),lang)
edit(R/"META-INF/neoforge.mods.toml",'version="0.10.0"','version="0.11.0"')
for file in R.rglob("*.json"):json.loads(file.read_text(encoding="utf8"))
print("PASS: Evergreen — two renewable plants, eight growth textures, three item sprites, interactive bell, licensed audio registry and renewable market orders")
