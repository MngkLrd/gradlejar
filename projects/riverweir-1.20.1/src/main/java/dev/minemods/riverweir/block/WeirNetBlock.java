package dev.minemods.riverweir.block;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.logic.WeirLogic;
import dev.minemods.riverweir.logic.WeirWater;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class WeirNetBlock extends Block {
    // Vanilla block-event packet; no additional persistent block-state combinations.
    public static final int TENSION_EVENT=41;
    @FunctionalInterface public interface TensionListener {
        void pulse(Level level,BlockPos pos,int side);
    }
    private static TensionListener tensionListener=(level,pos,side)->{};
    public static void setTensionListener(TensionListener listener){tensionListener=listener;}
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int data){
        if(id==TENSION_EVENT){
            if(level.isClientSide&&state.getValue(STAGE)<2)tensionListener.pulse(level,pos,data==1?1:-1);
            return true;
        }
        return super.triggerEvent(state,level,pos,id,data);
    }
    public static final EnumProperty<Direction.Axis> AXIS=EnumProperty.create("axis",Direction.Axis.class,Direction.Axis.X,Direction.Axis.Z);
    public static final IntegerProperty STAGE=IntegerProperty.create("stage",0,2);
    public static final IntegerProperty TIER=IntegerProperty.create("tier",0,2);
    public static final BooleanProperty WATERLOGGED=BlockStateProperties.WATERLOGGED;
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty WATER_KIND=net.minecraft.world.level.block.state.properties.IntegerProperty.create("water_kind",0,1);
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty FLOW=net.minecraft.world.level.block.state.properties.IntegerProperty.create("flow",0,16);
    public static final BooleanProperty TOP=BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM=BooleanProperty.create("bottom");
    public static final BooleanProperty TRAPPED=BooleanProperty.create("trapped");
    private static final VoxelShape X_SHAPE=Block.box(0,0,7,16,16,9);
    private static final VoxelShape Z_SHAPE=Block.box(7,0,0,9,16,16);

    public WeirNetBlock(Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS,Direction.Axis.X).setValue(STAGE,0)
            .setValue(WATERLOGGED,true).setValue(TIER,0).setValue(WATER_KIND,0).setValue(FLOW,16)
            .setValue(TOP,false).setValue(BOTTOM,false).setValue(TRAPPED,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){
        b.add(AXIS,STAGE,TIER,WATERLOGGED,WATER_KIND,FLOW,TOP,BOTTOM,TRAPPED);
    }
    // No BlockItem exists for individual installed panels. Return the actual tier roll
    // for Creative pick and HUDs that resolve the name from a picked item.
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockGetter level,BlockPos pos,BlockState state){
        return new net.minecraft.world.item.ItemStack(switch(state.getValue(TIER)){
            case 1 -> RiverWeirMod.BRONZE_NET_ROLL.get();
            case 2 -> RiverWeirMod.IRON_NET_ROLL.get();
            default -> RiverWeirMod.NET_ROLL.get();
        });
    }
    @Override public net.minecraft.network.chat.MutableComponent getName(){
        return net.minecraft.network.chat.Component.translatable("container.riverweir.weir");
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return state.getValue(AXIS)==Direction.Axis.X?X_SHAPE:Z_SHAPE;
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return state.getValue(STAGE)==2?net.minecraft.world.phys.shapes.Shapes.empty():getShape(state,level,pos,context);
    }
    @Override public FluidState getFluidState(BlockState state){
        return state.getValue(WATERLOGGED)?WeirWater.fluid(state.getValue(WATER_KIND),state.getValue(FLOW)):super.getFluidState(state);
    }
    @Override public BlockState updateShape(BlockState state,Direction direction,BlockState adjacent,LevelAccessor level,BlockPos pos,BlockPos neighbour){
        if(state.getValue(WATERLOGGED))level.scheduleTick(pos,state.getFluidState().getType(),state.getFluidState().getType().getTickDelay(level));
        return super.updateShape(state,direction,adjacent,level,pos,neighbour);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.isClientSide && player instanceof ServerPlayer server){
            BlockPos post=WeirLogic.findLinkedPost((ServerLevel)level,pos,state.getValue(AXIS));
            if(post!=null){
                if(player.isShiftKeyDown()&&WeirLogic.isJuteNet(player.getItemInHand(hand)))
                    WeirLogic.repair((ServerLevel)level,post,player,hand);
                else if(player.isShiftKeyDown()&&player.getItemInHand(hand).isEmpty())
                    WeirLogic.clean((ServerLevel)level,post,player);
                else if(WeirLogic.isBait(player.getItemInHand(hand)))
                    WeirLogic.addBait((ServerLevel)level,post,player,hand);
                else WeirLogic.openMenu((ServerLevel)level,post,server);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock()) && level instanceof ServerLevel server){
            BlockPos post=WeirLogic.findLinkedPost(server,pos,state.getValue(AXIS));
            if(post!=null)WeirLogic.tear(server,post);
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
