package dev.minemods.worldlogicterrain.tree;

import java.util.ArrayList;
import dev.minemods.worldlogicterrain.worldgen.LandscapeMosaic;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EcologicalTreeFeature extends Feature<NoneFeatureConfiguration> {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(BuiltInRegistries.FEATURE,"worldlogicterrain");
    public static final Supplier<Feature<?>> ECOLOGICAL=FEATURES.register("ecological_trees",EcologicalTreeFeature::new);
    private enum Zone { OPEN,TEMPERATE,BOREAL,WET,DRY,TROPICAL,CHERRY,COAST,NONE }
    private record Form(String feature,String wood) {}
    public EcologicalTreeFeature(){super(NoneFeatureConfiguration.CODEC);}
    private static TagKey<Biome> tag(String name){return TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","tree_"+name));}
    private static boolean tagged(Holder<Biome> biome,String zone){return biome.is(tag(zone));}
    private static Zone zone(Holder<Biome> b){
        if(tagged(b,"coast"))return Zone.COAST;
        if(tagged(b,"cherry"))return Zone.CHERRY;
        if(tagged(b,"wet"))return Zone.WET;
        if(tagged(b,"boreal"))return Zone.BOREAL;
        if(tagged(b,"tropical"))return Zone.TROPICAL;
        if(tagged(b,"dry"))return Zone.DRY;
        if(tagged(b,"temperate"))return Zone.TEMPERATE;
        if(tagged(b,"open"))return Zone.OPEN;
        return Zone.NONE;
    }
    private static Holder<Biome> localBiome(WorldGenLevel level,BlockPos pos){
        var boreal=LandscapeMosaic.habitat(level,pos,tag("boreal"));
        if(boreal!=null)return boreal;
        var ground=level.getBiome(pos.below());
        if(zone(ground)!=Zone.NONE)return ground;
        var surface=level.getBiome(pos);
        if(zone(surface)!=Zone.NONE)return surface;
        for(var name:new String[]{"temperate","tropical","wet","dry","coast","cherry","open"}){
            var candidate=LandscapeMosaic.habitat(level,pos,tag(name));
            if(candidate!=null)return candidate;
        }
        return surface;
    }
    private static String id(Holder<Biome> b){return b.unwrapKey().map(k->k.location().getPath()).orElse("");}
    private static String namespace(String wood){return java.util.Set.of("willow","larch","pine","fir","cedar","maple","beech","alder","palm","date_palm","baobab","ebony","mahogany").contains(wood)?"worldlogicterrain":"minecraft";}
    static boolean soil(BlockState state){return state.is(BlockTags.DIRT)||state.is(Blocks.MUD)||state.is(Blocks.CLAY);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        WorldGenLevel level=context.level();
        var generator=context.chunkGenerator();
        RandomSource random=context.random();
        var origin=context.origin();
        // One immutable sampler per chunk batch. Candidates remain inside the chunk.
        var sampler=CoreHabitat.batch(level,generator);
        int startX=origin.getX()&~15,startZ=origin.getZ()&~15;
        long seed=level.getSeed();
        List<BlockPos> trunks=new ArrayList<>();
        boolean placed=false,deadwood=false;
        // Sample every 4x4 cell before extra boreal attempts. Small biome fragments
        // must not depend on a single random origin or a handful of lucky columns.
        int offset=random.nextInt(16);
        for(int i=0;i<48&&trunks.size()<7;i++){
            int cell=(i+offset)&15;
            int x=startX+(cell&3)*4+random.nextInt(4),z=startZ+(cell>>2)*4+random.nextInt(4);
            BlockPos pos=new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
            if(level.isOutsideBuildHeight(pos)||!level.ensureCanWrite(pos)||!level.ensureCanWrite(pos.below()))continue;
            var biome=localBiome(level,pos);Zone zone=zone(biome);
            if(zone==Zone.NONE||(i>=16&&zone!=Zone.BOREAL))continue;
            // High latitudes can have steep regional relief but still contain real soil benches.
            String name=id(biome);
            boolean mangrove=name.contains("mangrove");
            BlockState ground=level.getBlockState(pos.below());
            if(ground.is(Blocks.SNOW)&&level.ensureCanWrite(pos.below(2))&&soil(level.getBlockState(pos.below(2)))){
                pos=pos.below();ground=level.getBlockState(pos.below());
            }
            if(!soil(ground)&&!(zone==Zone.COAST&&ground.is(BlockTags.SAND))){
                // Mangrove roots alone may start in shallow water; no water or terrain is replaced by this selection step.
                if(!mangrove||!ground.is(Blocks.WATER))continue;
                BlockPos shallow=pos.below();
                if(level.ensureCanWrite(shallow.below())&&soil(level.getBlockState(shallow.below()))){pos=shallow;ground=level.getBlockState(pos.below());}
                else continue;
            }
            var local=sampler.sample(pos,biome,ground);
            double temperature=local.map(CoreHabitat.Sample::temperature).orElse((double)biome.value().getBaseTemperature()*25.0);
            if(zone==Zone.COAST&&temperature<18)continue;
            double field=LandscapeMosaic.forest(seed,x,z);
            double base=switch(zone){
                case OPEN -> name.equals("snowy_plains")?.035:.105;
                case TEMPERATE -> .68;
                case BOREAL -> name.equals("forest_tundra")?.14:.70;
                case WET -> mangrove?.68:.36;
                case DRY -> .15;
                case TROPICAL -> .82;
                case CHERRY -> .42;
                case COAST -> .34;
                default -> 0;
            };
            // Smooth, seeded groves instead of independent uniform rolls.
            double probability=base*LandscapeMosaic.forestDensity(field);
            boolean borealStand=zone==Zone.BOREAL&&!name.equals("forest_tundra");
            if(local.isPresent()){
                var h=local.get();
                if(h.zone().equals("MARINE")||h.zone().equals("SUBTERRANEAN")||h.slope()>(zone==Zone.BOREAL?55:42)||h.surface().equals("ROCKY")&&zone!=Zone.BOREAL||h.surface().equals("SANDY")&&!mangrove&&zone!=Zone.COAST)continue;
                if((h.temperature() < -10 && zone!=Zone.BOREAL)||name.equals("arctic_wetland"))continue;
                // Biome identity is authoritative for forest presence; temperature
                // and moisture select species rather than erasing a mapped taiga.
                if(h.coldness()>.88&&zone!=Zone.BOREAL||h.dryness()>.92&&zone!=Zone.DRY&&zone!=Zone.BOREAL&&zone!=Zone.COAST)continue;
                if(zone==Zone.BOREAL&&h.dryness()>.92)probability*=.70;
                probability*=Math.max(.15,Math.min(1.2,.35+.8*h.fertility()+.25*h.strength()));
                if(h.slope()>25)probability*=.55;
                if(h.waterlogging()>.8&&zone!=Zone.WET)probability*=.3;
                if(h.wind()>.75)probability*=.6;
            }
            // Protect a viable mapped boreal stand after climate multipliers, while
            // retaining genuine clearings and excluding marine, stone and ice above.
            if(borealStand&&field>.20&&local.map(h->h.waterlogging()<.88).orElse(true))
                probability=Math.max(probability,trunks.isEmpty()?.30:.14);
            if(random.nextDouble()>=probability)continue;
            boolean close=false;
            for(var previous:trunks)if(Math.abs(previous.getX()-x)<=3&&Math.abs(previous.getZ()-z)<=3){close=true;break;}
            if(close)continue;
            Form form=choose(zone,name,local,random);
            var key=ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","tree_"+form.feature()));
            if(level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(key).value().place(level,generator,random,pos)){
                placed=true;trunks.add(pos);
                if(zone==Zone.BOREAL||zone==Zone.TEMPERATE||zone==Zone.WET||zone==Zone.TROPICAL){
                    for(int n=0;n<3;n++){
                        int sx=x+random.nextInt(9)-4,sz=z+random.nextInt(9)-4;
                        if((sx&~15)!=startX||(sz&~15)!=startZ)continue;
                        BlockPos floor=new BlockPos(sx,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,sx,sz),sz);
                        if(random.nextInt(5)==0)recruit(level,floor,form.wood());
                        else {
                            double moisture=local.map(CoreHabitat.Sample::moisture).orElse(.5);
                            String cover=moisture>.57&&random.nextBoolean()?"low_fern":field<.45&&random.nextBoolean()?"forest_shrub":random.nextBoolean()?"forest_litter":"fallen_leaves";
                            understory(level,floor,cover);
                        }
                    }
                }
            }
            // Deadwood is a separate rare feature on nearby free, level ground.
            if(!deadwood&&!mangrove&&(zone==Zone.TEMPERATE||zone==Zone.BOREAL||zone==Zone.WET)&&random.nextInt(field>.65?24:48)==0){
                BlockPos start=new BlockPos(startX+3+random.nextInt(10),pos.getY(),startZ+3+random.nextInt(10));
                start=new BlockPos(start.getX(),level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,start.getX(),start.getZ()),start.getZ());
                Direction fall=local.map(EcologicalTreeFeature::fallDirection).orElseGet(()->random.nextBoolean()?Direction.EAST:Direction.SOUTH);
                deadwood=fallen(level,start,form.wood(),fall,3+random.nextInt(3));
                placed|=deadwood;
            }
        }
        return placed;
    }
    private static Form form(String feature,String wood){return new Form(feature,wood);}
    private static Form choose(Zone zone,String name,Optional<CoreHabitat.Sample> local,RandomSource random){
        double wind=local.map(CoreHabitat.Sample::wind).orElse(0.0);
        double fertility=local.map(CoreHabitat.Sample::fertility).orElse(.55);
        double moisture=local.map(CoreHabitat.Sample::moisture).orElse(.5);
        double river=local.map(CoreHabitat.Sample::riverbank).orElse(0.0);
        boolean young=wind>.72||fertility<.28||random.nextInt(5)==0;
        return switch(zone){
            case OPEN -> wind>.62?form("oak_windswept","oak"):young?form("oak_young","oak"):random.nextInt(14)==0?form("oak_ancient","oak"):form("oak_spreading","oak");
            case TEMPERATE -> {
                if(!name.contains("birch")&&random.nextInt(5)==0)yield form(young?"maple_young":"maple_spreading","maple");
                if(!name.contains("birch")&&moisture>.45&&random.nextInt(5)==0)yield form(young?"beech_young":"beech_tall","beech");
                if(name.contains("birch")||random.nextInt(5)==0)yield form(young?(random.nextBoolean()?"birch_pioneer":"birch_young"):"birch_tall","birch");
                if(name.equals("dark_forest")&&!young&&random.nextInt(3)==0)yield form("dark_oak_old","dark_oak");
                if(moisture>.68&&river>.45&&random.nextInt(3)==0)yield form(young?"willow_young":"willow_spreading","willow");
                yield form(wind>.68?"oak_windswept":young?"oak_young":random.nextInt(15)==0?"oak_ancient":"oak_spreading","oak");
            }
            case BOREAL -> {
                if(name.equals("forest_tundra")||name.equals("light_conifer_taiga")||random.nextInt(4)==0)yield form(wind>.62||name.equals("forest_tundra")?"larch_exposed":young?"larch_young":"larch_tall","larch");
                if(wind>.62||moisture<.43||name.equals("continental_taiga")&&random.nextBoolean())yield form(wind>.62?"pine_windswept":young?"pine_young":"pine_tall","pine");
                if(moisture>.60&&random.nextInt(3)==0)yield form(young?"fir_young":"fir_tall","fir");
                if(!name.equals("forest_tundra")&&random.nextInt(5)==0)yield form(young?"cedar_young":"cedar_spreading","cedar");
                if(young)yield form("spruce_young","spruce");
                if(!young&&fertility>.5&&(name.contains("old_growth")||name.contains("dark_conifer"))&&random.nextInt(7)==0)yield form("spruce_old","spruce");
                yield moisture>.55||random.nextBoolean()?form("spruce_dense","spruce"):form("pine_tall","pine");
            }
            case WET -> name.contains("mangrove")?form("mangrove_coast","mangrove"):random.nextBoolean()?form(young?"alder_young":"alder_spreading","alder"):form(young?"willow_young":"willow_spreading","willow");

            case COAST -> moisture<.45?form(young?"date_palm_young":"date_palm_tall","date_palm"):form(young?"palm_young":"palm_leaning","palm");
            case DRY -> {
                if(local.map(CoreHabitat.Sample::temperature).orElse(24.0)>21&&random.nextInt(5)==0)yield form(young?"baobab_young":"baobab_ancient","baobab");
                yield form(young?"acacia_stunted":"acacia_spreading","acacia");
            }
            case TROPICAL -> {
                if(random.nextInt(5)==0)yield form(young?"ebony_young":"ebony_spreading","ebony");
                if(random.nextInt(3)==0)yield form(young?"mahogany_young":"mahogany_canopy","mahogany");
                yield form(!young&&fertility>.46&&random.nextInt(6)==0?"jungle_giant":"jungle_canopy","jungle");
            }
            case CHERRY -> form("cherry_spreading","cherry");
            default -> form("oak_young","oak");
        };
    }
    /** Seeded smooth field; no per-world mutable cache. */
    static double grove(long seed,int x,int z){
        int gx=Math.floorDiv(x,64),gz=Math.floorDiv(z,64);
        double fx=Math.floorMod(x,64)/64.0,fz=Math.floorMod(z,64)/64.0;
        fx=fx*fx*(3-2*fx);fz=fz*fz*(3-2*fz);
        return (1-fz)*((1-fx)*noise(seed,gx,gz)+fx*noise(seed,gx+1,gz))+fz*((1-fx)*noise(seed,gx,gz+1)+fx*noise(seed,gx+1,gz+1));
    }
    private static double noise(long seed,int x,int z){
        long h=seed^(x*0x9E3779B97F4A7C15L)^(z*0xC2B2AE3D27D4EB4FL);
        h=(h^(h>>>30))*0xBF58476D1CE4E5B9L;h=(h^(h>>>27))*0x94D049BB133111EBL;h^=h>>>31;
        return (h>>>11)*0x1.0p-53;
    }
    private static Direction fallDirection(CoreHabitat.Sample h){
        if(Math.abs(h.windX())>=Math.abs(h.windZ()))return h.windX()>=0?Direction.EAST:Direction.WEST;
        return h.windZ()>=0?Direction.SOUTH:Direction.NORTH;
    }
    private static boolean safeFloor(WorldGenLevel level,BlockPos pos){
        return !level.isOutsideBuildHeight(pos)&&level.ensureCanWrite(pos)&&level.ensureCanWrite(pos.below())
            &&level.getBlockState(pos).isAir()&&soil(level.getBlockState(pos.below()));
    }
    static boolean recruit(WorldGenLevel level,BlockPos pos,String wood){
        if(!safeFloor(level,pos)||wood.equals("mangrove")||wood.equals("dark_oak"))return false;
        String namespace=namespace(wood);
        Block sapling=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(namespace,wood+"_sapling"));
        BlockState state=sapling.defaultBlockState();
        if(!(sapling instanceof net.minecraft.world.level.block.SaplingBlock)||!state.canSurvive(level,pos))return false;
        // Real saplings use the normal random-tick growth rules after chunk generation.
        return level.setBlock(pos,state,2);
    }
    static boolean understory(WorldGenLevel level,BlockPos pos,String name){
        if(!safeFloor(level,pos)||!java.util.Set.of("forest_litter","fallen_leaves","low_fern","forest_shrub").contains(name))return false;
        BlockState state=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain",name)).defaultBlockState();
        return !state.isAir()&&state.canSurvive(level,pos)&&level.setBlock(pos,state,2);
    }
    static boolean fallen(WorldGenLevel level,BlockPos start,String wood,Direction dir,int length){
        String namespace=namespace(wood);
        Block log=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(namespace,wood+"_log"));
        if(!log.defaultBlockState().hasProperty(RotatedPillarBlock.AXIS)||length<3||length>5)return false;
        for(int i=0;i<=length;i++){
            BlockPos p=start.relative(dir,i);
            if(!level.ensureCanWrite(p)||!level.ensureCanWrite(p.below())||level.isOutsideBuildHeight(p)||!soil(level.getBlockState(p.below())))return false;
            BlockState state=level.getBlockState(p);
            if(!state.isAir()&&!state.is(Blocks.SHORT_GRASS))return false;
        }
        // Do not partially place a trunk through a structure or terrain.
        level.setBlock(start,log.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Y),2);
        for(int i=1;i<=length;i++)level.setBlock(start.relative(dir,i),log.defaultBlockState().setValue(RotatedPillarBlock.AXIS,dir.getAxis()),2);
        return true;
    }
}
