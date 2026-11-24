package dev.minemods.campfirebatch;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.Event;

@Mod("campfirebatch")
public final class CampfireBatch {
    public CampfireBatch() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, CampfireBatch::onRightClick);
    }

    private static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getUseBlock() == Event.Result.DENY) return;
        InteractionResult result = BatchLoader.interact(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
}
