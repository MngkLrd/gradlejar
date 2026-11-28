package dev.minemods.traumacraft.client;

import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.disease.DiseaseModel;
import dev.minemods.traumacraft.integration.ThermalCondition;
import dev.minemods.traumacraft.network.SyncBodyStatePacket;

public final class ClientBodyState {
    private static final float[] HP = new float[BodyPart.values().length];
    private static final byte[] WOUNDS=new byte[BodyPart.values().length];
    private static final byte[] BLEEDING = new byte[BodyPart.values().length];
    private static final boolean[] FRACTURE = new boolean[BodyPart.values().length];
    private static final boolean[] STABILIZED = new boolean[BodyPart.values().length];
    private static final float[] ORGANS=new float[dev.minemods.traumacraft.health.OrganState.Organ.values().length], INTERNAL=new float[ORGANS.length];
    private static float oxygen=1,perfusion=1,delivery=1,renal=1,digestive=1,repair=1,shockLoad;
    private static boolean unconscious;
    private static boolean received;
    private static int concussionTicks;
    private static int painkillerTicks;
    private static float bloodMl=5000,pain;
    private static int shock;
    private static DiseaseModel.Kind illnessKind=DiseaseModel.Kind.NONE;
    private static DiseaseModel.Stage illnessStage=DiseaseModel.Stage.HEALTHY;
    private static boolean illnessTreated;
    private static ThermalCondition thermal=ThermalCondition.NONE;

    private ClientBodyState() {}
    public static void accept(SyncBodyStatePacket packet) {
        for (BodyPart part : BodyPart.values()) {
            int i = part.ordinal();
            HP[i] = Math.max(0f, Math.min(part.maximum, packet.hp()[i]));
            BLEEDING[i] = (byte)Math.max(0, Math.min(2, packet.bleeding()[i]));
            WOUNDS[i]=(byte)(packet.wounds()[i]&63);
            FRACTURE[i] = packet.fracture()[i];
            STABILIZED[i] = packet.stabilized()[i];
        }
        concussionTicks = packet.concussion();
        painkillerTicks = packet.painkiller();
        thermal=ThermalCondition.fromWire(packet.thermalStage());
        bloodMl=Float.isFinite(packet.bloodMl())?Math.max(0,Math.min(5000,packet.bloodMl())):5000;
        pain=Float.isFinite(packet.pain())?Math.max(0,Math.min(100,packet.pain())):0;
        shock=Math.max(0,Math.min(3,packet.shock()));
        var kinds=DiseaseModel.Kind.values();var stages=DiseaseModel.Stage.values();
        illnessKind=packet.illnessKind()>0&&packet.illnessKind()<kinds.length?kinds[packet.illnessKind()]:DiseaseModel.Kind.NONE;
        illnessStage=illnessKind!=DiseaseModel.Kind.NONE&&packet.illnessStage()>=DiseaseModel.Stage.MILD.ordinal()&&packet.illnessStage()<stages.length?stages[packet.illnessStage()]:DiseaseModel.Stage.HEALTHY;
        if(illnessStage==DiseaseModel.Stage.HEALTHY)illnessKind=DiseaseModel.Kind.NONE;
        illnessTreated=illnessKind!=DiseaseModel.Kind.NONE&&packet.illnessTreated();
        var clinical=packet.clinical();
        for(int i=0;i<ORGANS.length;i++){ORGANS[i]=bounded(clinical.integrity()[i],1,1);INTERNAL[i]=bounded(clinical.internalBleeding()[i],12,0);}
        oxygen=bounded(clinical.oxygen(),1,1);perfusion=bounded(clinical.perfusion(),1,1);delivery=bounded(clinical.delivery(),1,1);renal=bounded(clinical.renal(),1,1);digestive=bounded(clinical.digestive(),1,1);repair=bounded(clinical.repair(),1,1);shockLoad=bounded(clinical.shockLoad(),1,0);unconscious=clinical.unconscious();
        received = true;
    }
    private static float bounded(float v,float max,float fallback){return Float.isFinite(v)?Math.max(0,Math.min(max,v)):fallback;}
    public static float organ(dev.minemods.traumacraft.health.OrganState.Organ o){return received?ORGANS[o.ordinal()]:1;}
    public static float internal(dev.minemods.traumacraft.health.OrganState.Organ o){return received?INTERNAL[o.ordinal()]:0;}
    public static float internalLoss(){float total=0;if(received)for(float x:INTERNAL)total+=x;return total;}
    public static float oxygen(){return received?oxygen:1;}
    public static float perfusion(){return received?perfusion:1;}
    public static float delivery(){return received?delivery:1;}
    public static float renal(){return received?renal:1;}
    public static float digestive(){return received?digestive:1;}
    public static float repair(){return received?repair:1;}
    public static float shockLoad(){return received?shockLoad:0;}
    public static boolean unconscious(){return received&&unconscious;}
    public static DiseaseModel.Kind illnessKind(){return received?illnessKind:DiseaseModel.Kind.NONE;}
    public static DiseaseModel.Stage illnessStage(){return received?illnessStage:DiseaseModel.Stage.HEALTHY;}
    public static boolean illnessTreated(){return received&&illnessTreated;}
    public static float bloodMl(){return received?bloodMl:5000;}
    public static float pain(){return received?pain:0;}
    public static int shock(){return received?shock:0;}
    public static int concussionTicks() { return received ? concussionTicks : 0; }
    public static int painkillerTicks() { return received ? painkillerTicks : 0; }
    public static boolean hasReceived() { return received; }
    public static ThermalCondition thermal(){return received?thermal:ThermalCondition.NONE;}
    public static void clear() { received = false; concussionTicks = 0; painkillerTicks = 0; thermal=ThermalCondition.NONE; illnessKind=DiseaseModel.Kind.NONE;illnessStage=DiseaseModel.Stage.HEALTHY;illnessTreated=false; }
    public static float hp(BodyPart part) { return received ? HP[part.ordinal()] : part.maximum; }
    public static float fraction(BodyPart part) { return hp(part) / part.maximum; }
    public static int wounds(BodyPart part){return received?WOUNDS[part.ordinal()]&63:0;}
    public static int bleeding(BodyPart part) { return received ? BLEEDING[part.ordinal()] : 0; }
    public static boolean fractured(BodyPart part) { return received && FRACTURE[part.ordinal()]; }
    public static boolean stabilized(BodyPart part) { return received && STABILIZED[part.ordinal()]; }
    public static boolean injured(BodyPart part) {
        return hp(part) < part.maximum || fractured(part) || bleeding(part) > 0;
    }
    public static boolean hasAnyInjuries() {
        for (BodyPart part : BodyPart.values()) if (injured(part)) return true;
        return false;
    }
}
