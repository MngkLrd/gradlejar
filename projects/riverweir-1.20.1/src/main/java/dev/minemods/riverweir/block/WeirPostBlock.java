package dev.minemods.riverweir.block;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.blockentity.WeirPostBlockEntity;
import dev.minemods.riverweir.logic.WeirLogic;
import dev.minemods.riverweir.logic.WeirWater;
import dev.minemods.riverweir.item.NetRollItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class WeirPostBlock extends BaseEntityBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty WATER_KIND=net.minecraft.world.level.block.state.properties.IntegerProperty.create("water_kind",0,1);
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty FLOW=net.minecraft.world.level.block.state.properties.IntegerProperty.create("flow",0,16);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE=Block.box(5,0,5,11,16,11);

    public WeirPostBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED,false).setValue(CONNECTED,false).setValue(FACING,Direction.NORTH).setValue(WATER_KIND,0).setValue(FLOW,16));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        builder.add(WATERLOGGED,CONNECTED,FACING,WATER_KIND,FLOW);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        boolean wet=WeirWater.kind(ctx.getLevel().getFluidState(ctx.getClickedPos()))>=0;
        return defaultBlockState().setValue(WATERLOGGED,wet).setValue(FACING,ctx.getHorizontalDirection())
            .setValue(WATER_KIND,Math.max(0,WeirWater.kind(ctx.getLevel().getFluidState(ctx.getClickedPos()))))
            .setValue(FLOW,WeirWater.flow(ctx.getLevel().getFluidState(ctx.getClickedPos())));
    }
    @Override public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)?WeirWater.fluid(state.getValue(WATER_KIND),state.getValue(FLOW)):super.getFluidState(state);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new WeirPostBlockEntity(pos,state);}
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,RiverWeirMod.POST_BE.get(),WeirPostBlockEntity::serverTick);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player.getItemInHand(hand).getItem() instanceof NetRollItem)return InteractionResult.PASS;
        if(!level.isClientSide && player instanceof ServerPlayer server){
            if(player.isShiftKeyDown()&&WeirLogic.isJuteNet(player.getItemInHand(hand)))
                WeirLogic.repair((ServerLevel)level,pos,player,hand);
            else if(player.isShiftKeyDown()&&player.getItemInHand(hand).isEmpty())
                WeirLogic.clean((ServerLevel)level,pos,player);
            else if(WeirLogic.isBait(player.getItemInHand(hand)))
                WeirLogic.addBait((ServerLevel)level,pos,player,hand);
            else WeirLogic.openMenu((ServerLevel)level,pos,server);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level instanceof ServerLevel server)WeirLogic.dismantle(server,pos);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public BlockState updateShape(BlockState state,Direction direction,BlockState adjacent,LevelAccessor level,BlockPos pos,BlockPos neighbour){
        if(state.getValue(WATERLOGGED))level.scheduleTick(pos,state.getFluidState().getType(),state.getFluidState().getType().getTickDelay(level));
        return super.updateShape(state,direction,adjacent,level,pos,neighbour);
    }
}
