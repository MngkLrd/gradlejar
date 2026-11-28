package dev.minemods.traumacraft.health;

import dev.minemods.traumacraft.network.SyncBodyStatePacket;

/** Frozen medical state, distinct from the live body that will reset on respawn. */
public record DeathReport(Cause cause,Origin origin,String sourceId,int criticalRegions,int criticalOrgans,
                          int bleedingRegions,int bleedingOrgans,SyncBodyStatePacket body) {
    public enum Cause { DIRECT_TRAUMA, VITAL_DESTRUCTION, EXTERNAL_BLEEDING, INTERNAL_BLEEDING, BLOOD_LOSS, HYPOXIA, VANILLA }
    public enum Origin { FALL, EXPLOSION, FIRE, PROJECTILE, ATTACK, FREEZE, DROWNING, VOID, COMMAND, OTHER, SYSTEMIC }
    public boolean critical(BodyPart p){return (criticalRegions&(1<<p.ordinal()))!=0;}
    public boolean critical(OrganState.Organ o){return (criticalOrgans&(1<<o.ordinal()))!=0;}
    public boolean bleeding(BodyPart p){return (bleedingRegions&(1<<p.ordinal()))!=0;}
    public boolean bleeding(OrganState.Organ o){return (bleedingOrgans&(1<<o.ordinal()))!=0;}
    public static DeathReport capture(BodyState b,Cause cause,Origin origin,String sourceId){
        int regions=0,organs=0,bleeds=0,internal=0;
        for(var p:BodyPart.values()){
            if(cause!=Cause.VANILLA&&(p==BodyPart.HEAD||p==BodyPart.CHEST)&&b.health(p)<=0)regions|=1<<p.ordinal();
            if(b.bleeding(p)>0)bleeds|=1<<p.ordinal();
        }
        for(var organ:OrganState.Organ.values()){
            if(cause!=Cause.VANILLA&&organ==OrganState.Organ.BRAIN&&b.organs().brainDestroyed()){
                organs|=1<<organ.ordinal();regions|=1<<organ.region.ordinal();
            }
            if(b.organs().bleeding(organ)>.001f)internal|=1<<organ.ordinal();
        }
        // Chest destruction does not prove which specific organ was lethal.
        // Bleeding sources are marked separately from proven fatal anatomical regions.
        return new DeathReport(cause,origin,sourceId,regions,organs,bleeds,internal,SyncBodyStatePacket.from(b,0));
    }
}
