package dev.minemods.qa;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class RegistryProbe implements ModInitializer {
    @Override public void onInitialize() {
        // Only the isolated diagnostic workflow enables this code-only positive control.
        if ("1".equals(System.getenv("MINEMODS_REGISTRY_FIXTURE"))) {
            Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath("minemods_registry_probe", "code_only"),
                new Item(new Item.Properties()));
        }
    }
}
