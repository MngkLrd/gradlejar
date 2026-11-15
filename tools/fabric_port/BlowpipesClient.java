package dev.minemods.blowpipes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class BlowpipesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(DartRenderer.LAYER, DartModel::createBodyLayer);
        EntityRendererRegistry.register(Blowpipes.DART_TYPE(), DartRenderer::new);
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            ResourceLocation name = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!Blowpipes.ID.equals(name.getNamespace())) return;
            String id = name.getPath();
            switch (id) {
                case "bamboo_blowpipe", "hunting_blowpipe", "reinforced_blowpipe",
                     "plain_dart", "iron_dart", "poison_dart", "slowness_dart",
                     "fire_dart", "sleep_dart" -> {
                    lines.add(Component.translatable("tooltip.blowpipes." + id + ".primary")
                            .withStyle(ChatFormatting.GRAY));
                    lines.add(Component.translatable("tooltip.blowpipes." + id + ".secondary")
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
                default -> {}
            }
        });
    }
}
