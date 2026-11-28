package dev.minemods.traumacraft.health;

/** Descriptive injury flags; fractures and concussion retain existing independent state. */
public enum WoundKind {
    CONTUSION(1), CUT(2), PUNCTURE(4), LACERATION(8), BURN(16), FROSTBITE(32);
    public final int bit;
    WoundKind(int bit){this.bit=bit;}
    public static final int ALL=63;
}
