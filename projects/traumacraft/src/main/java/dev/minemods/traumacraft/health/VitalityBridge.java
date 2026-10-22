package dev.minemods.traumacraft.health;

import dev.minemods.traumacraft.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

/**
 * Seven local HP pools are authoritative; vanilla hearts are their aggregate display.
 * Food regeneration is deliberately ~10 times slower. Vanilla effect healing
 * is routed into one injured region instead of independently restoring hearts.
 */
public final class VitalityBridge {
    private static final float FOOD_HEAL_FACTOR = 0.10f;
    private VitalityBridge() {}

    public static float targetHealth(BodyState state, float maximum) {
        float total = 0f;
        for (BodyPart part : BodyPart.values()) total += state.health(part);
        return Math.max(1.0f, Math.min(maximum, maximum * total / 440f));
    }

    public static void synchronize(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        BodyState body = player.getData(ModAttachments.BODY.get());
        if (!body.isVitalityLinked()) {
            // Migration: an existing player at low vanilla health is not magically
            // healed on first load. Distribute that deficit over the seven zones.
            float wanted = targetHealth(body, player.getMaxHealth());
            float vanilla = player.getHealth();
            if (vanilla > 0f && vanilla + 0.5f < wanted) {
                float factor = vanilla / wanted;
                for (BodyPart part : BodyPart.values()) {
                    float current = body.health(part);
                    float desired = current * factor;
                    if (part == BodyPart.HEAD || part == BodyPart.CHEST)
                        desired = Math.max(1.0f, desired);
                    body.damage(part, current - desired);
                }
            }
            body.markVitalityLinked();
        }
        float target = targetHealth(body, player.getMaxHealth());
        if (Math.abs(player.getHealth() - target) >= 0.08f) {
            player.setHealth(target);
        }
    }

    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getAmount() <= 0) return;
        BodyState body = player.getData(ModAttachments.BODY.get());
        // Regeneration from potions/Golden Apples is stronger, but still heals
        // the actual body. Passive hunger healing is a slow fallback.
        boolean enhanced = player.hasEffect(MobEffects.REGENERATION);
        float healedVanilla = event.getAmount() * (enhanced ? 1f : FOOD_HEAL_FACTOR);
        event.setAmount(healedVanilla);
        BodyPart part = null;
        float lowest = 1f;
        for (BodyPart zone : BodyPart.values()) {
            if (body.health(zone) > 0 && body.percent(zone) < lowest) {
                part = zone; lowest = body.percent(zone);
            }
        }
        if (part != null) {
            body.heal(part, healedVanilla * 22f);
        }
    }
}
