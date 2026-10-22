package dev.minemods.cowardlymobs.ai;

import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class MobAiRegistration {
    private MobAiRegistration() {}

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // The event may happen before a chunk is ready: only install goals here.
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Zombie zombie) {
            zombie.goalSelector.addGoal(1, new WoundedRetreatGoal(zombie));
        } else if (event.getEntity() instanceof AbstractSkeleton skeleton) {
            skeleton.goalSelector.addGoal(1, new WoundedRetreatGoal(skeleton));
            skeleton.goalSelector.addGoal(2, new SkeletonFormationGoal(skeleton));
        }
    }
}
