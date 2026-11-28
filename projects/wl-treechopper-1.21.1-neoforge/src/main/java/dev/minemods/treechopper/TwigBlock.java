package dev.minemods.treechopper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class TwigBlock extends FallingBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    final String species;
    TwigBlock(Properties props,String species){super(props);this.species=species;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected com.mojang.serialization.MapCodec<? extends FallingBlock> codec(){return simpleCodec(p->new TwigBlock(p,species));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){
        return state.getValue(FACING).getAxis()==Direction.Axis.Z?Block.box(2,0,1,14,2,16):Block.box(1,0,2,16,2,14);
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){return Shapes.empty();}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(level instanceof ServerLevel l&&player instanceof net.minecraft.server.level.ServerPlayer server){
            if(!player.mayBuild()||!TreeChopper.allowed(l,server,pos,state))return InteractionResult.FAIL;
            if(l.removeBlock(pos,false)&&!player.getAbilities().instabuild){
                ItemStack stick=new ItemStack(Items.STICK);
                if(!player.getInventory().add(stick))Block.popResource(l,pos,stick);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
