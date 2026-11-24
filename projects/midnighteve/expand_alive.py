#!/usr/bin/env python3
"""Alive Settlements: bounded terrain selection, decorative geometry, settlement life."""
import expand_market_ui as m
import json,gzip,math
v,e,w,f,g,t=m.v,m.e,m.w,m.f,m.g,m.t
R,D,A,J=m.R,m.D,m.A,m.J
write,text,mod,edit=m.write,m.text,m.mod,m.edit

text(J/"alive/AliveContent.java",'''package dev.mngklrd.midnighteve.alive;
import dev.mngklrd.midnighteve.MidnightEve;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;
public final class AliveContent {
    private static final DeferredRegister<StructureType<?>> STRUCTURES=DeferredRegister.create(Registries.STRUCTURE_TYPE,"midnighteve");
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,"midnighteve");
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,"midnighteve");
    public static final DeferredHolder<StructureType<?>,StructureType<FlatVillage>> FLAT=STRUCTURES.register("flat_village",()->()->FlatVillage.CODEC);
    public static final DeferredHolder<Feature<?>,Feature<NoneFeatureConfiguration>> UNDER_FIR=FEATURES.register("under_fir_snow",()->new UnderFirSnow(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<SoundEvent,SoundEvent> WINTER=SOUNDS.register("music.winter",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("midnighteve","music.winter")));
    public static final DeferredHolder<SoundEvent,SoundEvent> VILLAGE=SOUNDS.register("music.village",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("midnighteve","music.village")));
    public static final DeferredBlock<Block> SOIL=MidnightEve.BLOCKS.registerSimpleBlock("frozen_soil",BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).sound(SoundType.GRAVEL));
    public static final DeferredItem<BlockItem> SOIL_ITEM=MidnightEve.ITEMS.registerSimpleBlockItem(SOIL);
    public static final DeferredBlock<LightString> STRING=MidnightEve.BLOCKS.register("festive_lights",()->new LightString(BlockBehaviour.Properties.of().strength(.2F).sound(SoundType.WOOL).noOcclusion().noCollission().lightLevel(s->9)));
    public static final DeferredItem<BlockItem> STRING_ITEM=MidnightEve.ITEMS.registerSimpleBlockItem(STRING);
    public static final DeferredBlock<PresentBlock> PRESENT=MidnightEve.BLOCKS.register("stacked_presents",()->new PresentBlock(BlockBehaviour.Properties.of().strength(.4F).sound(SoundType.WOOL).noOcclusion()));
    public static final DeferredItem<BlockItem> PRESENT_ITEM=MidnightEve.ITEMS.registerSimpleBlockItem(PRESENT);
    public static void init(IEventBus bus){STRUCTURES.register(bus);FEATURES.register(bus);SOUNDS.register(bus);NeoForge.EVENT_BUS.addListener(SettlementLife::tick);}
}
''')
edit(J/"MidnightEve.java","        dev.mngklrd.midnighteve.market.MarketContent.init(bus);","        dev.mngklrd.midnighteve.market.MarketContent.init(bus);\n        dev.mngklrd.midnighteve.alive.AliveContent.init(bus);")
text(J/"alive/FlatVillage.java",'''package dev.mngklrd.midnighteve.alive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import java.util.Optional;
public final class FlatVillage extends Structure {
    public static final MapCodec<FlatVillage> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        settingsCodec(i),StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s->s.pool),
        Codec.intRange(12,48).fieldOf("sample_radius").forGetter(s->s.radius),
        Codec.intRange(4,24).fieldOf("max_relief").forGetter(s->s.relief)).apply(i,FlatVillage::new));
    private final Holder<StructureTemplatePool> pool;
    private final int radius,relief;
    private final JigsawStructure delegate;
    public FlatVillage(StructureSettings settings,Holder<StructureTemplatePool> pool,int radius,int relief){
        super(settings);this.pool=pool;this.radius=radius;this.relief=relief;
        delegate=new JigsawStructure(settings,pool,1,ConstantHeight.of(VerticalAnchor.absolute(-1)),false,Heightmap.Types.WORLD_SURFACE_WG);
    }
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ();
        int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
        // Only noise height samples, no chunk loads or recursive worldgen.
        for(int dx:new int[]{-radius,0,radius})for(int dz:new int[]{-radius,0,radius}){
            int y=c.chunkGenerator().getBaseHeight(x+dx,z+dz,Heightmap.Types.WORLD_SURFACE_WG,c.heightAccessor(),c.randomState());
            min=Math.min(min,y);max=Math.max(max,y);
            if(max-min>relief)return Optional.empty();
        }
        if(min<=c.chunkGenerator().getSeaLevel()+1)return Optional.empty();
        return delegate.findGenerationPoint(c);
    }
    @Override public StructureType<?> type(){return AliveContent.FLAT.get();}
}
'''.replace("net.minecraft.world.level.Heightmap","net.minecraft.world.level.levelgen.Heightmap"))
# Apply the terrain gate only to the settlement-sized templates.
# Moderate thresholds retain viable sites; never increase the vanilla locate radius.
for n,radius,relief in [("everlight_hamlet",40,14),("frostmarket",22,12),("hollycroft_farm",23,12)]:
    path=D/"worldgen/structure"/(n+".json");d=json.loads(path.read_text())
    keep={k:d[k] for k in ("biomes","step","spawn_overrides","terrain_adaptation","start_pool")}
    write(path,{"type":mod("flat_village"),**keep,"sample_radius":radius,"max_relief":relief})
    # Increase candidate density to offset rejected steep sites.
    path=D/"worldgen/structure_set"/(n+".json");d=json.loads(path.read_text())
    d["placement"]["spacing"]=24 if n=="everlight_hamlet" else 28
    d["placement"]["separation"]=10 if n=="everlight_hamlet" else 12;write(path,d)

text(J/"alive/LightString.java",'''package dev.mngklrd.midnighteve.alive;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
public final class LightString extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public LightString(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return s.getValue(FACING).getAxis()==Direction.Axis.Z?box(0,4,5,16,15,11):box(5,4,0,11,15,16);
    }
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
}
''')
text(J/"alive/PresentBlock.java",'''package dev.mngklrd.midnighteve.alive;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
public final class PresentBlock extends Block {
    private static final VoxelShape SHAPE=Shapes.or(box(1,0,2,10,7,12),box(7,0,5,15,11,14),box(2,7,3,9,13,10));
    public PresentBlock(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
}
''')
# Native 16px material textures, small color clusters, no procedural pixel noise.
soil=[
"bbssbbbmmmbbssbb","bssbbbbmmmbbbssb","bbsssbbmmmmbbbss","mbbbbssssbbbmbbb","mmbbbsssbbbmmmbb","mmmbbbssbbbmmmbb","bbmmmmbbbbssbbbb","bbbmmmmbbsssbbmm",
"ssbbbmmmbbbbbbmm","sssbbbbmmmbbbbmm","bssbbbmmmbbbssbb","bbbbbbmmmmbssbbb","mbbbssbbbbssbbbb","mmmbsssbbbbmmmbb","mmbbbssbbbmmmmbb","bbbbbsssbbmmmbbb"]
g.png(A/"textures/block/frozen_soil.png",soil,{k:g.rgba(c) for k,c in {"b":"768c93","s":"94a6aa","m":"687a80"}.items()})
write(A/"models/block/frozen_soil.json",{"parent":"minecraft:block/cube_all","textures":{"all":mod("block/frozen_soil")}})
write(A/"blockstates/frozen_soil.json",{"variants":{"":{"model":mod("block/frozen_soil")}}})
write(A/"models/item/frozen_soil.json",{"parent":mod("block/frozen_soil")})
for name in ("frozen_soil","festive_lights","stacked_presents"):
    write(D/"loot_table/blocks"/(name+".json"),{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":mod(name)}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
write(R/"data/minecraft/tags/block/mineable/shovel.json",{"replace":False,"values":[mod("frozen_soil")]})
# Only dirt-compatible surface patches in two winter biomes; preserve other geology.
path=D/"worldgen/noise_settings/frostlands.json";d=json.loads(path.read_text())
d["surface_rule"]["sequence"][1]["then_run"]["sequence"].insert(0,f.condition(f.floor(),f.condition(f.biome("silent_snowfields","starlit_steppe"),f.condition(f.dry,f.condition(f.threshold(-.20,.05),f.block(mod("frozen_soil")))))))
write(path,d)

# Add true 3D colored bulbs, cable, sockets and glass-like highlights.
for name,base,hi,shade in [("wire","49583b","68774d","35442f"),("red_bulb","b94a3f","ec8771","883d38"),("gold_bulb","c8a14e","efd489","95783c"),("green_bulb","618758","a0c381","446b44"),("blue_bulb","6393ae","abd1d9","466b89"),("red_wrap","ad4744","ca6155","853c3b"),("green_wrap","638450","819a63","465f40"),("gold_ribbon","c39f50","e7c478","95783b"),("cream_wrap","c8bda1","e1d8c0","a99a83")]:
    rows=["bbbbbbbbbbbbbbbb","bbhhhbbbbbbbbbbb","bbhhhbbbbbbbbbbb","bbb hbbbbbbbbbbb".replace(" ","b"),"bbbbbbbbbbbbbbbb","bbbbbbbssbbbbbbb","bbbbbbsssbbbbbbb","bbbbbbbssbbbbbbb","bbbbbbbbbbbbbbbb","bbbbbbbbbbbbbbbb","bbbbb hhhbbbbbbb".replace(" ","b"),"bbbbbbhhbbbbbbbb","bbbbbbbbbbbbbbbb","sbbbbbbbbbbbbbbb","ssbbbbbbbbbbbbbb","ssssssssssssssss"]
    g.png(A/"textures/block"/(name+".png"),rows,{k:g.rgba(c) for k,c in {"b":base,"h":hi,"s":shade}.items()})
els=[]
# Sagging segmented cable (one model spans a full block).
for lo,hi in [([0,12,7],[3,13,9]),([3,11,7],[6,12,9]),([6,10,7],[10,11,9]),([10,11,7],[13,12,9]),([13,12,7],[16,13,9])]:els.append(f.elem(lo,hi,"wire"))
for x,y,col in [(1,12,"red_bulb"),(5,11,"gold_bulb"),(9,10,"green_bulb"),(13,12,"blue_bulb")]:
    els.append(f.elem([x,y-2,7],[x+2,y,9],"wire"))
    bulb=f.elem([x,y-5,6],[x+2,y-2,10],col)
    for face in bulb["faces"].values():face["neoforge_data"]={"block_light":15,"sky_light":15}
    bulb["shade"]=False;els.append(bulb)
write(A/"models/block/festive_lights.json",{"parent":"minecraft:block/block","textures":{"particle":mod("block/wire"),**{n:mod("block/"+n) for n in ("wire","red_bulb","gold_bulb","green_bulb","blue_bulb")}},"elements":els})
write(A/"blockstates/festive_lights.json",{"variants":{"facing="+n:{"model":mod("block/festive_lights"),"y":y} for n,y in [("north",0),("east",90),("south",180),("west",270)]}})
write(A/"models/item/festive_lights.json",{"parent":mod("block/festive_lights")})
# Keep the old garland ID and recipe, upgrade its model, do not remove old placed blocks.
write(A/"models/block/garland.json",{"parent":mod("block/festive_lights")})
boxes=[]
for lo,hi,tex in [([1,0,2],[10,7,12],"red_wrap"),([7,0,5],[15,11,14],"green_wrap"),([2,7,3],[9,13,10],"cream_wrap")]:
    boxes.append(f.elem(lo,hi,tex))
    x,y,z=lo;X,Y,Z=hi;cx=(x+X)//2;cz=(z+Z)//2
    # Narrow raised ribbons and paired bows with explicit local UV.
    boxes+= [f.elem([cx-1,y,Z],[cx+1,Y,Z+.25],"gold_ribbon"),f.elem([cx-1,Y,z],[cx+1,Y+.25,Z],"gold_ribbon"),f.elem([x,Y,cz-1],[X,Y+.25,cz+1],"gold_ribbon"),f.elem([cx-3,Y,cz-1],[cx,Y+1,cz+1],"gold_ribbon"),f.elem([cx,Y,cz-1],[cx+3,Y+1,cz+1],"gold_ribbon")]
write(A/"models/block/stacked_presents.json",{"parent":"minecraft:block/block","textures":{"particle":mod("block/red_wrap"),**{n:mod("block/"+n) for n in ("red_wrap","green_wrap","cream_wrap","gold_ribbon")}},"elements":boxes})
write(A/"models/item/stacked_presents.json",{"parent":mod("block/stacked_presents")})
write(A/"blockstates/stacked_presents.json",{"variants":{"":{"model":mod("block/stacked_presents")}}})
t.recipe("festive_lights",["SRS","GTB"],{"S":"string","R":"redstone","G":"glowstone_dust","T":"torch","B":"glass"},4)
t.recipe("stacked_presents",["PGP","PRP"],{"P":"paper","G":"gold_nugget","R":"red_wool"})
edit(J/"market/MarketContent.java","o.accept(FrostMaterials.BRICKS_ITEM.get());","o.accept(FrostMaterials.BRICKS_ITEM.get());o.accept(dev.mngklrd.midnighteve.alive.AliveContent.SOIL_ITEM.get());o.accept(dev.mngklrd.midnighteve.alive.AliveContent.STRING_ITEM.get());o.accept(dev.mngklrd.midnighteve.alive.AliveContent.PRESENT_ITEM.get());")
# Replace outdoor gifts and plain garlands in structures, preserving functional crates indoors.
decor=0
for path in (D/"structure").glob("*.nbt"):
    root=f.read_nbt(gzip.decompress(path.read_bytes()));palette=root["palette"][1][1];blocks=root["blocks"][1][1]
    idx={tuple(b["pos"][1][1]):b for b in blocks}
    def name(pos):
        b=idx.get(pos);return palette[b["state"][1]]["Name"][1] if b else ""
    def put(pos,n,props=None):
        entry={"Name":g.Str(n)}
        if props:entry["Properties"]=g.C({k:g.Str(str(v).lower()) for k,v in props.items()})
        if entry not in palette:palette.append(entry)
        if pos in idx:
            idx[pos]["state"]=g.I(palette.index(entry))
        else:
            b={"pos":g.L(3,list(pos)),"state":g.I(palette.index(entry))};idx[pos]=b;blocks.append(b)
    # A tree has a continuous vertical spruce trunk and leaves around the trunk.
    size=root["size"][1][1]
    trees=[]
    for (x,y,z),b in list(idx.items()):
        if y!=2 or name((x,y,z))!="minecraft:spruce_log":continue
        if all(name((x,y+k,z))=="minecraft:spruce_log" for k in range(1,8)) and any(name((x+dx,y+4,z+dz))=="minecraft:spruce_leaves" for dx,dz in ((1,0),(-1,0),(0,1),(0,-1))):trees.append((x,y,z))
    for x,y,z in trees:
        for dy,r in ((4,4),(7,3),(10,2)):
            for dx,dz,face in ((r+1,0,"east"),(-r-1,0,"west"),(0,r+1,"south"),(0,-r-1,"north")):
                p=(x+dx,y+dy,z+dz)
                if all(0<=a<b for a,b in zip(p,size)) and name(p) in ("","minecraft:air"):
                    put(p,mod("festive_lights"),{"facing":face});decor+=1
        for dx,dz in ((-2,-2),(2,2),(-2,2),(2,-2)):
            p=(x+dx,y,z+dz)
            if name(p)==mod("gift_crate"):put(p,mod("stacked_presents"));decor+=1
        for dx in range(-4,5):
            for dz in range(-4,5):
                p=(x+dx,y,z+dz)
                if dx*dx+dz*dz<=18 and name(p) in ("","minecraft:air") and name((p[0],y-1,p[2])) in ("minecraft:grass_block","minecraft:spruce_planks","minecraft:cobblestone","minecraft:stone_bricks",mod("froststone_bricks")):
                    if all(0<=a<b for a,b in zip(p,size)):put(p,"minecraft:snow",{"layers":1+(abs(dx+dz)%2)})
    root["blocks"]=g.L(10,blocks)
    path.write_bytes(gzip.compress(b"\x0a\x00\x00"+g.payload(g.C(root)),mtime=0))
# Snow under wild spruces: scan only the current 16x16 chunk, bounded vertical range.
text(J/"alive/UnderFirSnow.java",'''package dev.mngklrd.midnighteve.alive;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
public final class UnderFirSnow extends Feature<NoneFeatureConfiguration>{
    public UnderFirSnow(Codec<NoneFeatureConfiguration> codec){super(codec);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        WorldGenLevel l=c.level();int x0=c.origin().getX()&~15,z0=c.origin().getZ()&~15;int placed=0;
        for(int x=x0;x<x0+16;x+=2)for(int z=z0;z<z0+16;z+=2){
            int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            BlockPos p=new BlockPos(x,y,z);
            if(!l.isEmptyBlock(p)||l.canSeeSky(p))continue;
            boolean foliage=false;for(int h=2;h<=14;h++)if(l.getBlockState(p.above(h)).is(BlockTags.LEAVES)){foliage=true;break;}
            var st=Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS,1+c.random().nextInt(2));
            if(foliage&&st.canSurvive(l,p)){l.setBlock(p,st,2);placed++;}
        }
        return placed>0;
    }
}
''')
write(D/"worldgen/configured_feature/under_fir_snow.json",{"type":mod("under_fir_snow"),"config":{}})
write(D/"worldgen/placed_feature/under_fir_snow.json",{"feature":mod("under_fir_snow"),"placement":[{"type":"minecraft:biome"}]})
for n in ("firlight_woods","peppermint_grove","silent_snowfields"):
    path=D/"worldgen/biome"/(n+".json");d=json.loads(path.read_text());d["features"][9].append(mod("under_fir_snow"));write(path,d)
print("PASS: bounded terrain sampling, frozen soil, volumetric bulbs and parcels, under-canopy snow, decorated tree additions",decor)
