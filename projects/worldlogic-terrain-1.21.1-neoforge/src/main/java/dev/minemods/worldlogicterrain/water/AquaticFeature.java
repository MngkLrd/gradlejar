package dev.minemods.worldlogicterrain.water;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AquaticFeature extends Feature<NoneFeatureConfiguration> {
    public static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(BuiltInRegistries.FEATURE,"worldlogicterrain");
    public static final Supplier<Feature<?>> AQUATIC=FEATURES.register("aquatic_ecology",AquaticFeature::new);
    public AquaticFeature(){super(NoneFeatureConfiguration.CODEC);}
    private static Block block(String id){return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain",id));}
    private static boolean inside(WorldGenLevel l,BlockPos p,int x0,int z0){
        return (p.getX()&~15)==x0&&(p.getZ()&~15)==z0&&!l.isOutsideBuildHeight(p)&&l.ensureCanWrite(p);
    }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        var l=c.level();var r=c.random();int x0=c.origin().getX()&~15,z0=c.origin().getZ()&~15;
        var prediction=AquaticHabitat.prediction(l,c.chunkGenerator());
        boolean any=false;
        for(int i=0;i<24;i++){
            int x=x0+r.nextInt(16),z=z0+r.nextInt(16);
            BlockPos p=new BlockPos(x,l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
            if(!inside(l,p,x0,z0)||!inside(l,p.below(),x0,z0)||!l.getBlockState(p).isAir())continue;
            BlockState below=l.getBlockState(p.below());
            if(AquaticHabitat.water(below)){
                // Respect the actual fluid, core forecast and local depth; never replace the fluid block.
                if(!below.getFluidState().isSource()||r.nextInt(3)!=0)continue;
                var kind=prediction.classify(l,p.below(),below);
                if(l.getBiome(p).value().getBaseTemperature()<=.15f)continue;
                int depth=waterDepth(l,p,x0,z0);
                String species=floatingSpecies(kind,depth,r);
                if(species!=null&&placePlant(l,p,species,x0,z0)){
                    any=true;
                    if(species.equals("duckweed")){
                        // One small natural group; check every neighboring water column independently.
                        for(Direction dir:Direction.Plane.HORIZONTAL){
                            if(r.nextBoolean())continue;
                            BlockPos q=p.relative(dir);
                            if(!inside(l,q,x0,z0)||!inside(l,q.below(),x0,z0)||!l.getBlockState(q).isAir())continue;
                            var floor=l.getBlockState(q.below());
                            if(AquaticHabitat.water(floor)&&floor.getFluidState().isSource()&&
                               AquaticHabitat.floating(prediction.classify(l,q.below(),floor))&&waterDepth(l,q,x0,z0)>=1&&waterDepth(l,q,x0,z0)<=4)
                                any|=placePlant(l,q,"duckweed",x0,z0);
                        }
                    }
                }
                continue;
            }
            // Shoreline may sit one block above the water surface. Limit reads to this chunk.
            for(Direction dir:Direction.Plane.HORIZONTAL){
                BlockPos bank=p.below().relative(dir);
                if(!inside(l,bank,x0,z0))continue;
                var water=l.getBlockState(bank);
                if(!AquaticHabitat.water(water)&&water.isAir()&&inside(l,bank.below(),x0,z0)){
                    bank=bank.below();water=l.getBlockState(bank);
                }
                if(!AquaticHabitat.water(water))continue;
                var kind=prediction.classify(l,bank,water);
                String species=shoreSpecies(kind,r);
                if(species!=null&&l.getBiome(p).value().getBaseTemperature()>.15f)
                    any|=placePlant(l,p,species,x0,z0);
                break;
            }
        }
        boolean nativeWorld=c.chunkGenerator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator n
            &&n.generatorSettings().unwrapKey().map(k->k.location().toString().equals("worldlogic:realistic_unified")).orElse(false);
        // Keep the earlier surface-plant random sequence unchanged.
        return BenthicEcology.generate(l,prediction,r,x0,z0,nativeWorld)|any;
    }
    static int waterDepth(WorldGenLevel l,BlockPos surface,int x0,int z0){
        int depth=0;
        for(int i=1;i<=5;i++){
            BlockPos p=surface.below(i);
            if(!inside(l,p,x0,z0))return 5;
            if(!AquaticHabitat.water(l.getBlockState(p)))break;
            depth++;
        }
        return depth;
    }
    static String floatingSpecies(AquaticHabitat.Kind kind,int depth,RandomSource random){
        if(!AquaticHabitat.floating(kind)||depth<1||depth>4)return null;
        if(depth>2||kind==AquaticHabitat.Kind.POND||kind==AquaticHabitat.Kind.WETLAND)
            return "duckweed";
        return random.nextInt(3)==0?"duckweed":"water_lily";
    }
    static String shoreSpecies(AquaticHabitat.Kind kind,RandomSource random){
        return switch(kind){
            case SEA,BRACKISH -> random.nextBoolean()?"dry_grass":"tall_dry_grass";
            case RIVER,SPRING -> random.nextBoolean()?"reed":"sedge";
            case WETLAND,POND,LAKE,FRESH_HINT -> switch(random.nextInt(4)){
                case 0 -> "reed"; case 1 -> "cattail"; case 2 -> "sedge"; default -> "marsh_marigold";};
            default -> null;
        };
    }
    static boolean placePlant(WorldGenLevel l,BlockPos p,String id,int x0,int z0){
        if(!inside(l,p,x0,z0)||!inside(l,p.below(),x0,z0)||!l.getBlockState(p).isAir())return false;
        BlockState state=block(id).defaultBlockState();
        if(state.isAir()||!state.canSurvive(l,p))return false;
        if(state.getBlock() instanceof DoublePlantBlock){
            if(!inside(l,p.above(),x0,z0)||!l.getBlockState(p.above()).isAir())return false;
            DoublePlantBlock.placeAt(l,state,p,2);return true;
        }
        return l.setBlock(p,state,2);
    }
}
