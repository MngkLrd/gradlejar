package dev.minemods.cowardlymobs.ai;

import dev.minemods.cowardlymobs.hunger.ai.ZombieFoodGoal;
import dev.minemods.cowardlymobs.jockey.SkeletonSpiderMountGoal;
import dev.minemods.cowardlymobs.spider.SpiderInstinctGoal;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Registers server-side goals once per mob across chunk and dimension reentry. */
public final class MobAiRegistration {
    private static final Set<Mob> REGISTERED =
            Collections.newSetFromMap(new WeakHashMap<>());

    private MobAiRegistration() {}

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof Zombie zombie) {
            if (!REGISTERED.add(zombie)) return;
            // Fear overrides eating. Food must outrank vanilla melee MOVE goals,
            // otherwise a chasing zombie never leaves the player for rotten flesh.
            zombie.goalSelector.addGoal(0, new WoundedRetreatGoal(zombie));
            zombie.goalSelector.addGoal(1, new ZombieFoodGoal(zombie));
        } else if (event.getEntity() instanceof Spider spider) {
            if (!REGISTERED.add(spider)) return;
            // Priority 2 outranks vanilla leaps/melee movement, but float/swim stays higher.
            spider.goalSelector.addGoal(1, new SpiderInstinctGoal(spider));
        } else if (event.getEntity() instanceof AbstractSkeleton skeleton) {
            if (!REGISTERED.add(skeleton)) return;
            // A chance to leap onto a spider takes precedence over fleeing.
            if (skeleton instanceof Skeleton ordinary) {
                ordinary.goalSelector.addGoal(0, new SkeletonSpiderMountGoal(ordinary));
            }
            skeleton.goalSelector.addGoal(1, new WoundedRetreatGoal(skeleton));
            skeleton.goalSelector.addGoal(2, new SkeletonFormationGoal(skeleton));
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide()
                && event.getEntity() instanceof PathfinderMob mob
                && (mob instanceof Zombie || mob instanceof AbstractSkeleton)
                && event.getSource().getEntity() instanceof Player player) {
            FearSignals.onPlayerAttack(mob, player);
        }
    }
}
