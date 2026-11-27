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
        received = true;
    }
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
