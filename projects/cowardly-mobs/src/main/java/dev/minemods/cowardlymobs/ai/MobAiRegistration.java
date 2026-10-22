package dev.minemods.cowardlymobs.ai;

import dev.minemods.cowardlymobs.hunger.ai.ZombieFoodGoal;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * One registration site for both AI systems.
 * Fear has goal priority 1; food gathering (zombies) and positioning (skeletons)
 * have priority 2. Weak keys avoid accumulating unloaded entities.
 */
public final class MobAiRegistration {
    private static final Set<Mob> REGISTERED =
            Collections.newSetFromMap(new WeakHashMap<>());

    private MobAiRegistration() {}

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Zombie zombie) {
            if (!REGISTERED.add(zombie)) return;
            zombie.goalSelector.addGoal(1, new WoundedRetreatGoal(zombie));
            zombie.goalSelector.addGoal(2, new ZombieFoodGoal(zombie));
        } else if (event.getEntity() instanceof AbstractSkeleton skeleton) {
            if (!REGISTERED.add(skeleton)) return;
            skeleton.goalSelector.addGoal(1, new WoundedRetreatGoal(skeleton));
            skeleton.goalSelector.addGoal(2, new SkeletonFormationGoal(skeleton));
        }
    }
}
