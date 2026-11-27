package dev.minemods.thermal.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class ThermalSync {
    public record Snapshot(int entityId, boolean active, double body, double air, double felt,
                           double trend, double wet, double insulation, double radiant, boolean sheltered)
            implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("worldlogic_thermal", "state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = new StreamCodec<>() {
            @Override public Snapshot decode(RegistryFriendlyByteBuf b) {
                return new Snapshot(b.readVarInt(), b.readBoolean(), b.readDouble(), b.readDouble(), b.readDouble(),
                    b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readBoolean());
            }
            @Override public void encode(RegistryFriendlyByteBuf b, Snapshot s) {
                b.writeVarInt(s.entityId); b.writeBoolean(s.active); b.writeDouble(s.body); b.writeDouble(s.air);
                b.writeDouble(s.felt); b.writeDouble(s.trend); b.writeDouble(s.wet); b.writeDouble(s.insulation);
                b.writeDouble(s.radiant); b.writeBoolean(s.sheltered);
            }
        };
        @Override public Type<Snapshot> type() { return TYPE; }
    }
    private record Received(Snapshot snapshot, long time) {}
    private static volatile Received latest;
    private ThermalSync() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Snapshot.TYPE, Snapshot.CODEC,
            (s, context) -> latest = new Received(s, System.nanoTime()));
    }
    public static void send(ServerPlayer p, Snapshot s) {
        if(!(p instanceof net.neoforged.neoforge.common.util.FakePlayer)) PacketDistributor.sendToPlayer(p,s);
    }
    public static Snapshot current(int id) {
        Received r=latest; return r!=null && r.snapshot.entityId()==id && System.nanoTime()-r.time<3_000_000_000L ? r.snapshot : null;
    }
    public static void clear() { latest=null; }
}
