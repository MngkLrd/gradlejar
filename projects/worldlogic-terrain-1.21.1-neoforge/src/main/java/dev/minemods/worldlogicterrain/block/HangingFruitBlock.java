package dev.minemods.worldlogicterrain.block;

import dev.minemods.worldlogicterrain.tree.FruitEcology;
import dev.minemods.worldlogicterrain.tree.TreeFruit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A persistent fruiting site beneath an actual leaf; no changes to native leaves or leaf decay. */
public final class HangingFruitBlock extends Block {
    public static final IntegerProperty AGE=IntegerProperty.create("age",0,2);
    public static final int RIPE=2;
    private static final VoxelShape STEM=Block.box(7,14,7,10,16,10);
    private static final VoxelShape IMMATURE=Block.box(6,11,6,11,16,11);
    private static final VoxelShape FRUIT=Block.box(4,6,4,12,16,12);
    private final TreeFruit fruit;
    public HangingFruitBlock(TreeFruit fruit,Properties props){
        super(props);this.fruit=fruit;
        registerDefaultState(stateDefinition.any().setValue(AGE,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AGE);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){
        return switch(state.getValue(AGE)){case 0->STEM;case 1->IMMATURE;default->FRUIT;};
    }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        return fruit.supports(level.getBlockState(pos.above()))&&level.getFluidState(pos).isEmpty();
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){
        if(direction==Direction.UP&&!state.canSurvive(level,pos))return Blocks.AIR.defaultBlockState();
        return super.updateShape(state,direction,neighbor,level,pos,neighborPos);
    }
    @Override public InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(state.getValue(AGE)!=RIPE)return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        var current=level.getBlockState(pos);
        if(!current.is(this)||current.getValue(AGE)!=RIPE||player.isSpectator()||!player.mayBuild()||!current.canSurvive(level,pos))
            return InteractionResult.PASS;
        if(!level.setBlock(pos,current.setValue(AGE,0),Block.UPDATE_ALL))return InteractionResult.PASS;
        Block.popResource(level,pos,new ItemStack(fruit.food()));
        level.playSound(null,pos,SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,SoundSource.BLOCKS,.8f,1.05f);
        return InteractionResult.CONSUME;
    }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!state.canSurvive(level,pos)){level.destroyBlock(pos,true);return;}
        int age=state.getValue(AGE);
        if(age<RIPE&&!FruitEcology.isWinter(level.getBiome(pos))&&random.nextInt(3)==0){
            // Two random-tick growth transitions; the timer is naturally paused in unloaded chunks.
            level.setBlock(pos,state.setValue(AGE,age+1),Block.UPDATE_ALL);
        }
    }
}
