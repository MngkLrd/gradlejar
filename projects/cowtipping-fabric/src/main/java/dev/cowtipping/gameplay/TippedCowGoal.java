package dev.cowtipping.gameplay;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;
public final class TippedCowGoal extends Goal {
    private final Cow cow;
    private boolean dustSpawned;
    public TippedCowGoal(Cow cow){this.cow=cow;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
    @Override public boolean canUse(){return cow.isAlive()&&CowTipState.isTipped(cow);}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void start(){dustSpawned=false;cow.getNavigation().stop();}
    @Override public void tick(){
        cow.getNavigation().stop();
        Vec3 velocity=cow.getDeltaMovement();
        cow.setDeltaMovement(0D,velocity.y,0D);
        if(!dustSpawned&&CowTipState.age(cow)>=CowTipState.FALL_TICKS&&cow.level() instanceof ServerLevel level){
            dustSpawned=true;
            level.sendParticles(ParticleTypes.POOF,cow.getX(),cow.getY()+(cow.isBaby()?0.20:0.36),cow.getZ(),
                7,0.38,0.12,0.38,0.015);
        }
    }
}
