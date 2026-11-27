package dev.minemods.worldlogicterrain.tree;

import java.util.ArrayList;
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
    private enum Zone { OPEN,TEMPERATE,BOREAL,WET,DRY,TROPICAL,CHERRY,NONE }
    private record Form(String feature,String wood) {}
    public EcologicalTreeFeature(){super(NoneFeatureConfiguration.CODEC);}
    private static TagKey<Biome> tag(String name){return TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","tree_"+name));}
    private static boolean tagged(Holder<Biome> biome,String zone){return biome.is(tag(zone));}
    private static Zone zone(Holder<Biome> b){
        if(tagged(b,"cherry"))return Zone.CHERRY;
        if(tagged(b,"wet"))return Zone.WET;
        if(tagged(b,"boreal"))return Zone.BOREAL;
        if(tagged(b,"tropical"))return Zone.TROPICAL;
        if(tagged(b,"dry"))return Zone.DRY;
        if(tagged(b,"temperate"))return Zone.TEMPERATE;
        if(tagged(b,"open"))return Zone.OPEN;
        return Zone.NONE;
    }
    private static String id(Holder<Biome> b){return b.unwrapKey().map(k->k.location().getPath()).orElse("");}
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
        for(int i=0;i<8;i++){
            int x=startX+random.nextInt(16),z=startZ+random.nextInt(16);
            BlockPos pos=new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
            if(level.isOutsideBuildHeight(pos)||!level.ensureCanWrite(pos)||!level.ensureCanWrite(pos.below()))continue;
            var biome=level.getBiome(pos);Zone zone=zone(biome);
            if(zone==Zone.NONE)continue;
            String name=id(biome);
            boolean mangrove=name.contains("mangrove");
            BlockState ground=level.getBlockState(pos.below());
            if(!soil(ground)){
                // Mangrove roots alone may start in shallow water; no water or terrain is replaced by this selection step.
                if(!mangrove||!ground.is(Blocks.WATER))continue;
                BlockPos shallow=pos.below();
                if(level.ensureCanWrite(shallow.below())&&soil(level.getBlockState(shallow.below()))){pos=shallow;ground=level.getBlockState(pos.below());}
                else continue;
            }
            var local=sampler.sample(pos,biome,ground);
            double field=grove(seed,x,z);
            double base=switch(zone){
                case OPEN -> name.equals("snowy_plains")?.035:.105;
                case TEMPERATE -> .68;
                case BOREAL -> name.equals("forest_tundra")?.14:.64;
                case WET -> mangrove?.68:.36;
                case DRY -> .15;
                case TROPICAL -> .82;
                case CHERRY -> .42;
                default -> 0;
            };
            // Smooth, seeded groves instead of independent uniform rolls.
            double probability=base*Math.max(0,Math.min(1.35,(field-.16)*1.8));
            if(local.isPresent()){
                var h=local.get();
                if(h.zone().equals("MARINE")||h.zone().equals("SUBTERRANEAN")||h.slope()>42||h.surface().equals("ROCKY")||h.surface().equals("SANDY")&&!mangrove)continue;
                if((h.temperature() < -10 && zone!=Zone.BOREAL)||name.equals("arctic_wetland"))continue;
                if(zone==Zone.BOREAL&&h.temperature()>23)probability*=.25;
                if(h.coldness()>.88&&zone!=Zone.BOREAL||h.dryness()>.92&&zone!=Zone.DRY)continue;
                probability*=Math.max(.15,Math.min(1.2,.35+.8*h.fertility()+.25*h.strength()));
                if(h.slope()>25)probability*=.55;
                if(h.waterlogging()>.8&&zone!=Zone.WET)probability*=.3;
                if(h.wind()>.75)probability*=.6;
            }
            if(random.nextDouble()>=probability)continue;
            boolean close=false;
            for(var previous:trunks)if(Math.abs(previous.getX()-x)<=3&&Math.abs(previous.getZ()-z)<=3){close=true;break;}
            if(close)continue;
            Form form=choose(zone,name,local,random);
            var key=ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","tree_"+form.feature()));
            if(level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(key).value().place(level,generator,random,pos)){
                placed=true;trunks.add(pos);
            }
            // Deadwood is a separate rare feature on nearby free, level ground.
            if(!deadwood&&!mangrove&&(zone==Zone.TEMPERATE||zone==Zone.BOREAL||zone==Zone.WET)&&random.nextInt(32)==0){
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
                if(name.contains("birch")||random.nextInt(5)==0)yield form(young?"birch_young":"birch_tall","birch");
                if(name.equals("dark_forest")&&!young&&random.nextInt(3)==0)yield form("dark_oak_old","dark_oak");
                if(moisture>.68&&river>.45&&random.nextInt(3)==0)yield form(young?"willow_young":"willow_spreading","willow");
                yield form(wind>.68?"oak_windswept":young?"oak_young":random.nextInt(15)==0?"oak_ancient":"oak_spreading","oak");
            }
            case BOREAL -> {
                if(name.equals("forest_tundra")||random.nextInt(4)==0)yield form(young?"larch_young":"larch_tall","larch");
                if(!young&&fertility>.5&&(name.contains("old_growth")||name.contains("dark_conifer"))&&random.nextInt(7)==0)yield form("spruce_old","spruce");
                yield form(moisture>.55||random.nextBoolean()?"spruce_dense":"pine_tall","spruce");
            }
            case WET -> name.contains("mangrove")?form("mangrove_coast","mangrove"):form(young?"willow_young":"willow_spreading","willow");
            case DRY -> form(young?"acacia_stunted":"acacia_spreading","acacia");
            case TROPICAL -> form(!young&&fertility>.46&&random.nextInt(6)==0?"jungle_giant":"jungle_canopy","jungle");
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
    static boolean fallen(WorldGenLevel level,BlockPos start,String wood,Direction dir,int length){
        String namespace=wood.equals("larch")||wood.equals("willow")?"worldlogicterrain":"minecraft";
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
