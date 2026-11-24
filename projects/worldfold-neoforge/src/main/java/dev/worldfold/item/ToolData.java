package dev.worldfold.item;

import dev.worldfold.fold.FoldNetwork;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

final class ToolData {
    static CompoundTag read(ItemStack s){return s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    static void write(ItemStack s,CompoundTag t){s.set(DataComponents.CUSTOM_DATA,CustomData.of(t));}
    static boolean owner(CompoundTag t,ServerPlayer p){return !t.hasUUID("selection_owner")||t.getUUID("selection_owner").equals(p.getUUID())||p.hasPermissions(2);}
    static void clearSelection(CompoundTag t){t.remove("selection");t.remove("selection_owner");t.remove("branch_link");}
    static Component text(String key,Object...args){return Component.translatable("worldfold."+key,args);}
    static void error(ServerPlayer p,IllegalArgumentException ex){
        String key=ex.getMessage();
        if(key!=null&&java.util.Set.of("limit","overlap","blocked","owner","already_branch","buffer","occupied","occupied_or_blocked").contains(key))FoldNetwork.message(p,key);
        else FoldNetwork.message(p,"failure");
    }
}
