package dev.minemods.worldlogicterrain.tree;

import dev.minemods.worldlogicterrain.worldgen.LandscapeMosaic;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Mossy deadwood preserves the host species and Core material identity.
 * Local habitat is sampled by the owning tree feature; no climate simulation here. */
public final class MossyDeadwood {
    public static final List<String> SPECIES=List.of("oak","dark_oak","birch","spruce","willow","larch","pine","fir","cedar","maple","beech","alder");
    private MossyDeadwood(){}
    public static ResourceLocation mossyId(String wood){return ResourceLocation.fromNamespaceAndPath("worldlogicterrain","mossy_"+wood+"_log");}
    public static ResourceLocation originalId(String wood){
        if(!SPECIES.contains(wood))throw new IllegalArgumentException("Unknown mossy wood "+wood);
        return ResourceLocation.fromNamespaceAndPath(List.of("oak","dark_oak","birch","spruce").contains(wood)?"minecraft":"worldlogicterrain",wood+"_log");
    }
    public static Optional<ResourceLocation> original(BlockState state){
        var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
        for(String wood:SPECIES)if(mossyId(wood).equals(id))return Optional.of(originalId(wood));
        return Optional.empty();
    }
    public static boolean suitable(CoreHabitat.Sample h){
        return Double.isFinite(h.temperature())&&Double.isFinite(h.moisture())&&h.temperature()>=1&&h.temperature()<=32
            &&h.moisture()>=.60&&h.dryness()<=.48&&h.waterlogging()<.90
            &&!h.zone().equals("MARINE")&&!h.zone().equals("SUBTERRANEAN");
    }
    static boolean mossSite(WorldGenLevel l,BlockPos p,CoreHabitat.Batch sampler){
        var biome=EcologicalTreeFeature.localBiome(l,p);
        if(!EcologicalTreeFeature.mossBiome(biome))return false;
        // No guessed humidity on older Core or unsupported generators.
        return sampler.sample(p,biome,l.getBlockState(p.below())).map(MossyDeadwood::suitable).orElse(false);
    }
    static boolean free(WorldGenLevel l,BlockPos p){
        return !l.isOutsideBuildHeight(p)&&l.ensureCanWrite(p)&&l.ensureCanWrite(p.below())
            &&l.getFluidState(p).isEmpty()&&l.getFluidState(p.below()).isEmpty()
            &&(l.getBlockState(p).isAir()||l.getBlockState(p).is(Blocks.SHORT_GRASS));
    }
    static boolean ground(WorldGenLevel l,BlockPos p){
        return free(l,p)&&EcologicalTreeFeature.soil(l.getBlockState(p.below()));
    }
    static boolean sameChunk(BlockPos a,BlockPos b){
        return (a.getX()>>4)==(b.getX()>>4)&&(a.getZ()>>4)==(b.getZ()>>4);
    }
    static boolean canMoss(WorldGenLevel l,BlockPos start,String wood,Direction dir,int length,Predicate<BlockPos> suitable){
        if(!SPECIES.contains(wood)||length<3||length>5)return false;
        boolean top=false;
        for(int i=0;i<=length;i++){
            var p=start.relative(dir,i);
            if(!sameChunk(start,p)||!ground(l,p)||!suitable.test(p))return false;
            if(i>0&&free(l,p.above()))top=true;
        }
        return top;
    }
    /** Called only after successful native deadwood placement. At least one top
     * carpet when a free top exists; scattered patches preserve existing plants. */
    static int decorate(WorldGenLevel l,BlockPos start,Direction dir,int length,RandomSource random,Predicate<BlockPos> suitable){
        int top=0,around=0;
        var carpet=Blocks.MOSS_CARPET.defaultBlockState();
        for(int i=1;i<=length;i++){
            var p=start.relative(dir,i).above();
            if(free(l,p)&&carpet.canSurvive(l,p)&&(top==0||random.nextDouble()<.68)){
                if(l.setBlock(p,carpet,Block.UPDATE_CLIENTS))top++;
            }
        }
        var side=dir.getClockWise();
        for(int i=0;i<=length&&around<7;i++){
            for(int sign:new int[]{-1,1}){
                if(around>=7||random.nextDouble()>.74)continue;
                int distance=1+random.nextInt(2);
                var raw=start.relative(dir,i).relative(sign==1?side:side.getOpposite(),distance);
                if(!sameChunk(start,raw))continue;
                var p=LandscapeMosaic.surface(l,raw.getX(),raw.getZ());
                if(Math.abs(p.getY()-start.getY())>1||!ground(l,p)||!suitable.test(p)||!carpet.canSurvive(l,p))continue;
                if(l.setBlock(p,carpet,Block.UPDATE_CLIENTS))around++;
            }
        }
        return top+around;
    }
}
