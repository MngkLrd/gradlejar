package dev.minemods.traumacraft.client;

import net.minecraft.client.Minecraft;
import dev.minemods.traumacraft.network.TreatmentProgressPacket;

/** Client only: visual projection of the server-owned treatment timer. */
public final class ClientTreatmentState {
    private static int startTick;
    private static int duration;
    private ClientTreatmentState() {}
    public static void accept(TreatmentProgressPacket p) {
        duration = Math.max(0, p.durationTicks());
        Minecraft mc = Minecraft.getInstance();
        startTick = (mc.player == null ? 0 : mc.player.tickCount) - p.elapsedTicks();
    }
    public static boolean active() { return duration > 0; }
    public static int remaining() {
        Minecraft mc = Minecraft.getInstance();
        int tick = mc.player == null ? startTick : mc.player.tickCount;
        return Math.max(0, duration - Math.max(0, tick - startTick));
    }
    public static float fraction() {
        return duration == 0 ? 0f : 1f - remaining() / (float) duration;
    }
}
