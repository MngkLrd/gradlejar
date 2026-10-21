package dev.norsegear.registry;

import dev.norsegear.NorseGear;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    /** Item tags can be extended by modpacks without changing Java code. */
    public static final TagKey<Item> FIELD_REPAIRABLE = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(NorseGear.MODID, "field_repairable"));
    public static final TagKey<Item> BOG_IRON_PICKAXES = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(NorseGear.MODID, "bog_iron_pickaxes"));
    private ModTags() {}
}
