package dev.minemods.traumacraft.qa;

import dev.minemods.traumacraft.api.TraumaApi;
import dev.minemods.traumacraft.disease.*;
import dev.minemods.traumacraft.disease.DiseaseModel.*;
import dev.minemods.traumacraft.integration.HydrationBridge;
import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.registry.ModItems;
import dev.minemods.traumacraft.medical.TreatmentManager;
import dev.minemods.traumacraft.network.SyncBodyStatePacket;
import dev.minemods.traumacraft.event.TraumaEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

/** Server assertions. Integration uses the actual provider classes on a detached, non-FakePlayer server player. */
public final class DiseaseChecks {
    private DiseaseChecks(){}
    private static void require(boolean ok,String what){if(!ok)throw new IllegalStateException("Disease QA: "+what);}
    private static State onset(State s){
        int n=s.incubation();
        require(n>=90&&n<=180,"bounded incubation");
        for(int i=0;i<n-1;i++){s=DiseaseModel.second(s,Physiology.UNKNOWN,true).state();require(DiseaseModel.loss(s)==0,"no incubation symptoms");}
        return DiseaseModel.second(s,Physiology.UNKNOWN,true).state();
    }
    public static void verify(ServerPlayer p){
        WaterIllness.clear(p);
        try{
            var b=BodyStates.get(p);b.reset();
            TraumaApi.reportWaterIntake(p,250,.05,35);
            TraumaApi.reportWaterIntake(p,-1,1,0);
            TraumaApi.reportWaterIntake(p,250,Double.NaN,0);
            require(WaterIllness.load(p).equals(State.HEALTHY)&&TraumaApi.fluidLossPointsPerSecond(p)==0,"clean/saline/invalid intake no infection");
            TraumaApi.reportWaterIntake(p,250,.4,0);
            require(WaterIllness.load(p).stage()==Stage.HEALTHY,"low dose below threshold");
            TraumaApi.reportWaterIntake(p,250,.4,0);
            State incubating=WaterIllness.load(p);
            require(incubating.kind()==Kind.GASTROENTERITIS&&incubating.stage()==Stage.INCUBATING,"accumulated dose initiates incubation");
            require(SyncBodyStatePacket.from(b,0,incubating).illnessKind()==0,"asymptomatic diagnosis hidden from client");
            State mild=onset(incubating);
            require(mild.stage()==Stage.MILD&&DiseaseModel.loss(mild)>0&&! (mild.vomiting()>0),"mild gut symptoms");
            State heavy=onset(DiseaseModel.intake(State.HEALTHY,750,.95,0));
            require(heavy.kind()==Kind.ENTEROCOLITIS&&heavy.stage()==Stage.SEVERE&&heavy.vomiting()==5,"heavy disease and vomiting");
            WaterIllness.store(p,heavy);
            require(WaterIllness.load(p).equals(heavy),"persistent roundtrip");
            double loss=TraumaApi.fluidLossPointsPerSecond(p);
            require(loss>.55&&loss<1.5&&TraumaApi.fluidLossPointsPerSecond(p)==loss&&WaterIllness.load(p).equals(heavy),"read-only bounded illness loss");
            b.setBleeding(BodyPart.LEFT_ARM,2);
            require(TraumaApi.fluidLossPointsPerSecond(p)==loss,"blood loss excluded");
            b.reset();
            require(MedicalAction.autoTarget(p,b,MedicalItem.Type.MEDKIT)==BodyPart.STOMACH,"medkit targets illness without tissue damage");
            var item=(MedicalItem)ModItems.MEDKIT.get();
            p.inventoryMenu.setCarried(new ItemStack(item,2));
            require(TreatmentManager.begin(p,item,BodyPart.STOMACH,true,null),"actual cursor treatment begins");
            require(p.inventoryMenu.getCarried().getCount()==1,"one medkit consumed");
            p.tickCount+=TreatmentManager.duration(item.medicalType());TreatmentManager.tick(p);
            State treated=WaterIllness.load(p);
            require(treated.support()==150&&treated.severity()==heavy.severity()&&!b.hasInjury(),"support not instant cure or anatomical repair");
            require(!MedicalAction.canApply(p,b,MedicalItem.Type.MEDKIT,BodyPart.STOMACH),"no stacking while supported");
            var packet=SyncBodyStatePacket.from(b,0,treated);
            require(packet.illnessKind()==Kind.ENTEROCOLITIS.ordinal()&&packet.illnessStage()==Stage.SEVERE.ordinal()&&packet.illnessTreated(),"client packet carries actual clinical state");
            State normal=DiseaseModel.second(treated,new Physiology(100,50,"NORMAL"),true).state();
            State dry=DiseaseModel.second(treated,new Physiology(8,95,"CRITICAL"),true).state();
            State untreated=DiseaseModel.second(heavy,Physiology.UNKNOWN,true).state();
            State nutritionLow=DiseaseModel.second(treated,new Physiology(100,50,"NORMAL"),true,.25).state();
            require(nutritionLow.severity()>normal.severity(),"nutrition resistance slows disease recovery");
            var neutral=dev.minemods.traumacraft.integration.MetabolismBridge.Factors.NEUTRAL;
            require(neutral.treatment()==1&&neutral.illnessLoss()==0,"neutral no-addon treatment and loss");
            require(normal.severity()<dry.severity()&&normal.severity()<untreated.severity(),"hydration/electrolytes and treatment affect recovery");
            State recovered=normal;boolean diarrhea=false,vomit=false;
            for(int i=0;i<2000&&recovered.symptomatic();i++){var step=DiseaseModel.second(recovered,Physiology.UNKNOWN,true);recovered=step.state();diarrhea|=step.diarrhea();vomit|=step.vomit();}
            require(recovered.equals(State.HEALTHY)&&DiseaseModel.loss(recovered)==0&&diarrhea&&vomit,"progressive recovery with recurring symptoms");
            p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            require(TraumaApi.fluidLossPointsPerSecond(p)==0&&TraumaApi.illnessStatus(p).isEmpty(),"creative excluded");
            TraumaApi.reportWaterIntake(p,1000,1,35);
            require(WaterIllness.load(p).equals(treated),"creative intake ignored");
            p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            TraumaEvents.resetAfterDeath(p);
            require(WaterIllness.load(p).equals(State.HEALTHY)&&TraumaApi.fluidLossPointsPerSecond(p)==0,"death clears illness");
        }finally{WaterIllness.clear(p);TreatmentManager.forget(p);p.inventoryMenu.setCarried(ItemStack.EMPTY);p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);}
    }
    public static void verifyIntegration(ServerPlayer fixture)throws ReflectiveOperationException{
        require(net.neoforged.fml.ModList.get().isLoaded("hydration"),"actual Hydration installed");
        var p=new ServerPlayer(fixture.serverLevel().getServer(),fixture.serverLevel(),new GameProfile(UUID.randomUUID(),"TraumaHydrationFixture"),ClientInformation.createDefault());
        try{
            var initial=HydrationBridge.read(p).orElseThrow(()->new IllegalStateException("HydrationApi.physiology unavailable"));
            require(initial.hydration()==100&&initial.electrolytes()==50&&initial.dehydration().equals("NORMAL"),"actual physiology defaults");
            Class<?> hydration=Class.forName("dev.minemods.hydration.Hydration");
            Class<?> balance=Class.forName("dev.minemods.hydration.WaterBalance");
            Class<?> state=Class.forName("dev.minemods.hydration.WaterBalance$State");
            // Set an explicit QA fixture through Hydration's own state writer, never used in production bridge.
            var seed=state.getConstructor(double.class,double.class,double.class,double.class,double.class).newInstance(18d,0d,0d,0d,85d);
            var store=hydration.getDeclaredMethod("store",ServerPlayer.class,state);store.setAccessible(true);store.invoke(null,p,seed);
            var snapshot=HydrationBridge.read(p).orElseThrow();
            require(snapshot.hydration()==18&&snapshot.electrolytes()==85&&snapshot.dehydration().equals("SEVERE"),"all actual physiology values");
            Class<?> batch=Class.forName("dev.minemods.hydration.WaterBatch"),quality=Class.forName("dev.minemods.hydration.WaterBalance$Quality");
            Object dirty=batch.getConstructor(int.class,quality,double.class,double.class).newInstance(750,quality.getField("TREAT_RECOMMENDED").get(null),0d,.95d);
            Class<?> bridge=Class.forName("dev.minemods.hydration.PhysiologyBridge");
            require(Boolean.TRUE.equals(bridge.getMethod("reportIntake",ServerPlayer.class,batch).invoke(null,p,dirty)),"actual Hydration intake bridge reaches Trauma");
            require(WaterIllness.load(p).stage()==Stage.INCUBATING,"integration incubation");
            var loss=bridge.getMethod("illnessLoss",ServerPlayer.class);
            require(((Number)loss.invoke(null,p)).doubleValue()==0,"Hydration sees zero loss during incubation, not fallback");
            int seconds=WaterIllness.load(p).incubation();
            for(int i=0;i<seconds;i++)WaterIllness.tick(p);
            double medical=((Number)loss.invoke(null,p)).doubleValue();
            require(medical>0&&medical==TraumaApi.fluidLossPointsPerSecond(p),"actual reverse loss bridge");
            require(hydration.getMethod("state",ServerPlayer.class).invoke(null,p).equals(seed),"Trauma never writes Hydration balance");
            Object normal=balance.getMethod("tick",state,double.class).invoke(null,seed,.05d);
            Object sick=balance.getMethod("tick",state,double.class).invoke(null,seed,.05d+medical);
            double normalWater=((Number)state.getMethod("water").invoke(normal)).doubleValue(),sickWater=((Number)state.getMethod("water").invoke(sick)).doubleValue();
            require(Math.abs(normalWater-sickWater-medical)<1e-8,"Hydration applies medical loss exactly once");
        }finally{WaterIllness.clear(p);BodyStates.forget(p);}
    }
}
