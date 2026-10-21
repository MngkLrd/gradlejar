package dev.minemods.traumacraft.health;

import dev.minemods.traumacraft.item.MedicalItem;

public final class MedicalAction {
    private MedicalAction() {}
    public static boolean canApply(BodyState state, MedicalItem.Type type, BodyPart part) {
        return switch (type) {
            case BANDAGE -> state.bleeding(part) > 0;
            case SPLINT -> state.fractured(part);
            case MEDKIT -> state.health(part) < part.maximum;
        };
    }
    public static boolean apply(BodyState state, MedicalItem.Type type, BodyPart part) {
        if (!canApply(state, type, part)) return false;
        switch (type) {
            case BANDAGE -> state.stopBleeding(part);
            case SPLINT -> state.setFractured(part, false);
            case MEDKIT -> state.heal(part, 25);
        }
        return true;
    }
}
