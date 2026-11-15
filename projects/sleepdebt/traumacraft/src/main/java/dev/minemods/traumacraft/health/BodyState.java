package dev.minemods.traumacraft.health;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Authoritative player medical state. Saved to player attachment NBT on server. */
public final class BodyState implements INBTSerializable<CompoundTag> {
    private final float[] health = new float[BodyPart.values().length];
    private final byte[] bleeding = new byte[BodyPart.values().length];
    private final boolean[] fractured = new boolean[BodyPart.values().length];
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
        if (criticalGraceTicks % 20 == 0) dirty = true;
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
        if (hasFracturedLeg()) return 0.42f;
        float leg = Math.min(percent(BodyPart.LEFT_LEG), percent(BodyPart.RIGHT_LEG));
        return leg < 0.20f ? 0.32f : leg < 0.50f ? 0.16f : 0f;
    }
    public void damage(BodyPart part, float value) {
        if (value <= 0) return;
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
        dirty = true;
    }
    public boolean heal(BodyPart part, float amount) {
        if (amount <= 0 || health(part) >= part.maximum) return false;
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
        int i = target.ordinal();
        health[i] = Math.min(target.maximum, health[i] + amount);
        dirty = true;
        return true;
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
        for (BodyPart part : BodyPart.values()) if (fractured(part)) return part;
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
        }
        dirty = true;
    }
    public boolean takeDirty() {
        boolean result = dirty;
        dirty = false;
        return result;
    }
    @Override
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
            result.put(part.name(), zone);
        }
        return result;
    }
    @Override
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
            health[i] = Math.max(0, Math.min(part.maximum, zone.getFloat("hp")));
            bleeding[i] = (byte) Math.max(0, Math.min(2, zone.getByte("bleed")));
            fractured[i] = zone.getBoolean("fracture");
        }
        dirty = true;
    }
}
