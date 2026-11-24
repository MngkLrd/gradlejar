package dev.mngklrd.hearthbound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import javax.annotation.Nullable;

public final class VesselBlock extends BaseEntityBlock {
    public static final BooleanProperty LID=BooleanProperty.create("lid");
    public static final IntegerProperty STAGE=IntegerProperty.create("stage",0,4);
    public final int batches;
    public VesselBlock(int batches,Properties props){
        super(props);this.batches=batches;
        registerDefaultState(stateDefinition.any().setValue(LID,false).setValue(STAGE,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(LID,STAGE);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(1,0,1,15,s.getValue(LID)?14:12,15);}
    // A real hollow collision shape lets dropped items reach the vessel interior.
    private static final VoxelShape OPEN=Shapes.or(box(1,0,1,15,3,15),box(1,3,1,3,12,15),box(13,3,1,15,12,15),box(3,3,1,13,12,3),box(3,3,13,13,12,15));
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return s.getValue(LID)?getShape(s,l,p,c):OPEN;
    }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new VesselEntity(p,s);}
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){
        return l.isClientSide?null:createTickerHelper(type,Hearthbound.VESSEL.get(),VesselEntity::tick);
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(l.isClientSide)return InteractionResult.SUCCESS;
        return l.getBlockEntity(p) instanceof VesselEntity v?v.interact(player,hand):InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof VesselEntity v&&!l.isClientSide)v.spill();
        super.onRemove(s,l,p,next,moving);
    }
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){
        int stage=s.getValue(STAGE);if(stage<2)return;
        if(r.nextInt(s.getValue(LID)?8:3)==0){
            l.addParticle(stage==4?ParticleTypes.SMOKE:ParticleTypes.CAMPFIRE_COSY_SMOKE,p.getX()+0.35+r.nextDouble()*.3,p.getY()+.73,p.getZ()+.35+r.nextDouble()*.3,0,.025,0);
        }
    }
}
