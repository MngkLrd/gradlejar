package com.kirill.pocketrealms.block;

import com.kirill.pocketrealms.world.PocketTeleporter;
import com.kirill.pocketrealms.blockentity.PocketDoorPortalBlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

/** A double-height real door. Open with right click, then step into the rift. */
public class PocketDoorBlock extends DoorBlock implements EntityBlock {
    public PocketDoorBlock(Properties properties) { super(BlockSetType.OAK, properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return state.getValue(HALF)==DoubleBlockHalf.LOWER?new PocketDoorPortalBlockEntity(pos,state):null;
    }
    private InteractionResult open(BlockState state, Level level, BlockPos pos, Player player) {
        if (!state.getValue(OPEN)) {
            if (!level.isClientSide()) {
                BlockPos bottom=state.getValue(HALF)==DoubleBlockHalf.UPPER?pos.below():pos;
                var dir=state.getValue(FACING);
                double dx=player.getX()-bottom.getX()-.5, dz=player.getZ()-bottom.getZ()-.5;
                boolean inside=dx*dir.getStepX()+dz*dir.getStepZ()>0;
                var hinge=inside?DoorHingeSide.RIGHT:DoorHingeSide.LEFT;
                if(state.getValue(HINGE)!=hinge){
                    level.setBlock(bottom,level.getBlockState(bottom).setValue(HINGE,hinge),2);
                    level.setBlock(bottom.above(),level.getBlockState(bottom.above()).setValue(HINGE,hinge),2);
                    state=level.getBlockState(pos);
                }
                setOpen(player,level,state,pos,true);
                // First pulse starts immediately; keep the portal alive while the door stays open.
                level.scheduleTick(bottom,this,2);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }


    /** Reliable multiplayer portal VFX: particles are broadcast by the server,
     * rather than depending on a rare randomDisplayTick client event. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if(!state.is(this) || state.getValue(HALF)!=DoubleBlockHalf.LOWER
                || !state.getValue(OPEN))return;
        var facing=state.getValue(FACING);
        double x=pos.getX()+.5;
        double z=pos.getZ()+.5;
        for(int i=0;i<10;i++){
            double across=(random.nextDouble()-.5)*.85;
            double height=.12+random.nextDouble()*1.75;
            double px=x+(facing.getAxis()==net.minecraft.core.Direction.Axis.X
                    ?(random.nextDouble()-.5)*.08:across);
            double pz=z+(facing.getAxis()==net.minecraft.core.Direction.Axis.Z
                    ?(random.nextDouble()-.5)*.08:across);
            level.sendParticles(ParticleTypes.PORTAL,px,pos.getY()+height,pz,
                    1,.03,.07,.03,.018);
            if(i%3==0)level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    px,pos.getY()+height,pz,1,.05,.05,.05,.006);
        }
        level.scheduleTick(pos,this,4);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        return open(state, level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
            BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        open(state, level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
