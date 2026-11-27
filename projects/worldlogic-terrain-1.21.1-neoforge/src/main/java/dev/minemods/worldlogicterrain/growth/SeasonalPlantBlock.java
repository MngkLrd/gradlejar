package dev.minemods.worldlogicterrain.growth;

import com.mojang.serialization.MapCodec;
import dev.minemods.worldlogicterrain.soil.SoilExhaustion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Old worlds and natural populations remain mature by default. Planting always uses age zero. */
public final class SeasonalPlantBlock extends BushBlock {
    public static final MapCodec<SeasonalPlantBlock> CODEC=simpleCodec(SeasonalPlantBlock::new);
    public static final IntegerProperty AGE=IntegerProperty.create("age",0,3),QUALITY=IntegerProperty.create("quality",0,3);
    public SeasonalPlantBlock(Properties props){super(props);registerDefaultState(stateDefinition.any().setValue(AGE,3).setValue(QUALITY,2));}
    @Override public MapCodec<SeasonalPlantBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(AGE,QUALITY);}
    @Override protected boolean mayPlaceOn(BlockState floor,BlockGetter level,BlockPos pos){
        if(super.mayPlaceOn(floor,level,pos)||floor.is(Blocks.MUD))return true;
        var profile=PlantSpecies.profile(defaultBlockState());
        return profile!=null&&(profile.sand()&&floor.is(BlockTags.SAND)||profile.minTemp()<0&&floor.is(Blocks.SNOW_BLOCK)&&level.getBlockState(pos.below()).is(BlockTags.DIRT));
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){
        return Block.box(2,0,2,14,switch(state.getValue(AGE)){case 0->5;case 1->9;case 2->13;default->16;},14);
    }
    @Override public void randomTick(BlockState state,ServerLevel l,BlockPos pos,RandomSource random){
        if(state.getValue(AGE)>=3)return;
        var p=PlantSpecies.profile(state);
        if(p!=null&&random.nextInt(p.tickDivisor())==0)advance(l,pos,SeasonalGrowth.at(l,pos,state),random);
    }
    static boolean advance(ServerLevel l,BlockPos pos,SeasonalGrowth.Result result,RandomSource random){
        var state=l.getBlockState(pos);
        if(!(state.getBlock() instanceof SeasonalPlantBlock)||state.getValue(AGE)>=3||result==null||result.rate()<=0||
            random.nextDouble()>=result.rate()||!state.canSurvive(l,pos))return false;
        int age=state.getValue(AGE),sample=(int)Math.round(PlantSpecies.clamp(result.quality())*3);
        int grade=age==0?sample:(int)Math.round((state.getValue(QUALITY)*age+sample)/(double)(age+1));
        SoilExhaustion.plantGrowth(l,pos,state);
        return l.setBlock(pos,state.setValue(AGE,age+1).setValue(QUALITY,grade),Block.UPDATE_ALL);
    }
}
