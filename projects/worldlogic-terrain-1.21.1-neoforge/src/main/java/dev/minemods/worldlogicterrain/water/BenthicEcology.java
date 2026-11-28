package dev.minemods.worldlogicterrain.water;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.ArrayList;

/** Chunk-local clustered bottom ecology. Samples only a bounded number of water columns. */
public final class BenthicEcology {
    private BenthicEcology(){}
    static BenthicBlock block(BenthicSpecies species){return (BenthicBlock)BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain",species.id));}
    static boolean inside(WorldGenLevel l,BlockPos p,int x0,int z0){return (p.getX()&~15)==x0&&(p.getZ()&~15)==z0&&!l.isOutsideBuildHeight(p)&&l.ensureCanWrite(p);}
    private record Column(BlockPos bed,int depth,double temperature,AquaticHabitat.Kind kind,int fluid){}
    private static Column column(WorldGenLevel l,AquaticHabitat.Prediction pred,int x,int z,int x0,int z0){
        int top=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        int y=l.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z);
        var p=new BlockPos(x,y,z);
        // Some generation stages have a stale ocean-floor heightmap; walk down through exposed sources.
        int depth=top-y;
        if(depth<1||depth>48||!inside(l,p,x0,z0)||!inside(l,p.below(),x0,z0))return null;
        var surface=new BlockPos(x,top-1,z);
        if(!inside(l,surface,x0,z0)||!AquaticHabitat.water(l.getBlockState(surface)))return null;
        var state=l.getBlockState(p);if(!AquaticHabitat.water(state)||!state.getFluidState().isSource())return null;
        var kind=pred.classify(l,p,state);
        if(!pred.available()&&BenthicBlock.nativeAvailable()&&state.is(net.minecraft.world.level.block.Blocks.WATER))return null;
        if(kind==AquaticHabitat.Kind.NONE||kind==AquaticHabitat.Kind.UNKNOWN||kind==AquaticHabitat.Kind.GROUNDWATER)return null;
        int f=BenthicBlock.worldgenIndex(state,kind,pred.available());if(f<0)return null;
        return new Column(p,depth,l.getBiome(surface).value().getBaseTemperature(),kind,f);
    }
    static boolean habitat(BenthicSpecies s,AquaticHabitat.Kind kind,double t,int depth){return s.accepts(kind)&&s.climate(t,depth);}
    private static BenthicSpecies choose(WorldGenLevel l,Column c,RandomSource r){
        var options=new ArrayList<BenthicSpecies>();int total=0;
        for(var s:BenthicSpecies.values())if(habitat(s,c.kind,c.temperature,c.depth)&&s.supports(l,c.bed.below())){options.add(s);total+=s.weight;}
        if(total==0)return null;
        int n=r.nextInt(total);
        for(var s:options){n-=s.weight;if(n<0)return s;}return null;
    }
    static boolean placeColumn(WorldGenLevel l,BlockPos pos,BenthicSpecies s,int fluid,int height,int x0,int z0){
        if(!inside(l,pos,x0,z0)||!inside(l,pos.below(),x0,z0)||!s.supports(l,pos.below())||BenthicBlock.fluid(fluid)==net.minecraft.world.level.material.Fluids.EMPTY)return false;
        var original=l.getBlockState(pos);if(!AquaticHabitat.water(original)||!original.getFluidState().isSource())return false;
        int source=BenthicBlock.index(original.getFluidState().getType());if(source<0||source!=0&&source!=fluid)return false;
        boolean placed=false;
        for(int i=0;i<Math.min(s.height,height);i++){
            var q=pos.above(i);if(!inside(l,q,x0,z0))break;
            var old=l.getBlockState(q);if(!AquaticHabitat.water(old)||!old.getFluidState().isSource()||BenthicBlock.index(old.getFluidState().getType())!=source)break;
            var state=block(s).defaultBlockState().setValue(BenthicBlock.WATER,fluid).setValue(BenthicBlock.SEGMENT,i);
            if(!state.canSurvive(l,q)||!l.setBlock(q,state,Block.UPDATE_CLIENTS))break;
            placed=true;
        }
        return placed;
    }
    static boolean generate(WorldGenLevel l,AquaticHabitat.Prediction pred,RandomSource r,int x0,int z0){
        boolean any=false;
        for(int i=0;i<36;i++){
            int x=x0+r.nextInt(16),z=z0+r.nextInt(16);
            var col=column(l,pred,x,z,x0,z0);if(col==null)continue;
            // Keep clear spaces; shallow freshwater vegetation is denser than deep marine fauna.
            double chance=col.kind==AquaticHabitat.Kind.SEA?(col.depth>18?.40:.70):.76;
            if(r.nextDouble()>=chance)continue;
            var s=choose(l,col,r);if(s==null)continue;
            int height=Math.min(col.depth,1+r.nextInt(s.height));
            if(!placeColumn(l,col.bed,s,col.fluid,height,x0,z0))continue;
            any=true;
            // The dominant species creates local colonies; each neighbor is independently checked.
            int spread=s.weight>=12?3:s.weight>=7?2:1;
            for(int n=0;n<spread;n++){
                int nx=x+r.nextInt(5)-2,nz=z+r.nextInt(5)-2;
                if((nx&~15)!=x0||(nz&~15)!=z0)continue;
                var other=column(l,pred,nx,nz,x0,z0);
                if(other==null||other.kind!=col.kind||Math.abs(other.bed.getY()-col.bed.getY())>2||!habitat(s,other.kind,other.temperature,other.depth))continue;
                any|=placeColumn(l,other.bed,s,other.fluid,Math.min(other.depth,1+r.nextInt(s.height)),x0,z0);
            }
        }
        return any;
    }
}
