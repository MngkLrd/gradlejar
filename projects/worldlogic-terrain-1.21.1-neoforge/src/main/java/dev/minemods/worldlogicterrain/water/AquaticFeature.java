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
        boolean any=false;
        for(int i=0;i<16;i++){
            int x=x0+r.nextInt(16),z=z0+r.nextInt(16);
            BlockPos p=new BlockPos(x,l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
            if(!inside(l,p,x0,z0)||!inside(l,p.below(),x0,z0)||!l.getBlockState(p).isAir())continue;
            BlockState below=l.getBlockState(p.below());
            if(AquaticHabitat.water(below)){
                // Source-only floating vegetation, shallow open water and no fluid replacement.
                if(r.nextInt(4)!=0||!inside(l,p.below(3),x0,z0))continue;
                var kind=AquaticHabitat.hint(below,l.getBiome(p.below()));
                if(AquaticHabitat.floating(kind)&&!AquaticHabitat.water(l.getBlockState(p.below(3)))
                    &&l.getBiome(p).value().getBaseTemperature()>.15f)
                    any|=placePlant(l,p,"water_lily",x0,z0);
                continue;
            }
            // Read only within the decorated chunk and only actual adjacent water.
            for(Direction dir:Direction.Plane.HORIZONTAL){
                BlockPos bank=p.below().relative(dir);
                if(!inside(l,bank,x0,z0))continue;
                var water=l.getBlockState(bank);
                if(!AquaticHabitat.water(water))continue;
                var kind=AquaticHabitat.hint(water,l.getBiome(bank));
                String species=shoreSpecies(kind,r);
                if(species!=null&&l.getBiome(p).value().getBaseTemperature()>.15f)
                    any|=placePlant(l,p,species,x0,z0);
                break;
            }
        }
        return any;
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
