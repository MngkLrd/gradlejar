package dev.minemods.redstonevision.network;

import dev.minemods.redstonevision.RedstoneVision;
import dev.minemods.redstonevision.ModItems;
import dev.minemods.redstonevision.item.GogglesPower;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record ToggleVisionPayload() implements CustomPacketPayload {
    public static final Type<ToggleVisionPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(RedstoneVision.MOD_ID,"toggle_vision"));
    public static final StreamCodec<FriendlyByteBuf, ToggleVisionPayload> STREAM_CODEC =
        StreamCodec.unit(new ToggleVisionPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (message, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.is(ModItems.GOGGLES.get())) {
                player.displayClientMessage(Component.translatable("message.redstonevision.need_goggles"), true);
                return;
            }
            boolean next = !GogglesPower.enabled(helmet);
            if (next && GogglesPower.ticks(helmet) == 0) {
                player.displayClientMessage(Component.translatable("message.redstonevision.need_fuel"), true);
                return;
            }
            GogglesPower.setEnabled(helmet, next);
            player.displayClientMessage(Component.translatable(next ? "message.redstonevision.on" : "message.redstonevision.off"), true);
        });
    }
}
