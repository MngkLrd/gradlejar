package com.kirill.simplesorter.sorting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Opt-in general-purpose storage; industrial IItemHandlers remain untouched. */
public final class StorageFilter {
    private static final TagKey<Block> ALLOW = TagKey.create(Registries.BLOCK,
            new ResourceLocation("simplesorter", "sortable_storage"));
    private static final TagKey<Block> DENY = TagKey.create(Registries.BLOCK,
            new ResourceLocation("simplesorter", "unsortable_storage"));

    private StorageFilter() {}

    public static boolean isEligible(BlockState state) {
        if (state.is(DENY)) return false;
        if (state.is(ALLOW)) return true;

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String ns = id.getNamespace();
        String path = id.getPath();

        // Exclude automation inventories by default: no stealing machine inputs.
        if (ns.equals("gtceu") || ns.equals("gregtech")
                || ns.equals("ae2") || ns.equals("appliedenergistics2")
                || ns.equals("mekanism") || ns.equals("create")
                || ns.equals("thermal")) return false;

        if (ns.equals("minecraft")) {
            return path.equals("chest") || path.equals("trapped_chest")
                    || path.equals("barrel") || path.endsWith("shulker_box");
        }
        // TFC barrels, vessels, crucibles and anvils have processing rules.
        if (ns.equals("tfc")) {
            return path.contains("/chest/") || path.contains("/crate/")
                    || path.endsWith("_chest") || path.endsWith("_crate");
        }
        return path.equals("chest") || path.endsWith("_chest")
                || path.contains("/chest/") || path.endsWith("_crate")
                || path.contains("/crate/") || path.endsWith("_drawer")
                || path.endsWith("_cabinet") || path.endsWith("_storage_box")
                || path.endsWith("_shulker_box");
    }
}
