package dev.quickstacknearby.network;

import dev.quickstacknearby.QuickStackNearby;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuickStackRequest() implements CustomPacketPayload {
    public static final QuickStackRequest INSTANCE = new QuickStackRequest();
    public static final Type<QuickStackRequest> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(QuickStackNearby.MOD_ID, "request")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, QuickStackRequest> CODEC = StreamCodec.unit(INSTANCE);
    @Override public Type<QuickStackRequest> type() { return TYPE; }
}
