package dev.minemods.redstonevision.client;

import dev.minemods.redstonevision.ModItems;
import dev.minemods.redstonevision.item.GogglesPower;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;

public final class VisionState {
    public static final int RANGE=16;
    public static final float TERRAIN_ALPHA=0.30f;
    private VisionState() {}
    public static boolean hasGoggles() {
        Minecraft mc=Minecraft.getInstance();
        return mc.player!=null && mc.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GOGGLES.get());
    }
    public static boolean active() {
        Minecraft mc=Minecraft.getInstance();
        return mc.level!=null && hasGoggles()
            && GogglesPower.enabled(mc.player.getItemBySlot(EquipmentSlot.HEAD));
    }
}
