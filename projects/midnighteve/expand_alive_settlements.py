#!/usr/bin/env python3
"""Alive Settlements: terrain-fit towns, festive geometry, snow, music and ambient life."""
import expand_market_ui as m
import json,gzip
v,e,w,f,g,t=m.v,m.e,m.w,m.f,m.g,m.t
S,R,D,A,J=m.S,m.R,m.D,m.A,m.J
write,text,mod,edit=m.write,m.text,m.mod,m.edit

# Use the vanilla Jigsaw implementation to select the actual template and rotation,
# then sample the chosen footprint, never generate chunks to search for a location.
text(J/"alive/FlatSettlement.java",'''package dev.mngklrd.midnighteve.alive;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import java.util.*;
public final class FlatSettlement extends Structure {
    public static final MapCodec<FlatSettlement> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        settingsCodec(i),StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s->s.pool),
        Codec.intRange(4,30).fieldOf("max_relief").forGetter(s->s.maxRelief)).apply(i,FlatSettlement::new));
    private final Holder<StructureTemplatePool> pool;
    private final int maxRelief;
    private final JigsawStructure delegate;
    public FlatSettlement(StructureSettings s,Holder<StructureTemplatePool> pool,int maxRelief){
        super(s);this.pool=pool;this.maxRelief=maxRelief;
        delegate=new JigsawStructure(s,pool,1,ConstantHeight.of(VerticalAnchor.absolute(-1)),false,Heightmap.Types.WORLD_SURFACE_WG);
    }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext ctx){
        var result=delegate.findGenerationPoint(ctx);
        if(result.isEmpty())return Optional.empty();
        var stub=result.get();var builder=stub.getPiecesBuilder();
        if(builder.isEmpty())return Optional.empty();
        var box=builder.getBoundingBox();int[] heights=new int[9];int n=0;
        int[] xs={box.minX()+2,(box.minX()+box.maxX())/2,box.maxX()-2};
        int[] zs={box.minZ()+2,(box.minZ()+box.maxZ())/2,box.maxZ()-2};
        for(int x:xs)for(int z:zs){
            int y=ctx.chunkGenerator().getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,ctx.heightAccessor(),ctx.randomState());
            int ground=ctx.chunkGenerator().getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,ctx.heightAccessor(),ctx.randomState());
            if(y-ground>2)return Optional.empty();
            heights[n++]=ground;
        }
        Arrays.sort(heights);
        if(heights[8]-heights[0]>maxRelief)return Optional.empty();
        int delta=heights[4]-2-box.minY();
        if(delta!=0)builder.offsetPiecesVertically(delta);
        return Optional.of(new GenerationStub(stub.position().offset(0,delta,0),Either.right(builder)));
    }
    @Override public StructureType<?> type(){return AliveContent.FLAT.get();}
}
''')
text(J/"alive/AliveContent.java",'''package dev.mngklrd.midnighteve.alive;
import dev.mngklrd.midnighteve.MidnightEve;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;
import java.util.function.Supplier;
public final class AliveContent {
    private static final DeferredRegister<StructureType<?>> TYPES=DeferredRegister.create(Registries.STRUCTURE_TYPE,"midnighteve");
    public static final Supplier<StructureType<FlatSettlement>> FLAT=TYPES.register("flat_settlement",()->()->FlatSettlement.CODEC);
    public static final DeferredBlock<Block> FROZEN_SOIL=MidnightEve.BLOCKS.registerSimpleBlock("frozen_soil",BlockBehaviour.Properties.of().strength(.7F).sound(SoundType.GRAVEL));
    public static final DeferredItem<BlockItem> SOIL_ITEM=MidnightEve.ITEMS.registerSimpleBlockItem(FROZEN_SOIL);
    public static void init(IEventBus bus){TYPES.register(bus);NeoForge.EVENT_BUS.addListener(VillageLife::tick);}
}
''')
# Village life piggybacks on vanilla brain and meeting POIs rather than replacing
# its whole task list. Infrequent queries, only tagged adults in the server world.
text(J/"alive/VillageLife.java",'''package dev.mngklrd.midnighteve.alive;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.entity.Mob;
public final class VillageLife {
    public static void tick(EntityTickEvent.Post e){
        if(!(e.getEntity().level() instanceof ServerLevel level))return;
        if(e.getEntity() instanceof Villager v){
            if(!v.getTags().contains("midnighteve_resident")||v.isBaby()||v.isSleeping()||v.tickCount%100!=Math.floorMod(v.getId(),100)||v.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY))return;
            long time=Math.floorMod(level.getDayTime(),24000L);
            if(time>=10500&&time<12000&&!v.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)){
                v.getBrain().getMemory(MemoryModuleType.MEETING_POINT).filter(p->p.dimension().equals(level.dimension())&&p.pos().distSqr(v.blockPosition())<2304).ifPresent(p->v.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(p.pos(),.45F,3)));
            }
            if(time>=1000&&time<12000&&v.getRandom().nextInt(3)==0){
                var neighbors=level.getEntitiesOfClass(Villager.class,v.getBoundingBox().inflate(4),a->a!=v&&a.getTags().contains("midnighteve_resident"));
                if(!neighbors.isEmpty()&&v.getNavigation().isDone()){
                    var friend=neighbors.get(0);v.getLookControl().setLookAt(friend,25F,25F);
                    if(v.getRandom().nextInt(5)==0)level.sendParticles(ParticleTypes.HAPPY_VILLAGER,v.getX(),v.getY()+1.8,v.getZ(),1,.15,.1,.15,0);
                }
            }
        }
    }
}
''')
edit(J/"MidnightEve.java","        dev.mngklrd.midnighteve.market.MarketContent.init(bus);","        dev.mngklrd.midnighteve.market.MarketContent.init(bus);\n        dev.mngklrd.midnighteve.alive.AliveContent.init(bus);")
edit(J/"market/MarketContent.java","o.accept(EvergreenContent.BELL_ITEM.get());","o.accept(EvergreenContent.BELL_ITEM.get());o.accept(dev.mngklrd.midnighteve.alive.AliveContent.SOIL_ITEM.get());")
for n,relief in (("everlight_hamlet",18),("frostmarket",14),("hollycroft_farm",14),("evergreen_lodge",14)):
    p=D/"worldgen/structure"/(n+".json");d=json.loads(p.read_text())
    d["type"]=mod("flat_settlement");d["max_relief"]=relief
    for k in ("size","start_height","project_start_to_heightmap","max_distance_from_center","use_expansion_hack"):d.pop(k,None)
    write(p,d)
# A close-value blue-grey soil texture, with frost concentrated along mineral/dirt clusters.
rows=["mmssmmmmmhhmmsss","mssmmmhmmhhmmssm","ssmmmmhhmmmhmmmm","smmmhhhmmsmmmmmm","mmhhhmmmssmmmhmm","mmhhmmmmssmmhhmm","mmmmmmmssmmmmhmm","mmssmmmmmmmmmmmm","mssmmmhhmmmmssmm","ssmmmhhlhmmmssmm","mmmmhhllhmmmmmmm","hmmmhhhhmmmssmmm","hhmmmmmmmmsmmmhm","hmmmsmmmmmmmmhhm","mmmmssmmmhmmmmhm","ssmmmmmmmhhmmmmm"]
assert all(len(x)==16 for x in rows)
g.png(A/"textures/block/frozen_soil.png",rows,{k:g.rgba(c) for k,c in {"m":"788d99","s":"687e8c","h":"91a5ae","l":"adc0c3"}.items()})
write(A/"models/block/frozen_soil.json",{"parent":"minecraft:block/cube_all","textures":{"all":mod("block/frozen_soil")}})
write(A/"models/item/frozen_soil.json",{"parent":mod("block/frozen_soil")})
write(A/"blockstates/frozen_soil.json",{"variants":{"":{"model":mod("block/frozen_soil")}}})
write(D/"loot_table/blocks/frozen_soil.json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":mod("frozen_soil")}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
for tag in ("dirt","mineable/shovel"):
    p=R/"data/minecraft/tags/block"/(tag+".json")
    a=json.loads(p.read_text()) if p.exists() else {"replace":False,"values":[]}
    a["values"].append(mod("frozen_soil"));write(p,a)
p=D/"worldgen/noise_settings/frostlands.json";d=json.loads(p.read_text())
d["surface_rule"]["sequence"][1]["then_run"]["sequence"].insert(0,f.condition(f.floor(),f.condition(f.biome("silent_snowfields","firlight_woods","peppermint_grove","starlit_steppe"),f.condition(f.dry,f.condition(f.threshold(.03,.32),f.block(mod("frozen_soil")))))))
write(p,d)
# Snow in uncovered air below spruce canopy. OCEAN_FLOOR_WG height ignores
# leaves; placement predicate also requires a soil surface and snow survival.
write(D/"worldgen/configured_feature/under_fir_snow.json",{"type":"minecraft:random_patch","config":{"tries":24,"xz_spread":5,"y_spread":3,"feature":{"feature":{"type":"minecraft:simple_block","config":{"to_place":{"type":"minecraft:weighted_state_provider","entries":[{"data":{"Name":"minecraft:snow","Properties":{"layers":"1"}},"weight":5},{"data":{"Name":"minecraft:snow","Properties":{"layers":"2"}},"weight":3}]}}},"placement":[{"type":"minecraft:block_predicate_filter","predicate":{"type":"minecraft:all_of","predicates":[{"type":"minecraft:matching_blocks","blocks":["minecraft:air"]},{"type":"minecraft:matching_blocks","offset":[0,-1,0],"blocks":["minecraft:grass_block","minecraft:dirt","minecraft:podzol","minecraft:coarse_dirt",mod("frozen_soil")]},{"type":"minecraft:would_survive","state":{"Name":"minecraft:snow","Properties":{"layers":"1"}}}]}}]}}})
write(D/"worldgen/placed_feature/under_fir_snow.json",{"feature":mod("under_fir_snow"),"placement":[{"type":"minecraft:count","count":8},{"type":"minecraft:in_square"},{"type":"minecraft:heightmap","heightmap":"MOTION_BLOCKING_NO_LEAVES"},{"type":"minecraft:biome"}]})
for n in g.names:
    p=D/"worldgen/biome"/(n+".json");d=json.loads(p.read_text())
    if n in ("firlight_woods","peppermint_grove","silent_snowfields","starlit_steppe"):d["features"][9].append(mod("under_fir_snow"))
    if "music" in d["effects"]:d["effects"]["music"].update({"min_delay":0,"max_delay":40,"replace_current_music":True})
    write(p,d)
# Three-dimensional garland: segmented swooping green cord, red/gold/ice-blue
# bulbs hang off alternating points. Each bulb has a body and narrower cap.
tex={"wire":"minecraft:block/green_wool","red":"minecraft:block/red_wool","gold":"minecraft:block/gold_block","blue":"minecraft:block/packed_ice","white":"minecraft:block/white_wool","leaf":"minecraft:block/spruce_leaves"}
els=[]
for x,y in [(0,9),(3,8),(6,7),(9,7),(12,8),(15,9)]:
    els.append(f.elem([x,y,7],[min(x+3,16),y+2,9],"wire"))
for x,y,c in [(2,8,"red"),(5,7,"gold"),(8,6,"blue"),(11,7,"red"),(14,8,"gold")]:
    els.extend([f.elem([x-1,y-3,6],[x+1,y,10],c),f.elem([x-1,y,7],[x+1,y+1,9],"white"),f.elem([x,y-4,7],[x+1,y-3,9],c)])
write(A/"models/block/garland.json",{"parent":"minecraft:block/block","textures":{"particle":"minecraft:block/green_wool",**tex},"elements":els})
edit(J/"content/TalesContent.java",'decor("garland",0,0,0,5,16,7,11)','decor("garland",6,0,2,5,16,11,11)')
# Non-cubic existing gift block, variation selected by vanilla positional model RNG.
# Three distinct silhouettes; boxes, ribbon straps and bow loops are separate 3D parts.
text(J/"alive/GiftPile.java",'''package dev.mngklrd.midnighteve.alive;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
public final class GiftPile extends Block {
    public GiftPile(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(1,0,1,15,13,15);}
}
''')
p=J/"MidnightEve.java";edit(p,'BLOCKS.registerSimpleBlock("gift_crate",','BLOCKS.register("gift_crate",()->new dev.mngklrd.midnighteve.alive.GiftPile(')
edit(p,'BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.WOOL));','BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.WOOL).noOcclusion()));')
def package(x,y,z,W,H,L,paper,ribbon):
    X=x+W;Y=y+H;Z=z+L;cx=x+W//2;cz=z+L//2
    return [f.elem([x,y,z],[X,Y,Z],paper),f.elem([cx-1,y,z-.05],[cx+1,Y,Z+.05],ribbon),f.elem([x-.05,y,cz-1],[X+.05,Y,cz+1],ribbon),f.elem([cx-1,Y,cz-2],[cx+1,Y+1,cz+2],ribbon),f.elem([cx-3,Y,cz-1],[cx,Y+2,cz+1],ribbon),f.elem([cx,Y,cz-1],[cx+3,Y+2,cz+1],ribbon)]
gifttex={"red":"minecraft:block/red_wool","green":"minecraft:block/green_wool","blue":"minecraft:block/light_blue_wool","gold":"minecraft:block/yellow_wool","white":"minecraft:block/white_wool","particle":"minecraft:block/red_wool"}
for i,parts in enumerate([package(2,0,2,12,8,12,"red","gold"),package(3,0,3,10,11,10,"green","white"),package(1,0,2,8,5,9,"blue","white")+package(8,0,5,7,8,9,"red","gold")]):
    write(A/"models/block"/("gift_pile_"+str(i)+".json"),{"parent":"minecraft:block/block","textures":gifttex,"elements":parts})
write(A/"blockstates/gift_crate.json",{"variants":{"":[{"model":mod("block/gift_pile_"+str(i))} for i in range(3)]}})
write(A/"models/item/gift_crate.json",{"parent":mod("block/gift_pile_0")})
# Bell: a damped sequence of state models. Old ringing boolean remains compatible
# with existing saved worlds; no block entity/network renderer necessary.
p=J/"evergreen/CelebrationBell.java"
edit(p,'import net.minecraft.world.level.block.state.properties.BooleanProperty;','import net.minecraft.world.level.block.state.properties.BooleanProperty;\nimport net.minecraft.world.level.block.state.properties.IntegerProperty;')
edit(p,'public static final BooleanProperty RINGING=BooleanProperty.create("ringing");','public static final BooleanProperty RINGING=BooleanProperty.create("ringing");\n    public static final IntegerProperty SWING=IntegerProperty.create("swing",0,7);')
edit(p,'stateDefinition.any().setValue(RINGING,false)','stateDefinition.any().setValue(RINGING,false).setValue(SWING,0)')
edit(p,'b.add(RINGING);','b.add(RINGING,SWING);')
edit(p,'server.setBlock(p,s.setValue(RINGING,true),3);server.scheduleTick(p,this,40);','server.setBlock(p,s.setValue(RINGING,true).setValue(SWING,1),3);server.scheduleTick(p,this,2);')
edit(p,'if(s.getValue(RINGING))l.setBlock(p,s.setValue(RINGING,false),3);','if(!s.getValue(RINGING))return;int k=s.getValue(SWING);if(k>=7)l.setBlock(p,s.setValue(RINGING,false).setValue(SWING,0),3);else{l.setBlock(p,s.setValue(SWING,k+1),3);l.scheduleTick(p,this,2);}')
# JSON block rotations allow these exact angles, progressively damped.
states={}
for i,angle in enumerate((0,45,22.5,-45,-22.5,22.5,-22.5,0)):
    d=json.loads((A/"models/block/celebration_bell_idle.json").read_text())
    for part in d["elements"][4:]:
        if angle:part["rotation"]={"origin":[8,13,8],"axis":"x","angle":angle,"rescale":False}
    name="bell_swing_"+str(i);write(A/"models/block"/(name+".json"),d)
    for b in ("true","false"):states["ringing="+b+",swing="+str(i)]={"model":mod("block/"+name)}
write(A/"blockstates/celebration_bell.json",{"variants":states})
# Existing native templates: add hanging garlands, tree ornaments, and snow
# on exposed earth. Do not overwrite existing doors, panes, beds, or farm crops.
added=0
for path in sorted((D/"structure").glob("*.nbt")):
    root=f.read_nbt(gzip.decompress(path.read_bytes()));palette=root["palette"][1][1];blocks=root["blocks"][1][1];size=root["size"][1][1]
    idx={tuple(b["pos"][1][1]):b for b in blocks}
    def name(p):
        b=idx.get(p);return palette[b["state"][1]]["Name"][1] if b else ""
    def place(p,n,props=None):
        if not all(0<=x<b for x,b in zip(p,size)):return False
        if name(p) not in ("","minecraft:air","minecraft:snow"):return False
        entry={"Name":g.Str(g.mc(n))}
        if props:entry["Properties"]=g.C({k:g.Str(str(v).lower()) for k,v in props.items()})
        if entry not in palette:palette.append(entry)
        if p in idx:idx[p]["state"]=g.I(palette.index(entry))
        else:
            b={"pos":g.L(3,list(p)),"state":g.I(palette.index(entry))}
            idx[p]=b;blocks.append(b)
        return True
    for (x,y,z),b in list(idx.items()):
        n=name((x,y,z))
        if n=="minecraft:spruce_leaves" and (x+y+z)%7==0:
            for dx,dz in ((1,0),(0,1),(-1,0),(0,-1)):
                if place((x+dx,y,z+dz),"midnighteve:ornament"):added+=1;break
        if n=="minecraft:spruce_leaves" and (x+z)%9==0:
            if place((x,y-1,z),"midnighteve:garland"):added+=1
        if n in ("minecraft:grass_block","minecraft:podzol","minecraft:coarse_dirt",mod("frozen_soil")):
            if (x*7+z*11)%4==0:place((x,y+1,z),"snow",{"layers":1+(x+z)%2})
    if path.stem.startswith("everlight_hamlet"):
        # A vanilla bell provides a genuine MEETING_POINT POI. Nearby custom
        # bells remain audible decoration. Use a clear plaza location.
        p=(38,2,44)
        place(p,"cobblestone_wall",{"up":True,"north":"none","east":"none","south":"none","west":"none","waterlogged":False})
        place((38,3,44),"bell",{"attachment":"floor","facing":"north","powered":False})
    root["blocks"]=g.L(10,blocks);path.write_bytes(gzip.compress(b"\x0a\x00\x00"+g.payload(g.C(root)),mtime=0))
print("PASS: decorated tree volumes and native meeting bell",added)
# Distinct village/exploration soundtrack using the existing original Oggs.
# NeoForge's music selection event respects the user's normal music volume.
text(J/"client/WinterMusic.java",'''package dev.mngklrd.midnighteve.client;
import dev.mngklrd.midnighteve.content.TalesContent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
@EventBusSubscriber(modid="midnighteve",value=Dist.CLIENT)
public final class WinterMusic {
    private static int last=-1000;
    private static boolean village=false;
    private static net.minecraft.client.multiplayer.ClientLevel cachedLevel;
    @SubscribeEvent public static void music(SelectMusicEvent e){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!mc.level.getBiome(mc.player.blockPosition()).unwrapKey().map(k->k.location().getNamespace().equals("midnighteve")).orElse(false))return;
        if(cachedLevel!=mc.level||mc.player.tickCount-last>=60||mc.player.tickCount<last){
            cachedLevel=mc.level;last=mc.player.tickCount;
            int n=mc.level.getEntitiesOfClass(Villager.class,mc.player.getBoundingBox().inflate(52,16,52)).size();
            village=n>=(village?2:3);
        }
        e.overrideMusic(new Music(Holder.direct(village?TalesContent.DUSK.get():TalesContent.FJORDS.get()),0,20,true));
    }
}
''')
# Improve the market's visual hierarchy without changing server-authoritative actions.
p=J/"client/MarketScreen.java"
edit(p,'g.fill(x,y,x+304,y+216,0xff493b2b);g.fill(x+3,y+3,x+301,y+213,0xffa88e63);','g.fill(x,y,x+304,y+216,0xff334c3d);g.fill(x+2,y+2,x+302,y+214,0xffad8250);g.fill(x+4,y+4,x+300,y+212,0xff843f39);')
edit(p,'g.fill(x+9,y+9,x+295,y+31,0xffd2c19d);','g.fill(x+9,y+9,x+295,y+31,0xffd2c19d);g.fill(x+9,y+32,x+295,y+34,0xffad8250);for(int i=0;i<8;i++){int xx=x+14+i*39;g.fill(xx,y+4,xx+7,y+6,0xffd4b46c);g.fill(xx,y+209,xx+7,y+211,0xffd4b46c);}')
edit(p,'g.drawString(font,Component.translatable("market.ui.owned",menu.owned(i),o.count()),x+15,y+80,ACCENT,false);','g.drawString(font,Component.translatable("market.ui.owned",menu.owned(i),o.count()),x+15,y+80,ACCENT,false);g.fill(x+15,y+94,x+135,y+101,0xffbcaa84);g.fill(x+16,y+95,x+16+118*Math.min(menu.owned(i),o.count())/Math.max(1,o.count()),y+100,0xff6f8b58);')
g.en["block.midnighteve.frozen_soil"]="Frozen Soil";g.ru["block.midnighteve.frozen_soil"]="Промёрзшая почва"
for n,lang in (("en_us",g.en),("ru_ru",g.ru)):write(A/"lang"/(n+".json"),lang)
edit(R/"META-INF/neoforge.mods.toml",'version="0.12.0"','version="0.13.0"')
for file in R.rglob("*.json"):json.loads(file.read_text(encoding="utf8"))
print("PASS: Alive Settlements terrain sampling, gifts, garlands, snow, soil, village routines, music and animated bell")
