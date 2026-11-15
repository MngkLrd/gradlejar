package dev.minemods.leashwrap;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod("leashwrap")
public final class LeashWrap {
    public LeashWrap() {
        NeoForge.EVENT_BUS.addListener(LeashWrap::rightClickBlock);
    }

    private static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(Items.LEAD)) return;
        if (!AnchorLogic.canAnchor(event.getLevel(), event.getPos())) return;

        if (event.getLevel() instanceof ServerLevel level) {
            if (!AnchorLogic.interact(level, event.getPos(), event.getEntity())) return;
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
