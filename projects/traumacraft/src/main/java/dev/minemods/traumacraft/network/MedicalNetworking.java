package dev.minemods.traumacraft.network;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.client.ClientBodyCache;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.item.MedicineLogic;
import dev.minemods.traumacraft.registry.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class MedicalNetworking {
    private MedicalNetworking() {}

    public record BodySync(float[] hp, byte[] bleeding, boolean[] fractured) implements CustomPacketPayload {
        public static final Type<BodySync> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID, "body_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BodySync> CODEC = StreamCodec.of(
                (buf, msg) -> {
                    for (int i = 0; i < BodyPart.values().length; i++) {
                        buf.writeFloat(msg.hp[i]);
                        buf.writeByte(msg.bleeding[i]);
                        buf.writeBoolean(msg.fractured[i]);
                    }
                },
                buf -> {
                    int n = BodyPart.values().length;
                    float[] hp = new float[n];
                    byte[] bleeding = new byte[n];
                    boolean[] fractured = new boolean[n];
                    for (int i = 0; i < n; i++) {
                        hp[i] = buf.readFloat();
                        bleeding[i] = buf.readByte();
                        fractured[i] = buf.readBoolean();
                    }
                    return new BodySync(hp, bleeding, fractured);
                });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
        public static BodySync from(BodyState body) {
            int n = BodyPart.values().length;
            float[] hp = new float[n];
            byte[] bleed = new byte[n];
            boolean[] broken = new boolean[n];
            for (BodyPart part : BodyPart.values()) {
                int i = part.ordinal();
                hp[i] = body.health(part);
                bleed[i] = (byte) body.bleeding(part);
                broken[i] = body.fractured(part);
            }
            return new BodySync(hp, bleed, broken);
        }
    }

    public record TreatPart(byte id) implements CustomPacketPayload {
        public static final Type<TreatPart> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID, "treat_part"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TreatPart> CODEC = StreamCodec.of(
                (buf, msg) -> buf.writeByte(msg.id),
                buf -> new TreatPart(buf.readByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(BodySync.TYPE, BodySync.CODEC,
                (message, context) -> context.enqueueWork(() -> ClientBodyCache.accept(message)));
        registrar.playToServer(TreatPart.TYPE, TreatPart.CODEC,
                (message, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    int index = message.id;
                    if (index < 0 || index >= BodyPart.values().length) return;
                    // A cursor-carried item exists only while the personal inventory is open.
                    if (player.containerMenu != player.inventoryMenu) return;
                    ItemStack carried = player.containerMenu.getCarried();
                    BodyPart part = BodyPart.values()[index];
                    if (MedicineLogic.treat(player, carried, part)) {
                        player.containerMenu.broadcastChanges();
                        sync(player);
                    }
                }));
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, BodySync.from(player.getData(ModAttachments.BODY.get())));
    }
    public static void treat(BodyPart part) {
        PacketDistributor.sendToServer(new TreatPart((byte) part.ordinal()));
    }
}
