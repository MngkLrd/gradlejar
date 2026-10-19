package dev.minemods.redstonevision.server;

import dev.minemods.redstonevision.item.GogglesPower;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid="redstonevision")
public final class GogglesFuelTicker {
    private GogglesFuelTicker() {}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (GogglesPower.drainTick(helmet))
            player.displayClientMessage(Component.translatable("message.redstonevision.empty"), true);
    }
}
