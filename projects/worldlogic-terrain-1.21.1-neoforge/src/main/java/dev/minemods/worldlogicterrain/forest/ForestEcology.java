package dev.minemods.worldlogicterrain.forest;

import dev.minemods.worldlogicterrain.tree.CoreHabitat;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Bounded, chunk-local decorations of actual generated tree sites. */
public final class ForestEcology {
    private ForestEcology(){}
    public record Site(BlockPos trunk,String wood,Optional<CoreHabitat.Sample> climate,boolean forest){}
    public static boolean inside(WorldGenLevel l,BlockPos p,int x0,int z0){
        return (p.getX()&~15)==x0&&(p.getZ()&~15)==z0&&!l.isOutsideBuildHeight(p)&&l.ensureCanWrite(p);
    }
    public static boolean place(WorldGenLevel l,BlockPos p,ForestSpecies s,Direction towardHost,int segment,int x0,int z0){
        if(towardHost.getAxis().isVertical()||segment<0||segment>3)return false;
        var host=s.wall()?p.relative(towardHost):p.below();
        if(!inside(l,p,x0,z0)||!inside(l,host,x0,z0)||!l.getBlockState(p).isAir()||!l.getFluidState(p).isEmpty())return false;
        BlockState state=s.block().defaultBlockState().setValue(ForestBlock.FACING,towardHost).setValue(ForestBlock.SEGMENT,segment);
        return state.canSurvive(l,p)&&l.setBlock(p,state,Block.UPDATE_CLIENTS);
    }
    public static boolean epiphyteHabitat(double t,double moisture,double dryness){return t>=-8&&t<=36&&moisture>=.56&&dryness<=.56;}
    public static boolean vineHabitat(double t,double moisture,double dryness){return t>=8&&t<=38&&moisture>=.48&&dryness<=.64;}
    private static BlockPos ground(WorldGenLevel l,int x,int z){return new BlockPos(x,l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);}
    public static int decorate(WorldGenLevel l,Site site,RandomSource r,int x0,int z0){
        var trunk=site.trunk();
        if(!inside(l,trunk,x0,z0)||!ForestSpecies.naturalLog(l.getBlockState(trunk)))return 0;
        var h=site.climate().orElse(null);
        double moisture=h==null?.5:h.moisture(),temperature=h==null?10:h.temperature(),dryness=h==null?.4:h.dryness();
        int count=0;
        // A separate RNG preserves the original tree generator's random stream.
        double rootChance=h!=null&&(h.slope()>12||h.riverbank()>.5)?.85:.50;
        int offset=r.nextInt(4);
        if(r.nextDouble()<rootChance)for(int n=0;n<2+r.nextInt(2);n++){
            Direction outward=Direction.from2DDataValue((offset+n)%4);
            var p=trunk.relative(outward);
            if(!inside(l,p,x0,z0))continue;
            p=ground(l,p.getX(),p.getZ());
            if(Math.abs(p.getY()-trunk.getY())>1)continue;
            if(place(l,p,ForestSpecies.root(site.wood()),outward.getOpposite(),0,x0,z0))count++;
        }
        if(!site.forest())return count;
        dev.minemods.worldlogicterrain.botany.TreeResourceBlock.attach(l,trunk,site.wood(),
            RandomSource.create(l.getSeed()^trunk.asLong()^0x424F54414E59L),x0,z0);
        if(h!=null&&epiphyteHabitat(temperature,moisture,dryness)&&r.nextInt(3)!=0){
            for(int n=0;n<3;n++){
                Direction outward=Direction.from2DDataValue(r.nextInt(4));var p=trunk.above(1+r.nextInt(4)).relative(outward);
                if(place(l,p,r.nextInt(3)==0?ForestSpecies.HANGING_LICHEN:ForestSpecies.BARK_MOSS,outward.getOpposite(),0,x0,z0))count++;
            }
        }
        if(h!=null&&vineHabitat(temperature,moisture,dryness)&&r.nextInt(4)==0){
            var s=temperature>20?ForestSpecies.VINE:ForestSpecies.IVY;
            Direction outward=Direction.from2DDataValue(r.nextInt(4));var p=trunk.above(3+r.nextInt(3)).relative(outward);
            for(int n=0;n<3;n++){if(!place(l,p.below(n),s,outward.getOpposite(),n,x0,z0))break;count++;}
        }
        // Natural exposed rock faces near moist wooded sites, never a forced rock replacement.
        if(h!=null&&epiphyteHabitat(temperature,moisture,dryness)&&r.nextInt(3)==0){
            for(int n=0;n<3;n++){
                int x=trunk.getX()+r.nextInt(9)-4,z=trunk.getZ()+r.nextInt(9)-4;
                if((x&~15)!=x0||(z&~15)!=z0)continue;
                var host=ground(l,x,z).below();
                if(!inside(l,host,x0,z0)||!l.getBlockState(host).is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD))continue;
                Direction outward=Direction.from2DDataValue(r.nextInt(4));
                ForestSpecies flora=vineHabitat(temperature,moisture,dryness)&&r.nextInt(4)==0?ForestSpecies.IVY:ForestSpecies.BARK_MOSS;
                if(place(l,host.relative(outward),flora,outward.getOpposite(),0,x0,z0))count++;
            }
        }
        int litter=0;
        for(int i=0;i<12&&litter<5;i++){
            int x=trunk.getX()+r.nextInt(9)-4,z=trunk.getZ()+r.nextInt(9)-4;
            if((x&~15)!=x0||(z&~15)!=z0)continue;
            var p=ground(l,x,z);if(Math.abs(p.getY()-trunk.getY())>2)continue;
            ForestSpecies s=r.nextInt(5)==0?ForestSpecies.BARK:ForestSpecies.conifer(site.wood())?ForestSpecies.NEEDLES:moisture>.55?ForestSpecies.DAMP_LEAVES:ForestSpecies.BARK;
            if(place(l,p,s,Direction.from2DDataValue(r.nextInt(4)),0,x0,z0)){litter++;count++;}
        }
        if(h!=null&&temperature>4&&temperature<35&&r.nextInt(5)==0){
            ForestSpecies thorn=moisture>.58?ForestSpecies.NETTLE:dryness>.45?ForestSpecies.THISTLE:ForestSpecies.THORN;
            int x=trunk.getX()+r.nextInt(7)-3,z=trunk.getZ()+r.nextInt(7)-3;
            if((x&~15)==x0&&(z&~15)==z0){var p=ground(l,x,z);if(Math.abs(p.getY()-trunk.getY())<=2&&place(l,p,thorn,Direction.NORTH,0,x0,z0))count++;}
        }
        return count;
    }
}
