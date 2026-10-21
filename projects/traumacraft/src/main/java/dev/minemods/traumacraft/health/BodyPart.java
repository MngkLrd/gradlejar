package dev.minemods.traumacraft.health;

import java.util.Locale;

/** Static anatomical zones. Minecraft does not expose these hitboxes natively. */
public enum BodyPart {
    HEAD(35, "Head"),
    CHEST(85, "Chest"),
    STOMACH(70, "Stomach"),
    LEFT_ARM(60, "Left arm"),
    RIGHT_ARM(60, "Right arm"),
    LEFT_LEG(65, "Left leg"),
    RIGHT_LEG(65, "Right leg");

    public final int maximum;
    public final String label;

    BodyPart(int maximum, String label) {
        this.maximum = maximum;
        this.label = label;
    }

    public static BodyPart parse(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isLeg() {
        return this == LEFT_LEG || this == RIGHT_LEG;
    }
}
