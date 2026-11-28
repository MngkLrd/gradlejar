package dev.minemods.traumacraft.health;

import net.minecraft.nbt.CompoundTag;
import java.util.Arrays;

/** Game-scale physiology, not an additional HP pool. All time inputs are server seconds.
 * Each region keeps its existing structural HP. Organs store function and internal bleeding separately.
 */
public final class OrganState {
    public enum Organ {
        BRAIN(BodyPart.HEAD), HEART(BodyPart.CHEST), LEFT_LUNG(BodyPart.CHEST), RIGHT_LUNG(BodyPart.CHEST),
        LIVER(BodyPart.STOMACH), LEFT_KIDNEY(BodyPart.STOMACH), RIGHT_KIDNEY(BodyPart.STOMACH),
        STOMACH(BodyPart.STOMACH), INTESTINES(BodyPart.STOMACH);
        public final BodyPart region;
        Organ(BodyPart region){this.region=region;}
    }
    private final float[] integrity=new float[Organ.values().length], bleeding=new float[Organ.values().length];
    private float oxygen=1, deficit, circulationSupport=1, shockLoad;
    private int lowSeconds, stableSeconds;
    private boolean unconscious;
    public OrganState(){reset();}
    public void reset(){Arrays.fill(integrity,1);Arrays.fill(bleeding,0);oxygen=1;deficit=0;circulationSupport=1;shockLoad=0;lowSeconds=stableSeconds=0;unconscious=false;}
    private static float clamp(float x,float min,float max){return Float.isFinite(x)?Math.max(min,Math.min(max,x)):min;}
    public float integrity(Organ organ){return integrity[organ.ordinal()];}
    public float bleeding(Organ organ){return bleeding[organ.ordinal()];}
    public float oxygen(){return oxygen;}
    public float oxygenDebt(){return deficit;}
    public float shockLoad(){return shockLoad;}
    public boolean unconscious(){return unconscious;}
    public int systemicShock(){return shockLoad>=.78f?3:shockLoad>=.48f?2:shockLoad>=.20f?1:0;}
    public float circulationSupport(){return circulationSupport;}
    public float ventilation(){float l=integrity(Organ.LEFT_LUNG),r=integrity(Organ.RIGHT_LUNG);return clamp((l+r)*.5f+.18f*Math.abs(l-r),0,1);}
    public float renalFunction(){return (integrity(Organ.LEFT_KIDNEY)+integrity(Organ.RIGHT_KIDNEY))*.5f;}
    public float digestiveFunction(){return (integrity(Organ.STOMACH)+integrity(Organ.INTESTINES))*.5f;}
    public float perfusion(float bloodFraction){
        // Near-normal volume compensates mild loss; severe hypovolemia and cardiac damage reduce delivery.
        return clamp(bloodFraction/.85f,0,1)*integrity(Organ.HEART)*circulationSupport;
    }
    public float delivery(float bloodFraction){return oxygen*perfusion(bloodFraction);}
    public float internalLossPerSecond(){float total=0;for(float x:bleeding)total+=x;return total;}
    public boolean hasInternalBleeding(){return internalLossPerSecond()>.001f;}
    public float capacity(float bloodFraction){
        return unconscious?0:Math.min(integrity(Organ.BRAIN),clamp(delivery(bloodFraction)/.75f,0,1));
    }
    public float repairFactor(float bloodFraction){
        return clamp(delivery(bloodFraction)/.75f,0,1)
          *(.4f+.6f*Math.min(integrity(Organ.LIVER),renalFunction()))
          *(.6f+.4f*digestiveFunction());
    }
    public boolean brainDestroyed(){return integrity(Organ.BRAIN)<=0;}
    void destroyBrainByTrauma(){integrity[Organ.BRAIN.ordinal()]=0;}
    private void injure(Organ organ,float fraction,float bloodRate){
        int i=organ.ordinal();
        integrity[i]=clamp(integrity[i]-fraction,0,1);
        bleeding[i]=clamp(bleeding[i]+bloodRate,0,12);
    }
    /** Deterministic regional trauma. Minor impacts do not injure protected organs.
     * With only seven external hit zones, side-specific penetrating trajectories are intentionally not inferred.
     */
    public boolean impact(BodyPart region,float hp,WoundKind kind){return impact(region,hp,kind,null);}
    public boolean impact(BodyPart region,float hp,WoundKind kind,Organ target){
        if(!Float.isFinite(hp)||hp<=0||kind==null||kind==WoundKind.BURN||kind==WoundKind.FROSTBITE)return false;
        boolean penetrating=kind==WoundKind.PUNCTURE||kind==WoundKind.LACERATION;
        float threshold=penetrating?7:12;
        if(hp<=threshold)return false;
        float severity=clamp((hp-threshold)/region.maximum*(penetrating?1.35f:1f),0,1.5f);
        float rate=severity>.20f?(severity-.20f)*(penetrating?9:5):0;
        if(target!=null&&target.region==region){
            // Focused penetration consumes the same structural impact, not another external HP hit.
            float local=severity*(target==Organ.BRAIN?.85f:1.05f);
            injure(target,local,rate*.85f);return true;
        }
        switch(region){
            case HEAD -> injure(Organ.BRAIN,severity*.85f,rate*.20f);
            case CHEST -> {injure(Organ.HEART,severity*.65f,rate*.40f);injure(Organ.LEFT_LUNG,severity*.85f,rate*.30f);injure(Organ.RIGHT_LUNG,severity*.85f,rate*.30f);}
            case STOMACH -> {injure(Organ.LIVER,severity*.75f,rate*.36f);injure(Organ.LEFT_KIDNEY,severity*.45f,rate*.12f);injure(Organ.RIGHT_KIDNEY,severity*.45f,rate*.12f);injure(Organ.STOMACH,severity*.65f,rate*.18f);injure(Organ.INTESTINES,severity*.65f,rate*.22f);}
            default -> {return false;}
        }
        return true;
    }
    /** Returns the internal blood loss for this second; caller subtracts it from the ONE blood pool. */
    public float second(float bloodFraction,boolean breathing,boolean resting,float nutrition){return second(bloodFraction,breathing,resting,nutrition,1,0);}
    public float second(float bloodFraction,boolean breathing,boolean resting,float repair,float support,float pain){
        circulationSupport=clamp(support,.1f,1);
        float loss=internalLossPerSecond();
        float target=breathing?ventilation():0;
        oxygen=clamp(oxygen+Math.max(-.08f,Math.min(.12f,target-oxygen)),0,1);
        float delivered=delivery(bloodFraction);
        if(delivered<.45f)deficit=clamp(deficit+(.45f-delivered)/.45f,0,120);
        else deficit=clamp(deficit-.8f,0,120);
        float shockTarget=clamp(Math.max(1-delivered,Math.max((1-bloodFraction)*1.25f,Math.max(0,pain-.45f)*.8f)),0,1);
        shockLoad=clamp(shockLoad+Math.max(-.035f,Math.min(.10f,shockTarget-shockLoad)),0,1);
        boolean low=delivered<.24f||integrity(Organ.BRAIN)<.18f||bloodFraction<.27f;
        if(low){lowSeconds=Math.min(30,lowSeconds+1);stableSeconds=0;}else{lowSeconds=0;if(delivered>.45f&&integrity(Organ.BRAIN)>.28f&&bloodFraction>.35f)stableSeconds=Math.min(30,stableSeconds+1);else stableSeconds=0;}
        if(lowSeconds>=3)unconscious=true;
        if(unconscious&&stableSeconds>=6)unconscious=false;
        if(deficit>=18&&delivered<.45f)injure(Organ.BRAIN,.008f+(.45f-delivered)*.025f,0);
        // Only low-rate internal leaks can close on their own under safe conditions.
        if(resting&&delivered>=.50f&&!unconscious){
            for(int i=0;i<bleeding.length;i++)if(bleeding[i]>0&&bleeding[i]<=2.5f)bleeding[i]=Math.max(0,bleeding[i]-.025f);
        }
        if(resting&&delivered>=.55f&&!unconscious&&!hasInternalBleeding()&&deficit<1){
            float rate=.0006f*clamp(repair,0,1)*repairFactor(bloodFraction);
            for(int i=0;i<integrity.length;i++)if(integrity[i]>0&&integrity[i]<1)integrity[i]=Math.min(1,integrity[i]+rate*(.35f+.65f*integrity[i]));
        }
        return loss;
    }
    /** Existing advanced trauma kit supplies stabilization, not organ HP or instant internal closure. */
    public boolean canStabilize(BodyPart region){for(var organ:Organ.values())if(organ.region==region&&bleeding(organ)>2.5f)return true;return false;}
    public boolean impaired(){if(oxygen<.999f||deficit>0||shockLoad>.01f||unconscious||hasInternalBleeding())return true;for(float x:integrity)if(x<1)return true;return false;}
    public boolean stabilize(BodyPart region){
        boolean changed=false;
        for(var organ:Organ.values())if(organ.region==region&&bleeding(organ)>2.5f){
            bleeding[organ.ordinal()]=Math.max(2.5f,bleeding(organ)*.55f);changed=true;
        }
        return changed;
    }
    public CompoundTag save(){
        var n=new CompoundTag();n.putFloat("oxygen",oxygen);n.putFloat("deficit",deficit);n.putFloat("circulationSupport",circulationSupport);n.putFloat("shockLoad",shockLoad);n.putInt("lowSeconds",lowSeconds);n.putInt("stableSeconds",stableSeconds);n.putBoolean("unconscious",unconscious);
        for(var organ:Organ.values()){var o=new CompoundTag();o.putFloat("integrity",integrity(organ));o.putFloat("bleeding",bleeding(organ));n.put(organ.name(),o);}
        return n;
    }
    public void load(CompoundTag n){
        reset();
        oxygen=read(n,"oxygen",1,1);deficit=read(n,"deficit",0,120);circulationSupport=Math.max(.1f,read(n,"circulationSupport",1,1));shockLoad=read(n,"shockLoad",0,1);lowSeconds=Math.max(0,Math.min(30,n.getInt("lowSeconds")));stableSeconds=Math.max(0,Math.min(30,n.getInt("stableSeconds")));unconscious=n.getBoolean("unconscious");
        for(var organ:Organ.values())if(n.contains(organ.name(),10)){var o=n.getCompound(organ.name());integrity[organ.ordinal()]=read(o,"integrity",1,1);bleeding[organ.ordinal()]=read(o,"bleeding",0,12);}
    }
    private static float read(CompoundTag n,String key,float fallback,float max){
        if(!n.contains(key,99))return fallback;float value=n.getFloat(key);
        return Float.isFinite(value)?clamp(value,0,max):fallback;
    }
}
