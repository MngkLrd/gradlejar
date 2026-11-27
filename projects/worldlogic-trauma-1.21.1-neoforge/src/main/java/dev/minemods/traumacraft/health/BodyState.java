package dev.minemods.traumacraft.health;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/** Authoritative player medical state. Saved to player attachment NBT on server. */
public final class BodyState {
    private final float[] health = new float[BodyPart.values().length];
    private final byte[] bleeding = new byte[BodyPart.values().length];
    private final boolean[] fractured = new boolean[BodyPart.values().length];
    private final int[] boneRecovery = new int[BodyPart.values().length];
    private boolean dirty;
    private int concussionTicks;
    private int painkillerTicks;
    // Stored across relogs: no reset of the emergency treatment window by reconnecting.
    private int criticalGraceTicks;
    private boolean vitalityLinked; 
    public int concussionTicks() { return concussionTicks; }
    public int painkillerTicks() { return painkillerTicks; }

    /** Seconds are displayed in the UI; internal countdown is in server ticks. */
    public int criticalGraceTicks() { return criticalGraceTicks; }

    public boolean vitalDestroyed() {
        return health(BodyPart.HEAD) <= 0f || health(BodyPart.CHEST) <= 0f;
    }

    /** Called once when bleeding, rather than an external hit, blacks a vital zone. */
    public void startCriticalGrace(int ticks) {
        if (criticalGraceTicks > 0 || !vitalDestroyed()) return;
        criticalGraceTicks = Math.max(1, ticks);
        dirty = true;
    }

    /** Returns true only once, when the final critical tick expires. */
    public boolean tickCriticalGrace() {
        if (criticalGraceTicks <= 0) return false;
        criticalGraceTicks--;
        dirty = true; // Persist the precise remaining grace on logout, including sub-second ticks.
        return criticalGraceTicks == 0;
    }
    public void addConcussion(int ticks) {
        concussionTicks = Math.max(concussionTicks, ticks);
        dirty = true;
    }
    public void setPainkillerTicks(int ticks) {
        painkillerTicks = Math.max(0, ticks);
        dirty = true;
    }
    /** Advance temporary screen effects in one-second increments. */
    public void tickMedicalEffects() {
        if (concussionTicks > 0) { concussionTicks = Math.max(0, concussionTicks - 20); dirty = true; }
        if (painkillerTicks > 0) { painkillerTicks = Math.max(0, painkillerTicks - 20); dirty = true; }
    }
    public boolean isVitalityLinked() { return vitalityLinked; }
    public void markVitalityLinked() { vitalityLinked = true; dirty = true; }

    public BodyState() { reset(); }

    public float health(BodyPart part) { return health[part.ordinal()]; }
    public int bleeding(BodyPart part) { return bleeding[part.ordinal()]; }
    public boolean fractured(BodyPart part) { return fractured[part.ordinal()]; }
    public float percent(BodyPart part) { return health(part) / part.maximum; }

    public boolean hasBleeding() {
        for (byte value : bleeding) if (value > 0) return true;
        return false;
    }
    public boolean hasInjury() {
        for (BodyPart part : BodyPart.values()) {
            if (health(part) < part.maximum || bleeding(part) > 0 || fractured(part)) return true;
        }
        return false;
    }
    public boolean hasFracturedLeg() {
        return fractured(BodyPart.LEFT_LEG) || fractured(BodyPart.RIGHT_LEG);
    }
    public float legPenalty() {
        float fracturePenalty = 0f;
        if (hasFracturedLeg()) {
            fracturePenalty = ((fractured(BodyPart.LEFT_LEG) && !stabilized(BodyPart.LEFT_LEG)) ||
                (fractured(BodyPart.RIGHT_LEG) && !stabilized(BodyPart.RIGHT_LEG))) ? .42f : .22f;
        }
        float leg = Math.min(percent(BodyPart.LEFT_LEG), percent(BodyPart.RIGHT_LEG));
        return Math.max(fracturePenalty, leg < 0.20f ? 0.32f : leg < 0.50f ? 0.16f : 0f);
    }
    public void damage(BodyPart part, float value) {
        if (!Float.isFinite(value) || value <= 0) return;
        int i = part.ordinal();
        health[i] = Math.max(0, health[i] - value);
        dirty = true;
    }
    public void setBleeding(BodyPart part, int severity) {
        int i = part.ordinal();
        byte updated = (byte) Math.min(2, Math.max(bleeding[i], severity));
        if (bleeding[i] != updated) { bleeding[i] = updated; dirty = true; }
    }
    public void stopBleeding(BodyPart part) {
        bleeding[part.ordinal()] = 0;
        dirty = true;
    }
    public void setFractured(BodyPart part, boolean value) {
        fractured[part.ordinal()] = value;
        boneRecovery[part.ordinal()] = 0;
        dirty = true;
    }
    public boolean stabilized(BodyPart part) { return fractured(part) && boneRecovery[part.ordinal()] > 0; }
    public boolean stabilize(BodyPart part, int recoveryTicks) {
        if (!fractured(part) || stabilized(part)) return false;
        boneRecovery[part.ordinal()] = Math.max(20, recoveryTicks);
        dirty = true;
        return true;
    }
    public boolean armImpaired() {
        return fractured(BodyPart.LEFT_ARM) || fractured(BodyPart.RIGHT_ARM)
            || percent(BodyPart.LEFT_ARM) < .70f || percent(BodyPart.RIGHT_ARM) < .70f;
    }
    public void recoverBones(int ticks) {
        if (ticks <= 0) return;
        for (BodyPart part : BodyPart.values()) {
            int i = part.ordinal();
            if (boneRecovery[i] > 0 && fractured[i]) {
                boneRecovery[i] = Math.max(0, boneRecovery[i] - ticks);
                if (boneRecovery[i] == 0) fractured[i] = false;
                dirty = true;
            }
        }
    }
    public boolean heal(BodyPart part, float amount) {
        if (!Float.isFinite(amount) || amount <= 0 || health(part) >= part.maximum) return false;
        int i = part.ordinal();
        health[i] = Math.min(part.maximum, health[i] + amount);
        if (!vitalDestroyed() && criticalGraceTicks > 0) criticalGraceTicks = 0;
        dirty = true;
        return true;
    }
    public BodyPart mostDamaged() {
        BodyPart target = null;
        float lowest = 1.0f;
        for (BodyPart part : BodyPart.values()) {
            if (percent(part) < lowest) { target = part; lowest = percent(part); }
        }
        return target;
    }
    public boolean healMostDamaged(float amount) {
        BodyPart target = null;
        float worst = 1.0f;
        for (BodyPart part : BodyPart.values()) {
            float fraction = percent(part);
            if (fraction < worst) { target = part; worst = fraction; }
        }
        if (target == null) return false;
        return heal(target, amount);
    }
    public BodyPart mostBleeding() {
        BodyPart found = null;
        int max = 0;
        for (BodyPart part : BodyPart.values()) {
            if (bleeding(part) > max) { max = bleeding(part); found = part; }
        }
        return found;
    }
    public BodyPart firstFracture() {
        for (BodyPart part : BodyPart.values()) if (fractured(part) && !stabilized(part)) return part;
        return null;
    }
    public void reset() {
        vitalityLinked = false;
        concussionTicks = 0;
        painkillerTicks = 0;
        criticalGraceTicks = 0;
        for (BodyPart part : BodyPart.values()) {
            int i = part.ordinal();
            health[i] = part.maximum;
            bleeding[i] = 0;
            fractured[i] = false;
            boneRecovery[i] = 0;
        }
        dirty = true;
    }
    public boolean takeDirty() {
        boolean result = dirty;
        dirty = false;
        return result;
    }
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag result = new CompoundTag();
        result.putBoolean("vitalityLinked", vitalityLinked);
        result.putInt("concussionTicks", concussionTicks);
        result.putInt("painkillerTicks", painkillerTicks);
        result.putInt("criticalGraceTicks", criticalGraceTicks);
        for (BodyPart part : BodyPart.values()) {
            CompoundTag zone = new CompoundTag();
            zone.putFloat("hp", health(part));
            zone.putByte("bleed", bleeding[part.ordinal()]);
            zone.putBoolean("fracture", fractured(part));
            zone.putInt("boneRecovery", boneRecovery[part.ordinal()]);
            result.put(part.name(), zone);
        }
        return result;
    }
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        reset();
        vitalityLinked = nbt.getBoolean("vitalityLinked");
        concussionTicks = Math.max(0, nbt.getInt("concussionTicks"));
        painkillerTicks = Math.max(0, nbt.getInt("painkillerTicks"));
        criticalGraceTicks = Math.max(0, nbt.getInt("criticalGraceTicks"));
        for (BodyPart part : BodyPart.values()) {
            if (!nbt.contains(part.name(), 10)) continue;
            CompoundTag zone = nbt.getCompound(part.name());
            int i = part.ordinal();
            float stored = zone.contains("hp", 99) ? zone.getFloat("hp") : part.maximum;
            health[i] = Float.isFinite(stored) ? Math.max(0, Math.min(part.maximum, stored)) : part.maximum;
            bleeding[i] = (byte) Math.max(0, Math.min(2, zone.getByte("bleed")));
            fractured[i] = zone.getBoolean("fracture");
            boneRecovery[i] = fractured[i] ? Math.max(0, Math.min(12000, zone.getInt("boneRecovery"))) : 0;
        }
        dirty = true;
    }
}
