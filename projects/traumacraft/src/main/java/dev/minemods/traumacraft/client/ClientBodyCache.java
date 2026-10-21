package dev.minemods.traumacraft.client;

import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.network.MedicalNetworking;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientBodyCache {
    private static boolean synced;
    private static final float[] HP = new float[BodyPart.values().length];
    private static final byte[] BLEED = new byte[BodyPart.values().length];
    private static final boolean[] FRACTURE = new boolean[BodyPart.values().length];

    private ClientBodyCache() {}
    public static void accept(MedicalNetworking.BodySync packet) {
        for (BodyPart zone : BodyPart.values()) {
            int id = zone.ordinal();
            HP[id] = Math.max(0, Math.min(zone.maximum, packet.hp()[id]));
            BLEED[id] = packet.bleeding()[id];
            FRACTURE[id] = packet.fractured()[id];
        }
        synced = true;
    }
    public static boolean ready() { return synced; }
    public static float hp(BodyPart part) { return synced ? HP[part.ordinal()] : part.maximum; }
    public static int bleed(BodyPart part) { return BLEED[part.ordinal()]; }
    public static boolean broken(BodyPart part) { return FRACTURE[part.ordinal()]; }
    public static float percent(BodyPart part) { return hp(part) / part.maximum; }
    public static void clear() { synced = false; }
}
