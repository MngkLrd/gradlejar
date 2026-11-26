package dev.minemods.realisticdrops;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.TickEvent;
@Mod("realisticdrops")
public final class RealisticDrops {
    public RealisticDrops() {
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
        MinecraftForge.EVENT_BUS.addListener(this::onToss);
    }
    private void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) DropPhysics.sample(player);
    }
    private void onToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) DropPhysics.apply(player, event.getEntity());
    }
}
