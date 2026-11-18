package com.kirill.keepsigntext;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Runs only on server gameplay events; leaves vanilla drop logic intact. */
@Mod("keepsigntext")
public final class KeepSignText {
    public KeepSignText(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(KeepSignText::onBlockDrops);
    }

    private static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBlockEntity() instanceof SignBlockEntity sign)) return;
        var data = TypedEntityData.<BlockEntityType<?>>of(sign.getType(), sign.saveCustomOnly(event.getLevel().registryAccess()));
        for (ItemEntity entity : event.getDrops()) {
            var stack = entity.getItem();
            if (stack.getItem() instanceof StandingAndWallBlockItem) {
                stack.set(DataComponents.BLOCK_ENTITY_DATA, data);
            }
        }
    }
}
