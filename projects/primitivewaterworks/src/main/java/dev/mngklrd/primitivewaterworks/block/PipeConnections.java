package dev.mngklrd.primitivewaterworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Water-only gravity connection graph, never accepting upward routing. */
public final class PipeConnections {
    private PipeConnections() {}

    public static boolean isConduit(BlockState state) {
        return state.getBlock() instanceof GutterBlock || state.getBlock() instanceof DownspoutBlock;
    }
    public static boolean canJoinHorizontally(BlockGetter level,BlockPos origin,Direction direction) {
        if(!direction.getAxis().isHorizontal())return false;
        BlockPos at=origin.relative(direction);
        BlockState neighbor=level.getBlockState(at);
        if(neighbor.getBlock() instanceof GutterBlock)return true;
        if(neighbor.getBlock() instanceof DownspoutBlock)return true;
        if(neighbor.getBlock() instanceof RainCollectorBlock)
            return RainCollectorBlock.hasOutlet(neighbor,direction.getOpposite());
        return isFluidInput(level,at,direction.getOpposite());
    }
    public static boolean isFluidInput(BlockGetter level,BlockPos at,Direction side) {
        BlockEntity be=level.getBlockEntity(at);
        if(be==null || be instanceof dev.mngklrd.primitivewaterworks.blockentity.RainCollectorBlockEntity)
            return false;
        // Capability presence indicates a possible socket even if the tank is already full.
        // Real fluid acceptance is still checked during each server transfer.
        return be.getCapability(ForgeCapabilities.FLUID_HANDLER,side).isPresent();
    }
    public static boolean sourceOutput(BlockState source,Direction direction) {
        return direction==Direction.DOWN || RainCollectorBlock.hasOutlet(source,direction);
    }
    public static boolean conduitOutput(BlockGetter level,BlockPos pos,BlockState current,Direction direction) {
        if(direction==Direction.UP)return false;
        if(direction==Direction.DOWN)
            return current.getBlock() instanceof DownspoutBlock
                || (current.getBlock() instanceof GutterBlock && GutterBlock.hasDrop(level,pos));
        return canJoinHorizontally(level,pos,direction);
    }
    public static boolean canEnterConduit(BlockGetter level,BlockPos at,BlockState neighbor,Direction incoming) {
        if(neighbor.getBlock() instanceof DownspoutBlock)
            return incoming==Direction.DOWN ||
                (incoming.getAxis().isHorizontal() && canJoinHorizontally(level,at,incoming.getOpposite()));
        if(neighbor.getBlock() instanceof GutterBlock)
            return (incoming==Direction.DOWN && GutterBlock.hasInlet(level,at))
                || (incoming.getAxis().isHorizontal() && canJoinHorizontally(level,at,incoming.getOpposite()));
        return false;
    }
}
