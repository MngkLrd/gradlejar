package com.kirillmods.shieldbashlite;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side only. Triggered after a genuine shield block, never by merely
 * holding the shield. No client-only references or custom sound playback.
 */
public final class ShieldBashLogic {
    public static final int PERFECT_WINDOW_TICKS = 20;
    public static final int STUN_TICKS = 40;
    public static final int COOLDOWN_TICKS = 30;
    public static final double KNOCKBACK = 0.175D;

    // Keys are weak so a disconnected player cannot be retained forever.
    private static final Map<ServerPlayer, Long> NEXT_BASH_TICK = new WeakHashMap<>();

    private ShieldBashLogic() {}

    public static void onBlockedMelee(ServerPlayer player, DamageSource source) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!player.isUsingItem() || !player.getUseItem().is(Items.SHIELD)) return;
        int heldTicks = player.getTicksUsingItem();
        if (heldTicks < 0 || heldTicks > PERFECT_WINDOW_TICKS) return;

        Entity direct = source.getDirectEntity();
        Entity owner = source.getEntity();
        if (!(owner instanceof Mob attacker) || direct != attacker) return;
        // Covers skeletons, pillagers, witches, drowned, wither, and modded
        // mobs implementing the vanilla ranged-attack contract.
        if (attacker instanceof RangedAttackMob || direct instanceof Projectile) return;

        long now = level.getGameTime();
        Long readyAt = NEXT_BASH_TICK.get(player);
        if (readyAt != null && now < readyAt) return;

        NEXT_BASH_TICK.put(player, now + COOLDOWN_TICKS);
        attacker.getNavigation().stop();

        Vec3 velocity = attacker.getDeltaMovement();
        attacker.setDeltaMovement(0.0D, Math.min(velocity.y, 0.05D), 0.0D);
        attacker.knockback(KNOCKBACK, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());

        attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, STUN_TICKS, 7, false, false, false));
        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STUN_TICKS, 0, false, false, false));

        level.sendParticles(ParticleTypes.CLOUD, attacker.getX(),
                attacker.getY() + attacker.getBbHeight() * 0.55D, attacker.getZ(),
                24, 0.35D, 0.25D, 0.35D, 0.015D);
    }
}
