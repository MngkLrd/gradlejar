package com.kirill.harvestglow.crop;

import com.kirill.harvestglow.core.GlowSettings;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

/** Vanilla age checks plus guarded modded-block heuristics. */
public final class CropDetector {
    private CropDetector() {}
    public static boolean ready(BlockState state) {
        Block block = state.getBlock();
        if (state.isAir()) return false;
        String id = BuiltInRegistries.BLOCK.getKey(block).toString().toLowerCase(Locale.ROOT);
        if (GlowSettings.ignoredBlocks.contains(id)) return false;
        if (GlowSettings.forcedCropBlocks.contains(id)) return true;
        if (block == Blocks.SWEET_BERRY_BUSH)
            return state.getValue(SweetBerryBushBlock.AGE) >= 2;
        if (block instanceof CropBlock crop)
            return crop.isMaxAge(state);
        // Stems growing to their final age are not harvestable fruit.
        if (id.contains("stem") || id.contains("attached_stem")) return false;
        if (id.equals("minecraft:nether_wart") || id.equals("minecraft:cocoa"))
            return hasMatureGrowthProperty(state);
        if (id.equals("minecraft:cave_vines") || id.equals("minecraft:cave_vines_plant")) {
            for (Property<?> property : state.getProperties())
                if (property.getName().equals("berries"))
                    return Boolean.TRUE.equals(state.getValue((Property<Boolean>) property));
            return false;
        }
        if (!GlowSettings.looseModdedDetection) return false;
        if (!hasCropName(id)) return false;
        return hasMatureGrowthProperty(state);
    }
    private static boolean hasCropName(String id) {
        return id.contains("crop") || id.contains("plant") || id.contains("bush")
            || id.contains("herb") || id.contains("wart") || id.contains("rice")
            || id.contains("tomato") || id.contains("berry") || id.contains("carrot")
            || id.contains("potato") || id.contains("wheat") || id.contains("onion")
            || id.contains("cabbage") || id.contains("corn") || id.contains("flax");
    }
    private static boolean hasMatureGrowthProperty(BlockState state) {
        for (Property<?> p : state.getProperties()) {
            if (!(p instanceof IntegerProperty age) || !GlowSettings.growthProperties.contains(p.getName())) continue;
            int max = Integer.MIN_VALUE;
            for (int v : age.getPossibleValues()) if (v > max) max = v;
            if (max > 0 && state.getValue(age) >= max) return true;
        }
        return false;
    }
}
