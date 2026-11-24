package dev.worldfold.fold;

import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Both ends use the top capability of a container directly beneath the anchor.
 * A persistent in-flight buffer owns extracted resources until accepted at the destination.
 * Routing, reversal and dismantling are forbidden while a buffer contains resources. */
public final class Transport {
    private Transport(){}
    private static IItemHandler items(ServerLevel l,Anchor a){
        return l.hasChunkAt(a.floor().below())?l.getCapability(Capabilities.ItemHandler.BLOCK,a.floor().below(),Direction.UP):null;
    }
    private static IFluidHandler fluids(ServerLevel l,Anchor a){
        return l.hasChunkAt(a.floor().below())?l.getCapability(Capabilities.FluidHandler.BLOCK,a.floor().below(),Direction.UP):null;
    }
    private static ItemStack insert(IItemHandler h,ItemStack s,boolean simulate){
        ItemStack rest=s.copy();
        for(int i=0;i<h.getSlots()&&!rest.isEmpty();i++)rest=h.insertItem(i,rest,simulate);
        return rest;
    }
    public static void tick(MinecraftServer server,FoldData data,FoldLink f){
        if(f.flow==0)return;
        Anchor from=f.flow==1?f.a:f.destination(),to=f.flow==1?f.destination():f.a;
        ServerLevel sl=from.level(server),dl=to.level(server);
        if(!from.installed(sl)||!to.installed(dl))return;
        IItemHandler dst=items(dl,to);IFluidHandler fdst=fluids(dl,to);
        // Flush previous extraction even when under strain. Never switch its destination.
        if(!f.item.isEmpty()&&dst!=null){
            ItemStack rest=insert(dst,f.item,false);
            if(rest.getCount()!=f.item.getCount()){f.item=rest;data.setDirty();}
        }
        if(!f.fluid.isEmpty()&&fdst!=null){
            int n=fdst.fill(f.fluid.copy(),FluidAction.EXECUTE);
            if(n>0){int left=f.fluid.getAmount()-Math.min(n,f.fluid.getAmount());f.fluid=left==0?FluidStack.EMPTY:f.fluid.copyWithAmount(left);data.setDirty();}
        }
        if(f.strain>=95)return;
        int itemRate=f.strain>=75?1:8,fluidRate=f.strain>=75?50:250;
        IItemHandler src=items(sl,from);
        if(f.item.isEmpty()&&src!=null&&dst!=null&&src!=dst){
            for(int i=0;i<src.getSlots();i++){
                ItemStack probe=src.extractItem(i,itemRate,true);
                if(probe.isEmpty())continue;
                int accepted=probe.getCount()-insert(dst,probe,true).getCount();
                if(accepted<=0)continue;
                ItemStack extracted=src.extractItem(i,accepted,false);
                if(!extracted.isEmpty()){f.item=extracted.copy();data.setDirty();}break;
            }
        }
        IFluidHandler fsrc=fluids(sl,from);
        if(f.fluid.isEmpty()&&fsrc!=null&&fdst!=null&&fsrc!=fdst){
            FluidStack probe=fsrc.drain(fluidRate,FluidAction.SIMULATE);
            if(!probe.isEmpty()){
                int accepted=Math.min(probe.getAmount(),fdst.fill(probe.copy(),FluidAction.SIMULATE));
                if(accepted>0){
                    FluidStack extracted=fsrc.drain(probe.copyWithAmount(accepted),FluidAction.EXECUTE);
                    if(!extracted.isEmpty()){f.fluid=extracted.copy();data.setDirty();}
                }
            }
        }
    }
}
