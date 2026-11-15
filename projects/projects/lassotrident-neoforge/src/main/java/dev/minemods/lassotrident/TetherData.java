package dev.minemods.lassotrident;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class TetherData {
    private static final String KEY = "lassotrident_attached";
    private TetherData() {}

    public static boolean hasLead(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(KEY);
    }

    public static void attachLead(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(KEY, true));
    }
}
