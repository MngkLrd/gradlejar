package dev.minemods.traumacraft.client;

import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.network.SyncBodyStatePacket;

public final class ClientBodyState {
    private static final float[] HP = new float[BodyPart.values().length];
    private static final byte[] BLEEDING = new byte[BodyPart.values().length];
    private static final boolean[] FRACTURE = new boolean[BodyPart.values().length];
    private static boolean received;
    private static int concussionTicks;
    private static int painkillerTicks;

    private ClientBodyState() {}
    public static void accept(SyncBodyStatePacket packet) {
        for (BodyPart part : BodyPart.values()) {
            int i = part.ordinal();
            HP[i] = Math.max(0f, Math.min(part.maximum, packet.hp()[i]));
            BLEEDING[i] = (byte)Math.max(0, Math.min(2, packet.bleeding()[i]));
            FRACTURE[i] = packet.fracture()[i];
        }
        concussionTicks = packet.concussion();
        painkillerTicks = packet.painkiller();
        received = true;
    }
    public static int concussionTicks() { return received ? concussionTicks : 0; }
    public static int painkillerTicks() { return received ? painkillerTicks : 0; }
    public static boolean hasReceived() { return received; }
    public static void clear() { received = false; concussionTicks = 0; painkillerTicks = 0; }
    public static float hp(BodyPart part) { return received ? HP[part.ordinal()] : part.maximum; }
    public static float fraction(BodyPart part) { return hp(part) / part.maximum; }
    public static int bleeding(BodyPart part) { return received ? BLEEDING[part.ordinal()] : 0; }
    public static boolean fractured(BodyPart part) { return received && FRACTURE[part.ordinal()]; }
    public static boolean injured(BodyPart part) {
        return hp(part) < part.maximum || fractured(part) || bleeding(part) > 0;
    }
    public static boolean hasAnyInjuries() {
        for (BodyPart part : BodyPart.values()) if (injured(part)) return true;
        return false;
    }
}
