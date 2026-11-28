package dev.minemods.treechopper;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Re-send only the real server result of the primitive 2x2 recipe when its inputs change.
 * Never writes a client-predicted result or creates an item outside the vanilla result slot. */
final class PrimitiveRecipeSync {
    private static final Map<ServerPlayer,Long> LAST=new WeakHashMap<>();
    private PrimitiveRecipeSync(){}
    static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player)||player instanceof FakePlayer)return;
        var menu=player.inventoryMenu;
        if(player.containerMenu!=menu||!menu.getSlot(0).getItem().is(Items.STONE_AXE)){LAST.remove(player);return;}
        long signature=0;int flint=0,sticks=0;
        for(int i=1;i<=4;i++){
            var stack=menu.getSlot(i).getItem();int type=stack.is(Items.FLINT)?1:stack.is(Items.STICK)?2:stack.isEmpty()?0:3;
            if(type==1)flint++;if(type==2)sticks++;
            if(type==3){LAST.remove(player);return;}
            signature|=((long)(type*128+stack.getCount()))<<((i-1)*10);
        }
        if(flint!=1||sticks!=2){LAST.remove(player);return;}
        Long old=LAST.put(player,signature);
        if(old==null||old.longValue()!=signature)
            player.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId,menu.incrementStateId(),0,menu.getSlot(0).getItem().copy()));
    }
}
