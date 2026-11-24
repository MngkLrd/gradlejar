package dev.minemods.campfirebatch;
import net.minecraft.world.InteractionResult;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.minecraft.util.TriState;

@Mod("campfirebatch")
public final class CampfireBatch {
    public CampfireBatch() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CampfireBatch::onRightClick);
    }

    private static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getUseBlock() == TriState.FALSE) return;
        InteractionResult result = BatchLoader.interact(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
}
