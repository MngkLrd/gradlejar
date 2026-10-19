package dev.minemods.redstonevision.server;

import dev.minemods.redstonevision.ModItems;
import dev.minemods.redstonevision.item.GogglesPower;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Per-player, server-only fuel ticking and equip transitions.
 * Weak keys ensure players who leave do not remain referenced.
 */
@EventBusSubscriber(modid="redstonevision")
public final class GogglesFuelTicker {
    private static final Map<ServerPlayer, ItemStack> PREVIOUS =
        Collections.synchronizedMap(new WeakHashMap<>());
    private GogglesFuelTicker() {}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack before = PREVIOUS.put(player, helmet);
        if (!helmet.is(ModItems.GOGGLES.get())) return;
        if (before != helmet) {
            if (GogglesPower.ticks(helmet) > 0) {
                GogglesPower.setEnabled(helmet, true);
                player.displayClientMessage(Component.translatable("message.redstonevision.auto"), true);
            } else {
                GogglesPower.setEnabled(helmet, false);
                player.displayClientMessage(Component.translatable("message.redstonevision.need_fuel"), true);
            }
        }
        if (GogglesPower.drainTick(helmet))
            player.displayClientMessage(Component.translatable("message.redstonevision.empty"), true);
    }
}
