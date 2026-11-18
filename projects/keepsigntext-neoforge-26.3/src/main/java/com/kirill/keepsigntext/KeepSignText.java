package com.kirill.keepsigntext;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.entity.SignBlockEntity;
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
        for (ItemEntity entity : event.getDrops()) {
            var stack = entity.getItem();
            if (stack.getItem() instanceof StandingAndWallBlockItem) {
                stack.set(DataComponents.SIGN_TEXT_FRONT, sign.getText(SignTextSlot.FRONT));
                stack.set(DataComponents.SIGN_TEXT_BACK, sign.getText(SignTextSlot.BACK));
                if (sign.isWaxed()) stack.set(DataComponents.WAXED, Unit.INSTANCE);
            }
        }
    }
}
