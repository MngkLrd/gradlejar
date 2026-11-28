package dev.minemods.treechopper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class PebbleBlock extends Block {
    private static final VoxelShape OUTLINE=Block.box(4,0,4,12,3,12);
    final ResourceLocation source;
    final boolean flint;
    PebbleBlock(Properties properties,ResourceLocation source,boolean flint){super(properties);this.source=source;this.flint=flint;}
    ItemStack collectedStack(){return new ItemStack(flint?Items.FLINT:asItem());}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        var support=level.getBlockState(pos.below());
        return (BuiltInRegistries.BLOCK.getKey(support.getBlock()).equals(source)||!flint&&support.is(Blocks.GRASS_BLOCK))
            &&support.isFaceSturdy(level,pos.below(),Direction.UP);
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){
        return direction==Direction.DOWN&&!state.canSurvive(level,pos)?Blocks.AIR.defaultBlockState():super.updateShape(state,direction,neighbor,level,pos,neighborPos);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){return OUTLINE;}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){return Shapes.empty();}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(level instanceof ServerLevel l&&player instanceof net.minecraft.server.level.ServerPlayer server){
            if(!player.mayBuild()||!TreeChopper.allowed(l,server,pos,state))return InteractionResult.FAIL;
            if(l.removeBlock(pos,false)&&!player.getAbilities().instabuild){
                ItemStack drop=collectedStack();
                if(!player.getInventory().add(drop))Block.popResource(l,pos,drop);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
