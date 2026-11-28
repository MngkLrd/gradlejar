package dev.minemods.traumacraft.health;

/** Shared capability model. Core owns movement; pain relief never repairs structure. */
public final class PhysicalFunction {
    private PhysicalFunction(){}
    public static float limb(float hp,boolean fracture,boolean stabilized){
        if(hp<=0)return 0;
        float f=.20f+.80f*hp;
        return fracture?Math.min(f,stabilized?.68f:.32f):f;
    }
    public static float systemic(BodyState b){
        float shock=switch(b.shockStage()){case 1->.88f;case 2->.65f;case 3->.25f;default->1f;};
        return Math.min(shock,b.organs().capacity(b.bloodFraction()));
    }
    public static float arm(BodyState b,BodyPart arm){return b.limbFunction(arm);}
    public static float mining(BodyState b,BodyPart arm){return arm(b,arm)*systemic(b);}
    public static float strike(BodyState b,BodyPart arm){return arm(b,arm)*systemic(b);}
    public static float attackSpeed(BodyState b,BodyPart arm){return (.25f+.75f*arm(b,arm))*systemic(b);}
    public static boolean canSprint(BodyState b){
        return b.shockStage()<2&&systemic(b)>.65f&&b.limbFunction(BodyPart.LEFT_LEG)>.4f&&b.limbFunction(BodyPart.RIGHT_LEG)>.4f;
    }
    public static float jump(BodyState b){return Math.min(b.limbFunction(BodyPart.LEFT_LEG),b.limbFunction(BodyPart.RIGHT_LEG))*systemic(b);}
    public static float movementPenalty(BodyState b){
        float systemicPenalty=switch(b.shockStage()){case 1->.10f;case 2->.30f;case 3->.58f;default->0f;};
        return Math.max(Math.max(b.legPenalty(),systemicPenalty),1-systemic(b));
    }
    public static boolean canUse(BodyState b,BodyPart arm,boolean twoHanded){
        if(b.shockStage()>=3||systemic(b)<.20f||arm(b,arm)<=0)return false;
        BodyPart other=arm==BodyPart.LEFT_ARM?BodyPart.RIGHT_ARM:BodyPart.LEFT_ARM;
        return !twoHanded||Math.min(arm(b,arm),arm(b,other))>=.35f;
    }
}
