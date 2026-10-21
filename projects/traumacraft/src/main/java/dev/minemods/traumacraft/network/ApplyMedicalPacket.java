package dev.minemods.traumacraft.network;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.health.MedicalAction;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.registry.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Applies the medical item held by the inventory's CURSOR to one selected body part. */
public record ApplyMedicalPacket(int bodyPartOrdinal) implements CustomPacketPayload {
    public static final Type<ApplyMedicalPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID, "apply_medicine"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyMedicalPacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> buf.writeVarInt(packet.bodyPartOrdinal),
                    buf -> new ApplyMedicalPacket(buf.readVarInt()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ApplyMedicalPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!player.isAlive() || player.isSpectator()) return;
        // Never accept arbitrary inventory slot numbers or client-provided item IDs.
        if (player.containerMenu != player.inventoryMenu) return;
        if (packet.bodyPartOrdinal < 0 || packet.bodyPartOrdinal >= BodyPart.values().length) return;
        ItemStack cursor = player.containerMenu.getCarried();
        if (cursor.isEmpty() || !(cursor.getItem() instanceof MedicalItem medical)) return;
        BodyPart part = BodyPart.values()[packet.bodyPartOrdinal];
        BodyState state = player.getData(ModAttachments.BODY.get());
        if (!MedicalAction.apply(state, medical.medicalType(), part)) {
            player.displayClientMessage(Component.translatable("message.traumacraft.not_applicable"), true);
            return;
        }
        if (!player.getAbilities().instabuild) {
            cursor.shrink(1);
            player.containerMenu.setCarried(cursor);
            player.containerMenu.broadcastChanges();
        }
        player.displayClientMessage(Component.translatable("message.traumacraft.treated", part.label), true);
        ModNetwork.sync(player);
    }
}
