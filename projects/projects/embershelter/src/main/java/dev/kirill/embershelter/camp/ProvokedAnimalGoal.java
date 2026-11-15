package dev.kirill.embershelter.camp;
import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.InteractionHand;
/** Passive land animals become briefly defensive only when injured by a player. */
public final class ProvokedAnimalGoal extends Goal {
    private final Animal animal;
    private ServerPlayer target;
    private long nextHit;
    public ProvokedAnimalGoal(Animal animal) {
        this.animal=animal;
        setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean canUse() {
        target=RetaliationTracker.provokedTarget(animal);
        return target!=null;
    }
    @Override public boolean canContinueToUse() {
        return (target=RetaliationTracker.provokedTarget(animal))!=null;
    }
    @Override public void tick() {
        if(target==null) return;
        animal.getLookControl().setLookAt(target,30f,30f);
        animal.getNavigation().moveTo(target,1.15d);
        double reach=animal.getBbWidth()+target.getBbWidth()+0.65d;
        if(animal.level().getGameTime()>=nextHit && animal.distanceToSqr(target)<=reach*reach
          && animal.getSensing().hasLineOfSight(target)) {
            nextHit=animal.level().getGameTime()+25;
            target.hurt(animal.damageSources().mobAttack(animal),2.0f);
            animal.swing(InteractionHand.MAIN_HAND);
        }
    }
    @Override public void stop() {
        target=null;
        animal.getNavigation().stop();
    }
}
