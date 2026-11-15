package dev.minemods.blowpipes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Single server-authoritative projectile class shared by all six dart types.
 * Effects are applied only after successful damage. Sleeping deliberately
 * disables regular mob AI; a movement slow alone cannot stop skeleton arrows.
 */
public final class DartProjectile extends ThrowableItemProjectile {
    private int tier;

    public DartProjectile(EntityType<? extends DartProjectile> type, Level level) {
        super(type, level);
    }

    public void setPipeTier(int value) {
        tier = Math.max(0, Math.min(2, value));
    }

    @Override
    protected Item getDefaultItem() {
        return Blowpipes.DEFAULT_DART();
    }

    private String getDartKind() {
        ItemStack ammo = getItem();
        ResourceLocation name = BuiltInRegistries.ITEM.getKey(ammo.getItem());
        return Blowpipes.ID.equals(name.getNamespace()) ? name.getPath() : "plain_dart";
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide()) return;

        Entity target = hit.getEntity();
        String kind = getDartKind();
        float base = switch (kind) {
            case "iron_dart" -> 5.5f;
            case "fire_dart" -> 3.0f;
            case "plain_dart" -> 2.0f;
            case "poison_dart", "slowness_dart" -> 1.75f;
            case "sleep_dart" -> 0.75f;
            default -> 2.0f;
        };
        float weaponBonus = switch (tier) {
            case 1 -> 1.5f;
            case 2 -> 3.0f;
            default -> 0.0f;
        };
        if (!target.hurt(damageSources().thrown(this, getOwner()), base + weaponBonus)) return;

        float duration = switch (tier) {
            case 1 -> 1.4f;
            case 2 -> 1.8f;
            default -> 1.0f;
        };
        if ("fire_dart".equals(kind)) {
            target.igniteForSeconds(Math.round(6f * duration));
            return;
        }
        if (!(target instanceof LivingEntity living)) return;

        switch (kind) {
            case "poison_dart" ->
                living.addEffect(new MobEffectInstance(MobEffects.POISON,
                        Math.round(160 * duration), tier == 2 ? 1 : 0));
            case "slowness_dart" ->
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        Math.round(160 * duration), 2));
            case "sleep_dart" -> {
                if (living instanceof Mob mob) {
                    BlowpipeEvents.putToSleep(mob, Math.round(100 * duration));
                } else {
                    // Player and non-Mob entities are never placed into fake bed sleep.
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            50, 2));
                }
            }
            default -> { }
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) discard();
    }
}
