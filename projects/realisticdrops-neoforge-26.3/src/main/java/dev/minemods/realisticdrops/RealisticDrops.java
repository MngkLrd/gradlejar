package dev.minemods.realisticdrops;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
@Mod("realisticdrops")
public final class RealisticDrops {
    public RealisticDrops() {
        NeoForge.EVENT_BUS.addListener(this::onTick);
        NeoForge.EVENT_BUS.addListener(this::onToss);
    }
    private void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) DropPhysics.sample(player);
    }
    private void onToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) DropPhysics.apply(player, event.getEntity());
    }
}
