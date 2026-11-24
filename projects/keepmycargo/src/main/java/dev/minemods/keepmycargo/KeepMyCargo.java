package dev.minemods.keepmycargo;

import net.minecraft.network.chat.Component;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@Mod("keepmycargo")
public final class KeepMyCargo {
    public KeepMyCargo() {
        NeoForge.EVENT_BUS.addListener(this::join);
        NeoForge.EVENT_BUS.addListener(this::tooltip);
    }
    private void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && !event.isCanceled()) {
            CargoTransfer.restoreOnSpawn(event.getEntity());
        }
    }
    private void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (CargoTransfer.isPacked(stack)) {
            var contents = stack.get(net.minecraft.core.component.DataComponents.CONTAINER);
            event.getToolTip().add(Component.translatable("tooltip.keepmycargo.loaded", contents.nonEmptyStream().count()).withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }
}
