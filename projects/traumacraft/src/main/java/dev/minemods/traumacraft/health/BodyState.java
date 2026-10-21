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
