package dev.cowtipping.gameplay;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cow;
import java.util.Comparator;
import java.util.EnumSet;
public final class CuriousCowGoal extends Goal {
    private final Cow cow;
    private Cow subject;
    private int repathTicks;
    public CuriousCowGoal(Cow cow){this.cow=cow;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){
        if(CowTipState.isTipped(cow)||cow.isBaby()||cow.getRandom().nextInt(7)!=0) return false;
        subject=cow.level().getEntitiesOfClass(Cow.class,cow.getBoundingBox().inflate(9D),
            other->other!=cow && CowTipState.attractsAttention(other)).stream()
            .min(Comparator.comparingDouble(cow::distanceToSqr)).orElse(null);
        return subject!=null;
    }
    @Override public boolean canContinueToUse(){
        return subject!=null&&!CowTipState.isTipped(cow)&&CowTipState.attractsAttention(subject)&&cow.distanceToSqr(subject)<144D;
    }
    @Override public void start(){repathTicks=0;}
    @Override public void stop(){cow.getNavigation().stop();subject=null;}
    @Override public void tick(){
        if(subject==null)return;
        cow.getLookControl().setLookAt(subject,30F,30F);
        if(--repathTicks>0)return;
        repathTicks=12;
        double angle=((cow.getUUID().hashCode()&0xFFFF)/65535D)*(Math.PI*2D);
        double x=subject.getX()+Math.cos(angle)*2D;
        double z=subject.getZ()+Math.sin(angle)*2D;
        if(Math.pow(cow.getX()-x,2)+Math.pow(cow.getZ()-z,2)>0.95D*0.95D)
            cow.getNavigation().moveTo(x,subject.getY(),z,1.12D);
        else cow.getNavigation().stop();
    }
}
