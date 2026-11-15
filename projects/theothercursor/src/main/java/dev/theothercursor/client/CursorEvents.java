package dev.theothercursor.client;

import dev.theothercursor.TheOtherCursor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = TheOtherCursor.MOD_ID, value = Dist.CLIENT)
public final class CursorEvents {
    private static final HauntingCursor CURSOR = new HauntingCursor();

    private CursorEvents() {}

    @SubscribeEvent
    public static void afterScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> container) {
            CURSOR.render(container, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        } else {
            CURSOR.reset();
        }
    }
}
