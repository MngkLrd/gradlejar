package dev.minemods.traumacraft.health;

import dev.minemods.traumacraft.item.MedicalItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Pure medical addon. Combat, damage calculations, armour and trauma creation are untouched.
 * All methods that change inventory/state are called on the logical server.
 */
public final class MedicalAction {
    private MedicalAction() {}

    private static boolean anyRecoverable(BodyState state) {
        for (BodyPart zone : BodyPart.values()) {
            if (state.health(zone) < zone.maximum || state.bleeding(zone) > 0) return true;
        }
        return false;
    }

    public static boolean canApply(BodyState state, MedicalItem.Type type, BodyPart part) {
        return switch (type) {
            case BANDAGE, COMPRESSION_WRAP -> state.bleeding(part) > 0;
            case HEMOSTATIC_GEL, TOURNIQUET -> state.bleeding(part) == 2;
            case SPLINT, FRACTURE_BRACE, BONE_MENDER -> state.fractured(part);
            case MEDKIT, FIELD_MEDKIT -> state.health(part) < part.maximum;
            case RAG_DRESSING, STERILE_PATCH, COMPACT_MEDKIT ->
                    state.health(part) > 0 && state.health(part) < part.maximum;
            case TRAUMA_KIT ->
                    state.health(part) < part.maximum || state.bleeding(part) > 0 || state.fractured(part);
            case SURGICAL_KIT -> state.health(part) == 0;
            case RECOVERY_SERUM -> anyRecoverable(state);
            case ADRENALINE_INJECTOR, GUARDIAN_INJECTOR, FOCUS_INJECTOR, ENDURANCE_INJECTOR, ANALGESIC_TABLETS -> true;
        };
    }

    public static BodyPart autoTarget(BodyState state, MedicalItem.Type type) {
        return switch (type) {
            case BANDAGE, COMPRESSION_WRAP -> state.mostBleeding();
            case HEMOSTATIC_GEL, TOURNIQUET -> {
                BodyPart target = null;
                for (BodyPart zone : BodyPart.values()) if (state.bleeding(zone) == 2) {
                    target = zone; break;
                }
                yield target;
            }
            case SPLINT, FRACTURE_BRACE, BONE_MENDER -> state.firstFracture();
            case SURGICAL_KIT -> {
                BodyPart target = null;
                for (BodyPart zone : BodyPart.values()) if (state.health(zone) == 0) {
                    target = zone; break;
                }
                yield target;
            }
            case RAG_DRESSING, STERILE_PATCH, COMPACT_MEDKIT -> {
                BodyPart result = null;
                float lowest = 1f;
                for (BodyPart zone : BodyPart.values()) {
                    if (state.health(zone) > 0 && state.percent(zone) < lowest) {
                        lowest = state.percent(zone); result = zone;
                    }
                }
                yield result;
            }
            case TRAUMA_KIT -> {
                BodyPart result = state.mostDamaged();
                if (result == null) result = state.mostBleeding();
                if (result == null) result = state.firstFracture();
                yield result;
            }
            case MEDKIT, FIELD_MEDKIT -> state.mostDamaged();
            case RECOVERY_SERUM, ADRENALINE_INJECTOR, GUARDIAN_INJECTOR,
                 FOCUS_INJECTOR, ENDURANCE_INJECTOR, ANALGESIC_TABLETS -> BodyPart.CHEST;
        };
    }

    /** The original three treatments retain their previous behaviour. */
    public static boolean apply(Player player, BodyState state, MedicalItem.Type type, BodyPart part) {
        if (!canApply(state, type, part)) return false;
        switch (type) {
            case BANDAGE -> state.stopBleeding(part);
            case SPLINT -> state.setFractured(part, false);
            case MEDKIT -> state.heal(part, 25);

            case RAG_DRESSING -> state.heal(part, 4);
            case STERILE_PATCH -> state.heal(part, 10);
            case COMPRESSION_WRAP -> {
                state.stopBleeding(part);
                state.heal(part, 5);
            }
            case HEMOSTATIC_GEL -> {
                state.stopBleeding(part);
                state.heal(part, 15);
            }
            case TOURNIQUET -> state.stopBleeding(part);
            case FRACTURE_BRACE -> {
                state.setFractured(part, false);
                state.heal(part, 10);
            }
            case BONE_MENDER -> {
                state.setFractured(part, false);
                state.heal(part, 25);
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
            }
            case COMPACT_MEDKIT -> state.heal(part, 18);
            case FIELD_MEDKIT -> {
                state.heal(part, 42);
                state.stopBleeding(part);
            }
            case TRAUMA_KIT -> {
                state.heal(part, 60);
                state.stopBleeding(part);
                state.setFractured(part, false);
            }
            case SURGICAL_KIT -> {
                state.heal(part, 35);
                state.stopBleeding(part);
                state.setFractured(part, false);
            }
            case RECOVERY_SERUM -> {
                for (BodyPart zone : BodyPart.values()) state.heal(zone, 8);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0));
            }
            case ADRENALINE_INJECTOR -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
            }
            case GUARDIAN_INJECTOR -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 800, 0));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 800, 1));
            }
            case FOCUS_INJECTOR -> {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 1800, 1));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0));
            }
            case ENDURANCE_INJECTOR -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 1200, 1));
            }
            case ANALGESIC_TABLETS -> {
                state.setPainkillerTicks(2400);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
            }
        }
        return true;
    }
}
