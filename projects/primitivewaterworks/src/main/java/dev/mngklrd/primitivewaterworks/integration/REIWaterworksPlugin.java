package dev.mngklrd.primitivewaterworks.integration;

import dev.mngklrd.primitivewaterworks.PrimitiveWaterworks;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.CollapsibleEntryRegistry;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Optional client-only integration, discovered by REI on Forge when installed. */
@REIPluginClient
public final class REIWaterworksPlugin implements REIClientPlugin {
    @Override public void registerCollapsibleEntries(CollapsibleEntryRegistry registry) {
        group(registry, "rain_collectors");
        group(registry, "wooden_gutters");
        group(registry, "wooden_downspouts");
    }
    private static void group(CollapsibleEntryRegistry registry, String id) {
        ResourceLocation key = new ResourceLocation(PrimitiveWaterworks.MOD_ID, id);
        TagKey<Item> items = TagKey.create(Registries.ITEM, key);
        registry.group(key, Component.translatable("group.primitivewaterworks." + id),
            EntryIngredients.ofItemTag(items));
    }
}
