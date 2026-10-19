package dev.minemods.redstonevision.client;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class ClientGogglesExtensions {
    private ClientGogglesExtensions() {}

    public static void initialize(Consumer<IClientItemExtensions> accept) {
        accept.accept(new IClientItemExtensions() {
            private GogglesArmorModel cached;
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(
                LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (slot != EquipmentSlot.HEAD) return original;
                if (cached == null) cached = new GogglesArmorModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(GogglesArmorModel.LAYER));
                return cached;
            }
        });
    }
}
