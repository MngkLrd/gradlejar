package dev.minemods.cowardlymobs.hunger.ai;

import dev.minemods.cowardlymobs.hunger.config.ZombieHungerConfig;
import dev.minemods.cowardlymobs.hunger.food.FoodTags;
import dev.minemods.cowardlymobs.hunger.food.FoodTags.FoodKind;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;

public final class ZombieFoodGoal extends Goal {
    private final Zombie zombie;
    private ItemEntity food;
    private int scanCooldown;
    private int chewingTicks;
    private int lastMealTick = -10000;
    private int walkingTicks;

    public ZombieFoodGoal(Zombie zombie) {
        this.zombie = zombie;
        this.scanCooldown = zombie.getRandom().nextInt(20);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(zombie.level() instanceof ServerLevel) || !zombie.isAlive()
                || HungerSignals.interrupted(zombie)
                || zombie.tickCount - lastMealTick < ZombieHungerConfig.EAT_COOLDOWN.get()) {
            return false;
        }
        if (scanCooldown-- > 0) return false;
        scanCooldown = ZombieHungerConfig.SCAN_TICKS.get() + zombie.getRandom().nextInt(5);

        int max = Math.max(ZombieHungerConfig.ROTTEN_RADIUS.get(),
                Math.max(ZombieHungerConfig.RAW_RADIUS.get(), ZombieHungerConfig.COOKED_RADIUS.get()));
        UUID alert = HungerSignals.alertedItem(zombie);
        // Neighbors that heard the signal can search beyond their normal visual range.
        if (alert != null) max = Math.max(max, 18);

        List<ItemEntity> candidates = zombie.level().getEntitiesOfClass(ItemEntity.class,
                zombie.getBoundingBox().inflate(max),
                item -> item.isAlive() && !item.getItem().isEmpty());
        food = candidates.stream()
                .filter(item -> score(item, alert) > Double.NEGATIVE_INFINITY)
                .max(Comparator.comparingDouble(item -> score(item, alert)))
                .orElse(null);
        return food != null;
    }

    private double score(ItemEntity item, UUID alert) {
        FoodKind kind = FoodTags.classify(item.getItem());
        if (kind == FoodKind.NONE) return Double.NEGATIVE_INFINITY;
        double distSq = zombie.distanceToSqr(item);
        int radius = switch (kind) {
            case ROTTEN -> ZombieHungerConfig.ROTTEN_RADIUS.get();
            case RAW -> ZombieHungerConfig.RAW_RADIUS.get();
            case COOKED -> ZombieHungerConfig.COOKED_RADIUS.get();
            default -> 0;
        };
        boolean signaled = alert != null && alert.equals(item.getUUID());
        if (!signaled && distSq > radius * radius) return Double.NEGATIVE_INFINITY;

        // Wounded vanilla zombies seek ordinary meat; rotten flesh distracts healthy ones.
        if (zombie.getTarget() != null && kind != FoodKind.ROTTEN) {
            if (zombie.getHealth() >= zombie.getMaxHealth() * 0.4f
                    || zombie.distanceToSqr(zombie.getTarget()) < 16.0) {
                return Double.NEGATIVE_INFINITY;
            }
        }
        if (zombie instanceof Husk) {
            // Husks are less easily distracted and ignore cooked meat entirely.
            if (kind == FoodKind.COOKED || distSq > 36.0) return Double.NEGATIVE_INFINITY;
            if (zombie.getTarget() != null && zombie.getHealth() >= zombie.getMaxHealth() * 0.5f) {
                return Double.NEGATIVE_INFINITY;
            }
        }
        double bonus = 0;
        if (zombie instanceof Drowned) {
            // Underwater food is far more interesting to a drowned than food on land.
            if (item.isInWaterOrBubble()) {
                bonus = 80;
            } else if (kind != FoodKind.ROTTEN || distSq > 36.0) {
                return Double.NEGATIVE_INFINITY;
            }
        }
        double base = switch (kind) {
            case ROTTEN -> 200;
            case RAW -> 95;
            case COOKED -> 45;
            default -> 0;
        };
        return base + bonus + (signaled ? 65 : 0) - Math.sqrt(distSq) * 4;
    }

    @Override
    public void start() {
        chewingTicks = 0;
        walkingTicks = 0;
        if (food == null || !zombie.getNavigation().moveTo(food, 1.0)) {
            food = null;
            scanCooldown = 40;
            return;
        }
        HungerSignals.alertNeighbors(zombie, food);
    }

    @Override
    public boolean canContinueToUse() {
        return food != null && food.isAlive() && !food.getItem().isEmpty()
                && !HungerSignals.interrupted(zombie) && zombie.isAlive()
                && walkingTicks < 240
                && zombie.tickCount - lastMealTick >= ZombieHungerConfig.EAT_COOLDOWN.get();
    }

    @Override
    public void tick() {
        if (food == null) return;
        walkingTicks++;
        zombie.getLookControl().setLookAt(food, 30f, 30f);
        if (zombie.distanceToSqr(food) > 3.24) {
            chewingTicks = 0;
            if (walkingTicks % 12 == 0) zombie.getNavigation().moveTo(food, 1.0);
            return;
        }
        zombie.getNavigation().stop();
        chewingTicks++;
        if (chewingTicks >= ZombieHungerConfig.EAT_TICKS.get()) {
            eatOneItem();
        }
    }

    private void eatOneItem() {
        if (food == null || !(zombie.level() instanceof ServerLevel server)) return;
        ItemStack stack = food.getItem();
        if (stack.isEmpty() || FoodTags.classify(stack) == FoodKind.NONE) return;
        ItemStack particleStack = stack.copyWithCount(1);
        stack.shrink(1);
        if (stack.isEmpty()) food.discard();
        else food.setItem(stack);

        zombie.heal(ZombieHungerConfig.HEAL_HP.get());
        zombie.playSound(SoundEvents.GENERIC_EAT, 0.9f, 0.8f + zombie.getRandom().nextFloat() * 0.3f);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, particleStack),
                zombie.getX(), zombie.getY() + zombie.getBbHeight() * 0.72, zombie.getZ(),
                6, 0.20, 0.15, 0.20, 0.035);
        lastMealTick = zombie.tickCount;
        food = null;
        zombie.getNavigation().stop();
    }

    @Override
    public void stop() {
        food = null;
        chewingTicks = 0;
        zombie.getNavigation().stop();
        scanCooldown = Math.max(scanCooldown, 12);
    }
}
