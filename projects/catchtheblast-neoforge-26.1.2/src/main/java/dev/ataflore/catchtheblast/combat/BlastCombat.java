package dev.ataflore.catchtheblast.combat;

import dev.ataflore.catchtheblast.item.BlastShieldItem;
import dev.ataflore.catchtheblast.item.Charge;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.Entity;
import java.util.WeakHashMap;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

public final class BlastCombat {
    private BlastCombat() {}
    private static final WeakHashMap<Player,UUID> LAST_FIREBALL = new WeakHashMap<>();
    public static void onBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player player) ||
            !(player.level() instanceof ServerLevel level)) return;
        var source = event.getDamageSource();
        Entity direct = source.getDirectEntity();
        boolean fireball = direct instanceof LargeFireball || (source.getEntity() instanceof Ghast &&
                (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_PROJECTILE)));
        if (!fireball && !source.is(DamageTypeTags.IS_EXPLOSION)) return;
        ItemStack shield = player.getUseItem();
        if (!(shield.getItem() instanceof BlastShieldItem) || !player.isBlocking()) return;
        // A fireball can impact at the exact shield position, where vanilla's positional
        // test degenerates. Use its incoming velocity as a fallback, never block from behind.
        if (!event.getBlocked() || !event.getOriginalBlock()) {
            if (!fireball || !(direct instanceof LargeFireball ball)) return;
            Vec3 incoming = ball.getDeltaMovement().scale(-1);
            Vec3 facing = player.getLookAngle();
            if (incoming.lengthSqr() < 0.0001 || incoming.normalize().dot(facing) <= 0.10) return;
            event.setBlocked(true);
            event.setBlockedDamage(event.getOriginalBlockedDamage());
        }
        if (event.getBlockedDamage() <= 0) return;
        // One charge per projectile; its contact hit and explosion can share a tick.
        if (direct instanceof LargeFireball ball) {
            if(ball.getUUID().equals(LAST_FIREBALL.get(player)))return;
            LAST_FIREBALL.put(player,ball.getUUID());
        }
        int before = Charge.get(shield);
        boolean perfect = player.getTicksUsingItem() <= 15;
        int gained = Math.max(5, Math.min(120, Math.round(event.getBlockedDamage() * (perfect ? 3.6f : 3.0f))));
        if (fireball) gained = Math.max(20, gained);
        int after = Math.min(Charge.MAX, before + gained);
        long now = level.getServer().overworld().getGameTime();
        long deadline = after > 100 ? (before > 100 && Charge.deadline(shield) > 0 ? Charge.deadline(shield) : now + 70) : 0;
        Charge.set(shield, after, deadline);
        BlastFx.capture(level, player, after);
    }

    public static void release(ServerLevel level, Player player, InteractionHand hand, ItemStack stack) {
        int charge = Charge.get(stack);
        if (charge <= 0) return;
        Charge.set(stack, 0, 0);
        player.stopUsingItem();
        float strength = Math.min(1.5f, charge / 100f);
        double range = 3.0 + 5.0 * strength;
        Vec3 origin = player.getEyePosition().add(0, -0.45, 0);
        Vec3 dir = player.getLookAngle().normalize();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), e -> e != player && e.isAlive() && !e.isSpectator())) {
            if (player.isAlliedTo(target) || (target instanceof Player other && !player.canHarmPlayer(other))) continue;
            Vec3 delta = target.getBoundingBox().getCenter().subtract(origin);
            double distance = delta.length();
            if (distance < 0.01 || distance > range || dir.dot(delta.normalize()) < 0.67 || !player.hasLineOfSight(target)) continue;
            float falloff = (float)(1.0 - 0.45 * distance / range);
            if (target.hurtServer(level, player.damageSources().playerAttack(player), (2.0f + 9.0f * strength) * falloff)) {
                Vec3 away = delta.normalize();
                target.knockback((0.7 + 2.3 * strength) * falloff, -away.x, -away.z);
            }
        }
        BlastFx.release(level, player, dir, range, strength);
        if (player instanceof ServerPlayer serverPlayer) TravellingWave.launch(level, serverPlayer, origin, dir, range, strength);
        player.getCooldowns().addCooldown(stack, 20 + Math.round(20 * strength));
        stack.hurtAndBreak(Math.max(1, Math.round(charge / 18f)), player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    public static void tickOverload(ServerLevel level, Player player, ItemStack stack) {
        long now = level.getServer().overworld().getGameTime();
        long deadline = Charge.deadline(stack);
        if (deadline <= 0) { Charge.set(stack, Charge.get(stack), now + 70); return; }
        boolean held = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (deadline > now) {
            if (held && now % 7 == 0) BlastFx.warning(level, player);
            return;
        }
        Charge.set(stack, 0, 0);
        // Clear before damage to avoid reentrant event interactions.
        player.hurtServer(level, player.damageSources().magic(), 5.0f);
        if (held) stack.hurtAndBreak(16, player, player.getMainHandItem() == stack ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        else stack.setDamageValue(Math.min(stack.getMaxDamage()-1, stack.getDamageValue()+16));
        player.getCooldowns().addCooldown(stack, 60);
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY()+1, player.getZ(), 1, 0,0,0,0);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.1f, 0.65f);
    }
}
