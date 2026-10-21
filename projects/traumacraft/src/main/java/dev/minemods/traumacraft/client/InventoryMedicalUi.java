package dev.minemods.traumacraft.client;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.item.MedicineLogic;
import dev.minemods.traumacraft.mixin.ContainerScreenAccessor;
import dev.minemods.traumacraft.network.MedicalNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;

@Mod.EventBusSubscriber(modid = TraumaCraft.MOD_ID, value = Dist.CLIENT)
public final class InventoryMedicalUi {
    private InventoryMedicalUi() {}
    private static boolean hasRoom(InventoryScreen screen) {
        return screen.width >= MedicalPanelPainter.EXTENDED_WIDTH + 8;
    }

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventory) || !hasRoom(inventory)) return;
        ContainerScreenAccessor access = (ContainerScreenAccessor) inventory;
        int newX = MedicalPanelPainter.inventoryLeft(inventory.width);
        int delta = newX - access.traumacraft$getLeftPos();
        if (delta == 0) return;
        access.traumacraft$setLeftPos(newX);
        // The vanilla recipe-book button is a widget; move it with the inventory.
        for (var control : event.getListenersList()) {
            if (control instanceof AbstractWidget widget) widget.setX(widget.getX() + delta);
        }
    }

    @SubscribeEvent
    public static void preRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen inventory) || !hasRoom(inventory)) return;
        // Recipe-book layout can recompute leftPos after init; retain a stable expanded layout.
        ((ContainerScreenAccessor) inventory).traumacraft$setLeftPos(
                MedicalPanelPainter.inventoryLeft(inventory.width));
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventory) || !hasRoom(inventory)) return;
        int x = MedicalPanelPainter.panelX(MedicalPanelPainter.inventoryLeft(inventory.width));
        int y = MedicalPanelPainter.panelY(inventory.height);
        MedicalPanelPainter.render(event.getGuiGraphics(), x, y, event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen inventory) || !hasRoom(inventory)) return;
        int x = MedicalPanelPainter.panelX(MedicalPanelPainter.inventoryLeft(inventory.width));
        int y = MedicalPanelPainter.panelY(inventory.height);
        if (!MedicalPanelPainter.insidePanel(x, y, event.getMouseX(), event.getMouseY())) return;
        event.setCanceled(true); // IMPORTANT: prevent vanilla throwing the carried stack outside the slots.
        if (event.getButton() != 0 || Minecraft.getInstance().player == null) return;
        BodyPart part = MedicalPanelPainter.hovered(x, y, event.getMouseX(), event.getMouseY());
        if (part == null) return;
        var carried = Minecraft.getInstance().player.containerMenu.getCarried();
        if (MedicineLogic.medical(carried)) MedicalNetworking.treat(part);
    }

    @SubscribeEvent
    public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen inventory) || !hasRoom(inventory)) return;
        int x = MedicalPanelPainter.panelX(MedicalPanelPainter.inventoryLeft(inventory.width));
        int y = MedicalPanelPainter.panelY(inventory.height);
        if (MedicalPanelPainter.insidePanel(x, y, event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }
}
