package dev.mngklrd.primitivewaterworks.blockentity;

import dev.mngklrd.primitivewaterworks.PrimitiveWaterworks;
import dev.mngklrd.primitivewaterworks.block.DownspoutBlock;
import dev.mngklrd.primitivewaterworks.block.GutterBlock;
import dev.mngklrd.primitivewaterworks.block.PipeConnections;
import dev.mngklrd.primitivewaterworks.block.RainCollectorBlock;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * No fluid capability is exposed: third-party pipes cannot pump arbitrary
 * fluids into this rainwater-only wooden network.
 */
public final class RainCollectorBlockEntity extends BlockEntity {
    private static final int COLLECTION_PER_SECOND=40;
    private static final int TRANSFER_PER_SECOND=100;
    private static final int MAX_NODES=128;
    private static final Direction[] DIRECTIONS={Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST,Direction.DOWN};

    private final FluidTank tank=new FluidTank(1000) {
        @Override public boolean isFluidValid(FluidStack stack){return stack.getFluid()==Fluids.WATER;}
        @Override protected void onContentsChanged(){setChanged();}
    };
    private long collectedTotal,deliveredTotal;
    public RainCollectorBlockEntity(BlockPos pos,BlockState state){
        super(PrimitiveWaterworks.COLLECTOR_BE.get(),pos,state);
    }
    public int getWater(){return tank.getFluidAmount();}
    public long getTotalCollected(){return collectedTotal;}
    public long getTotalDelivered(){return deliveredTotal;}

    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);tank.writeToNBT(tag);
        tag.putLong("totalCollected",collectedTotal);tag.putLong("totalDelivered",deliveredTotal);
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);tank.readFromNBT(tag);
        collectedTotal=Math.max(0,tag.getLong("totalCollected"));
        deliveredTotal=Math.max(0,tag.getLong("totalDelivered"));
    }
    public static void serverTick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,
            RainCollectorBlockEntity collector){
        if(!(level instanceof ServerLevel server)||server.getGameTime()%20!=0)return;
        if(server.isRainingAt(pos.above())){
            int inserted=collector.tank.fill(new FluidStack(Fluids.WATER,COLLECTION_PER_SECOND),
                IFluidHandler.FluidAction.EXECUTE);
            if(inserted>0){collector.collectedTotal+=inserted;collector.setChanged();}
        }
        boolean flowed=collector.tank.getFluidAmount()>0&&collector.transfer(server);
        collector.refreshVisual(flowed);
    }
    public void refreshVisual(boolean flowed) {
        if(!(level instanceof ServerLevel server))return;
        BlockState state=getBlockState();
        if(!(state.getBlock() instanceof RainCollectorBlock))return;
        // A flowing collector keeps a visible one-pixel stream even if the receiver
        // immediately empties its internal reserve in the same server tick.
        int amount=getWater();
        int levelValue=Math.min(4,(amount+249)/250);
        if(flowed)levelValue=Math.max(1,levelValue);
        if(state.getValue(RainCollectorBlock.WATER_LEVEL)!=levelValue){
            server.setBlock(worldPosition,state.setValue(RainCollectorBlock.WATER_LEVEL,levelValue),3);
        }
        if(flowed)server.scheduleTick(worldPosition,state.getBlock(),25);
    }

    private boolean transfer(ServerLevel server){
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        Set<BlockPos> visited=new HashSet<>();
        Map<BlockPos,BlockPos> parent=new HashMap<>();
        queue.add(worldPosition);visited.add(worldPosition);
        int budget=Math.min(TRANSFER_PER_SECOND,tank.getFluidAmount());
        boolean movedAnything=false;
        while(!queue.isEmpty()&&visited.size()<=MAX_NODES&&budget>0){
            BlockPos from=queue.removeFirst();
            BlockState state=server.getBlockState(from);
            boolean source=from.equals(worldPosition);
            for(Direction direction:DIRECTIONS){
                if(budget<=0)break;
                if(source ? !PipeConnections.sourceOutput(state,direction)
                          : !PipeConnections.conduitOutput(server,from,state,direction))continue;
                BlockPos next=from.relative(direction);
                if(!server.isLoaded(next))continue;
                BlockState other=server.getBlockState(next);
                if(PipeConnections.isConduit(other)){
                    if(PipeConnections.canEnterConduit(server,next,other,direction)
                        &&visited.size()<MAX_NODES&&visited.add(next.immutable())){
                        parent.put(next.immutable(),from);
                        queue.add(next.immutable());
                    }
                    continue;
                }
                if(other.getBlock() instanceof RainCollectorBlock)continue;
                BlockEntity target=server.getBlockEntity(next);
                if(target==null)continue;
                IFluidHandler handler=target.getCapability(ForgeCapabilities.FLUID_HANDLER,direction.getOpposite())
                    .resolve().orElse(null);
                if(handler==null)continue;
                FluidStack offer=tank.drain(budget,IFluidHandler.FluidAction.SIMULATE);
                if(offer.isEmpty()||offer.getFluid()!=Fluids.WATER)continue;
                int accepted=handler.fill(offer.copy(),IFluidHandler.FluidAction.SIMULATE);
                if(accepted<=0)continue;
                int intended=Math.min(accepted,budget);
                FluidStack toInsert=offer.copy();toInsert.setAmount(intended);
                int actual=Math.min(intended,handler.fill(toInsert,IFluidHandler.FluidAction.EXECUTE));
                if(actual<=0)continue;
                tank.drain(actual,IFluidHandler.FluidAction.EXECUTE);
                budget-=actual;deliveredTotal+=actual;movedAnything=true;setChanged();
                highlightRoute(server,from,parent);
            }
        }
        return movedAnything;
    }
    private void highlightRoute(ServerLevel server,BlockPos from,Map<BlockPos,BlockPos> parent){
        BlockPos cur=from;
        int safety=0;
        while(!cur.equals(worldPosition)&&safety++<MAX_NODES){
            BlockState pipe=server.getBlockState(cur);
            if(pipe.getBlock() instanceof GutterBlock) {
                if(!pipe.getValue(GutterBlock.WET))
                    server.setBlock(cur,pipe.setValue(GutterBlock.WET,true),3);
                server.scheduleTick(cur,pipe.getBlock(),30);
            } else if(pipe.getBlock() instanceof DownspoutBlock){
                if(!pipe.getValue(DownspoutBlock.WET))
                    server.setBlock(cur,pipe.setValue(DownspoutBlock.WET,true),3);
                server.scheduleTick(cur,pipe.getBlock(),30);
            }else break;
            BlockPos prev=parent.get(cur);
            if(prev==null)break;
            cur=prev;
        }
    }
}
