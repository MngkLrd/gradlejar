package dev.umbral.block;

import dev.umbral.shadow.ShadowData;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class ProjectorBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
    public static final IntegerProperty RANGE=IntegerProperty.create("range",0,2);
    public static final int[] DISTANCES={8,12,16};
    public ProjectorBlock(Properties p){
        super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LIT,true).setValue(ENABLED,true).setValue(RANGE,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT,ENABLED,RANGE);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        return defaultBlockState().setValue(FACING,c.getHorizontalDirection()).setValue(LIT,!c.getLevel().hasNeighborSignal(c.getClickedPos()));
    }
    @Override public BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override public BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){
        super.onPlace(s,l,p,old,moved);
        if(l instanceof ServerLevel server&&!old.is(this))ShadowData.get(server).register(p);
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moved){
        if(l instanceof ServerLevel server&&!next.is(this))ShadowData.get(server).retire(p);
        super.onRemove(s,l,p,next,moved);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(!l.isClientSide){
            if(player.isShiftKeyDown()){
                boolean enabled=!s.getValue(ENABLED);
                l.setBlock(p,s.setValue(ENABLED,enabled).setValue(LIT,enabled&&!l.hasNeighborSignal(p)),3);
                player.displayClientMessage(Component.translatable(enabled?"umbral.enabled":"umbral.disabled"),true);
            }else{
                int range=(s.getValue(RANGE)+1)%DISTANCES.length;
                l.setBlock(p,s.setValue(RANGE,range),3);
                player.displayClientMessage(Component.translatable("umbral.range",DISTANCES[range]),true);
            }
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
