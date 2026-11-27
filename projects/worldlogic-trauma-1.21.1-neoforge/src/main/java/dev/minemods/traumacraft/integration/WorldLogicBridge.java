package dev.minemods.traumacraft.integration;
import dev.minemods.traumacraft.health.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.Balance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.HumanoidArm;
public final class WorldLogicBridge {
    private static final String LEG="worldlogic_injury", ARM="worldlogic_arm_until";
    private WorldLogicBridge() {}
    public static boolean active(Player p) { return p instanceof ServerPlayer && p.isAlive() && !p.isCreative() && !p.isSpectator(); }
    /** The Core rule disables only its legacy injuries when Trauma is installed. Trauma owns anatomy. */
    public static boolean injuryEnabled(Player p) { return active(p); }
    public static double fatigue(Player p) { return p instanceof ServerPlayer server ? WorldLogicApi.fatigue(server).fatiguePercent() : 0; }
    public static boolean legInjury(Player p) { return injuryEnabled(p) && BodyStates.get(p).legPenalty()>0; }
    public static boolean armInjury(Player p) { return injuryEnabled(p) && BodyStates.get(p).armImpaired(); }
    public static double mergePenalty(Player p,double original) { return injuryEnabled(p)?Math.max(original,PhysicalFunction.movementPenalty(BodyStates.get(p))):original; }
    public static boolean canTreat(Player p) { return !Balance.down(p); }
    public static boolean resting(Player p) {
        return active(p) && p.onGround() && !p.isSprinting() && !p.isSwimming() && !p.isInWater()
             && !p.isUsingItem() && p.getFoodData().getFoodLevel()>=14 && fatigue(p)<55
             && p.getDeltaMovement().horizontalDistance()<.04 && !Balance.down(p);
    }
    public static boolean functionsEnabled(Player p){return injuryEnabled(p);}
    public static float miningFactor(Player p){return functionsEnabled(p)?PhysicalFunction.mining(BodyStates.get(p),mainArm(p)):1f;}
    public static float strikeFactor(Player p){return functionsEnabled(p)?PhysicalFunction.strike(BodyStates.get(p),mainArm(p)):1f;}
    public static BodyPart armForHand(Player p,net.minecraft.world.InteractionHand hand){
        BodyPart main=mainArm(p);
        return hand==net.minecraft.world.InteractionHand.MAIN_HAND?main:main==BodyPart.LEFT_ARM?BodyPart.RIGHT_ARM:BodyPart.LEFT_ARM;
    }
    public static BodyPart mainArm(Player p) { return p.getMainArm()==HumanoidArm.LEFT?BodyPart.LEFT_ARM:BodyPart.RIGHT_ARM; }
    /** Trauma is authoritative for anatomy; old core timers must not become new injuries at login/respawn. */
    public static void clearLegacyInjuries(Player p) {
        p.getPersistentData().remove(LEG);
        p.getPersistentData().remove(ARM);
    }
    public static void migrateLegacy(Player p) {
        if (!active(p)) return;
        var tag=p.getPersistentData(); BodyState b=BodyStates.get(p);
        long now=p.level().getGameTime();
        long armNow=((ServerPlayer)p).serverLevel().getServer().overworld().getGameTime();
        if (WorldLogicApi.ruleEnabled("injuries") && tag.getLong(LEG)>now && !b.hasFracturedLeg()) {
            for (BodyPart part:new BodyPart[]{BodyPart.LEFT_LEG,BodyPart.RIGHT_LEG})
                if (b.percent(part)>.49f) b.damage(part,b.health(part)-part.maximum*.49f);
        }
        if (WorldLogicApi.ruleEnabled("shoulder_impact") && tag.getLong(ARM)>armNow && !b.armImpaired())
            b.damage(mainArm(p),mainArm(p).maximum*.35f);
        tag.remove(LEG); tag.remove(ARM);
        BodyStates.flush(p);
    }
    public static void collision(Player p,double severity,String reason) {
        if (!injuryEnabled(p) || severity<.75 || !"collision".equals(reason)) return;
        BodyState b=BodyStates.get(p); BodyPart arm=mainArm(p);
        b.damage(arm, (float)Math.min(20,8+severity*7));
        p.getPersistentData().remove(ARM);
        BodyStates.flush(p);
    }
    public static void aggravate(Player p,int ticks) {
        if (!legInjury(p) || ticks<=0) return;
        BodyState b=BodyStates.get(p);
        for (BodyPart leg:new BodyPart[]{BodyPart.LEFT_LEG,BodyPart.RIGHT_LEG})
            if (b.fractured(leg) && !b.stabilized(leg)) b.damage(leg, Math.min(.25f,ticks*.015f));
    }
}
