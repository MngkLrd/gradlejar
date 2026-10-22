package dev.minemods.traumacraft.event;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.combat.HitLocation;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.registry.ModAttachments;
import dev.minemods.traumacraft.network.ModNetwork;
import dev.minemods.traumacraft.health.VitalityBridge;
import dev.minemods.traumacraft.medical.TreatmentManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class TraumaEvents {
    private static final ResourceLocation LIMPING_ID =
            ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID, "limping");
    private static final ThreadLocal<Boolean> BLEEDING_DAMAGE = ThreadLocal.withInitial(() -> false);

    private TraumaEvents() {}

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ModNetwork.sync(player);
    }
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) TreatmentManager.forget(player);
    }
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { TreatmentManager.forget(player); ModNetwork.sync(player); }
    }

    public static void onDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer victim) || BLEEDING_DAMAGE.get()) return;
        float amount = event.getNewDamage();
        TreatmentManager.cancel(victim);
        if (amount <= 0) return;
        BodyState state = victim.getData(ModAttachments.BODY.get());

        boolean blunt = event.getSource().is(DamageTypeTags.IS_FALL)
                || event.getSource().is(DamageTypeTags.IS_EXPLOSION);
        boolean thermal = event.getSource().is(DamageTypeTags.IS_FIRE);
        if (event.getSource().is(DamageTypeTags.IS_FALL)) {
            apply(state, victim, BodyPart.LEFT_LEG, amount * 5.0f, true, false);
            apply(state, victim, BodyPart.RIGHT_LEG, amount * 5.0f, true, false);
        } else if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            for (BodyPart part : BodyPart.values()) {
                float factor = part == BodyPart.CHEST ? 1.5f : part == BodyPart.HEAD ? 0.7f : 0.6f;
                apply(state, victim, part, amount * factor, false, true);
            }
        } else {
            BodyPart impacted = thermal ? BodyPart.CHEST : HitLocation.locate(victim, event.getSource());
            apply(state, victim, impacted, amount * 8.0f, false, blunt || thermal);
        }

        if (state.health(BodyPart.HEAD) <= 0 || state.health(BodyPart.CHEST) <= 0) {
            event.setNewDamage(Math.max(amount, victim.getHealth() + victim.getAbsorptionAmount() + 1f));
        }
    }

    private static void apply(BodyState state, ServerPlayer player, BodyPart part,
                              float points, boolean falling, boolean blunt) {
        state.damage(part, points);
        if (falling && part.isLeg() && points >= 17.0f) {
            state.setFractured(part, true);
        } else if (!falling && blunt && part.isLeg() && points >= 22f && player.getRandom().nextFloat() < 0.4f) {
            state.setFractured(part, true);
        }
        if (!blunt && !falling && points >= 12) {
            float roll = player.getRandom().nextFloat();
            if (points >= 24 && roll < 0.35f) state.setBleeding(part, 2);
            else if (roll < 0.60f) state.setBleeding(part, 1);
        }
    }

    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TreatmentManager.tick(player);
        VitalityBridge.synchronize(player);
        if (player.tickCount % 20 != 0) return;
        BodyState state = player.getData(ModAttachments.BODY.get());
        updateMovement(player, state.legPenalty());

        if (player.tickCount % 40 == 0 && state.hasBleeding()) {
            int severity = 0;
            for (BodyPart part : BodyPart.values()) severity += state.bleeding(part);
            float bloodLoss = Math.min(3.0f, severity * 0.5f);
            // Blood loss damages the same body regions so vanilla hearts cannot
            // regenerate independently of the localized wounds.
            if (severity > 0) for (BodyPart part : BodyPart.values()) {
                if (state.bleeding(part) > 0) {
                    state.damage(part, 22f * bloodLoss * state.bleeding(part) / severity);
                }
            }
            try {
                BLEEDING_DAMAGE.set(true);
                if (state.health(BodyPart.HEAD) <= 0 || state.health(BodyPart.CHEST) <= 0) {
                    player.hurt(player.damageSources().magic(), Math.max(bloodLoss, player.getHealth() + player.getAbsorptionAmount() + 1f));
                } else {
                    player.hurt(player.damageSources().magic(), bloodLoss);
                }
            } finally {
                BLEEDING_DAMAGE.set(false);
            }
        }
        if (state.takeDirty() || player.tickCount % 40 == 0) {
            ModNetwork.sync(player);
        }
    }

    public static void updateMovement(ServerPlayer player, float penalty) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        var old = attr.getModifier(LIMPING_ID);
        if (penalty == 0f) {
            if (old != null) attr.removeModifier(LIMPING_ID);
        } else if (old == null || Math.abs(old.amount() + penalty) > 0.0001) {
            attr.addOrUpdateTransientModifier(new AttributeModifier(
                    LIMPING_ID, -penalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

}
