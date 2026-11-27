package dev.minemods.traumacraft.health;

import dev.minemods.traumacraft.item.MedicalItem;
import net.minecraft.world.entity.player.Player;

/** Canonical medical roles. Old registry IDs remain usable as equivalent medicines, not magic buffs. */
public final class MedicalAction {
    private MedicalAction(){}
    public static MedicalItem.Type canonical(MedicalItem.Type type){
        return switch(type){
            case COMPRESSION_WRAP -> MedicalItem.Type.BANDAGE;
            case FRACTURE_BRACE,BONE_MENDER -> MedicalItem.Type.SPLINT;
            case STERILE_PATCH,COMPACT_MEDKIT,FIELD_MEDKIT,RECOVERY_SERUM -> MedicalItem.Type.MEDKIT;
            case ADRENALINE_INJECTOR,GUARDIAN_INJECTOR,FOCUS_INJECTOR,ENDURANCE_INJECTOR -> MedicalItem.Type.ANALGESIC_TABLETS;
            default -> type;
        };
    }
    private static boolean livingDamage(BodyState b,BodyPart p){return b.health(p)>0&&b.health(p)<p.maximum;}
    private static boolean limb(BodyPart p){return p.isLeg()||p==BodyPart.LEFT_ARM||p==BodyPart.RIGHT_ARM;}
    public static boolean canApply(BodyState b,MedicalItem.Type type,BodyPart part){
        if(b==null||type==null||part==null)return false;
        return switch(canonical(type)){
            case BANDAGE -> b.bleeding(part)>0;
            case HEMOSTATIC_GEL -> b.bleeding(part)==2;
            case TOURNIQUET -> limb(part)&&b.bleeding(part)==2;
            case SPLINT -> limb(part)&&b.fractured(part)&&!b.stabilized(part);
            case MEDKIT -> livingDamage(b,part);
            case HERBAL_POULTICE -> b.bleeding(part)==0&&!b.fractured(part)&&livingDamage(b,part)&&(b.wounds(part)&WoundKind.CONTUSION.bit)!=0;
            case RAG_DRESSING -> b.bleeding(part)==1||(b.bleeding(part)==0&&livingDamage(b,part));
            case TRAUMA_KIT -> livingDamage(b,part)||b.bleeding(part)>0||(limb(part)&&b.fractured(part)&&!b.stabilized(part));
            case SURGICAL_KIT -> limb(part)&&b.health(part)==0;
            case ANALGESIC_TABLETS -> b.pain()>0&&b.painkillerTicks()==0;
            default -> false;
        };
    }
    public static BodyPart autoTarget(BodyState b,MedicalItem.Type type){
        if(b==null||type==null)return null;
        var canonical=canonical(type);
        BodyPart chosen=null;float best=-1;
        for(var part:BodyPart.values()){
            if(!canApply(b,canonical,part))continue;
            float score=switch(canonical){
                case BANDAGE,HEMOSTATIC_GEL,TOURNIQUET,RAG_DRESSING,TRAUMA_KIT -> b.bleeding(part)*2+(1-b.percent(part));
                case ANALGESIC_TABLETS -> part==BodyPart.CHEST?2:1;
                default -> 1-b.percent(part);
            };
            if(score>best){best=score;chosen=part;}
        }
        return chosen;
    }
    public static boolean apply(Player player,BodyState b,MedicalItem.Type type,BodyPart part){
        if(!canApply(b,type,part))return false;
        switch(canonical(type)){
            case BANDAGE -> b.reduceBleeding(part);
            case TOURNIQUET -> b.stopBleeding(part);
            case HEMOSTATIC_GEL -> {b.stopBleeding(part);if(b.health(part)>0)b.heal(part,10);}
            case SPLINT -> b.stabilize(part,4800);
            case MEDKIT -> b.heal(part,25);
            case HERBAL_POULTICE -> {b.heal(part,6);b.setPainkillerTicks(Math.max(b.painkillerTicks(),200));}
            case RAG_DRESSING -> {if(b.bleeding(part)==1)b.stopBleeding(part);if(b.health(part)>0)b.heal(part,4);}
            case TRAUMA_KIT -> {
                if(b.health(part)>0)b.heal(part,45);
                b.stopBleeding(part);
                if(limb(part)&&b.fractured(part)&&!b.stabilized(part))b.stabilize(part,3600);
            }
            case SURGICAL_KIT -> {
                b.heal(part,15);b.stopBleeding(part);
                if(b.fractured(part)&&!b.stabilized(part))b.stabilize(part,4800);
            }
            case ANALGESIC_TABLETS -> b.setPainkillerTicks(2400);
            default -> {return false;}
        }
        return true;
    }
}
