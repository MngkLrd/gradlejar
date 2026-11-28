package dev.minemods.worldlogicterrain.growth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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

/** Age 0 is the visible living mycelium, not an empty or harvested block. */
public final class FungalColonyBlock extends Block {
    public static final IntegerProperty AGE=IntegerProperty.create("age",0,2);
    private final FungalEcology.Species species;
    public FungalColonyBlock(FungalEcology.Species species,Properties properties){
        super(properties);this.species=species;
        registerDefaultState(stateDefinition.any().setValue(AGE,0));
    }
    public FungalEcology.Species species(){return species;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AGE);}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){
        return species.supports(l.getBlockState(p.below()))&&l.getFluidState(p).isEmpty();
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter l,BlockPos p,CollisionContext ctx){
        int h=state.getValue(AGE)==0?4:state.getValue(AGE)==1?8:species==FungalEcology.Species.MOREL?13:11;
        return Block.box(1,0,1,15,h,15);
    }
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState neighbor,LevelAccessor l,BlockPos pos,BlockPos other){
        if(d==Direction.DOWN&&!s.canSurvive(l,pos))return Blocks.AIR.defaultBlockState();
        return super.updateShape(s,d,neighbor,l,pos,other);
    }
    @Override public InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){
        if(s.getValue(AGE)!=2)return InteractionResult.PASS;
        if(l.isClientSide)return InteractionResult.SUCCESS;
        var actual=l.getBlockState(pos);
        if(!actual.is(this)||actual.getValue(AGE)!=2||!actual.canSurvive(l,pos)||p.isSpectator()||!p.mayBuild())return InteractionResult.PASS;
        if(!l.setBlock(pos,actual.setValue(AGE,0),Block.UPDATE_ALL))return InteractionResult.PASS;
        Block.popResource(l,pos,new ItemStack(species.item()));
        return InteractionResult.CONSUME;
    }
    @Override public void randomTick(BlockState s,ServerLevel l,BlockPos pos,RandomSource random){FungalEcology.tick(l,pos,random);}
}
