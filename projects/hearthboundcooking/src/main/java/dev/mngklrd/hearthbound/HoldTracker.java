package dev.mngklrd.hearthbound;
import java.util.WeakHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Server-only, staggered sampling of finished food still on a hot appliance. */
public final class HoldTracker {
    private HoldTracker(){}
    private static final WeakHashMap<BlockEntity,State> STATES=new WeakHashMap<>();
    private static final class State {
        final ItemStack[] last;
        final int[] seconds;
        State(int n){last=new ItemStack[n];seconds=new int[n];}
    }
    public static boolean tick(BlockEntity be,int first,int slots){
        if(be.getLevel()==null||be.getLevel().isClientSide||Math.floorMod(be.getLevel().getGameTime()+be.getBlockPos().asLong(),20L)!=0)return false;
        var opt=be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        if(opt.isEmpty())return false;
        var inv=opt.get();State state=STATES.computeIfAbsent(be,b->new State(slots));
        boolean changed=false;
        for(int i=0;i<slots&&first+i<inv.getSlots();i++){
            ItemStack s=inv.getStackInSlot(first+i);
            if(state.last[i]!=s){state.last[i]=s;state.seconds[i]=0;}
            if(!Quality.has(s)||!Quality.fresh(s)||HeatCapability.getTemperature(s)<180){state.seconds[i]=0;continue;}
            if(++state.seconds[i]>8&&(state.seconds[i]-8)%4==0&&Quality.get(s)>0){CookingReport.late(s,5);changed=true;}
        }
        if(changed)be.setChanged();
        return changed;
    }
}
