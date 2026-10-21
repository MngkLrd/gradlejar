package dev.norsegear.client;

import dev.norsegear.NorseGear;
import dev.norsegear.client.model.NorseArmorModel;
import dev.norsegear.client.renderer.ThrownWeaponRenderer;
import dev.norsegear.registry.ModEntities;
import dev.norsegear.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = NorseGear.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRegistration {
    public static final ModelLayerLocation ARMOR_LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(NorseGear.MODID, "volumetric_armor"), "main");

    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ARMOR_LAYER, NorseArmorModel::createLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.JAVELIN.get(), ThrownWeaponRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWING_AXE.get(), ThrownWeaponRenderer::new);
        event.registerEntityRenderer(ModEntities.SLING_STONE.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void armorExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private NorseArmorModel model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity wearer,
                    ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (model == null) model = new NorseArmorModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(ARMOR_LAYER));
                // NeoForge may request the same model for all four equipment slots.
                // Copy the entity's CURRENT animated pose before exposing individual parts.
                // Without this the extra shoulder/leg geometry can remain in a bind pose.
                copyArmorPose(original, model);
                model.onlyPiece(slot);
                return model;
            }
        }, ModItems.NORSE_HELMET.get(), ModItems.NORSE_CHESTPLATE.get(),
           ModItems.NORSE_LEGGINGS.get(), ModItems.NORSE_BOOTS.get());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void copyArmorPose(HumanoidModel<?> source, NorseArmorModel target) {
        // The source and target share the vanilla humanoid part layout.
        // Generics differ only in their entity type, not in their pose data.
        ((HumanoidModel) source).copyPropertiesTo(target);
    }
}
