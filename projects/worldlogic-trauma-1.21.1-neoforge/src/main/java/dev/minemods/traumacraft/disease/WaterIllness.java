package dev.minemods.traumacraft.disease;

import dev.minemods.traumacraft.integration.HydrationBridge;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import dev.minemods.traumacraft.climb.ClimbManager;
import dev.minemods.traumacraft.medical.TreatmentManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import java.util.Locale;

/** Durable medical state, written only on logical server thread. Hydration reads loss and owns the actual deduction. */
public final class WaterIllness {
    public static final String KEY="traumacraft_water_illness";
    private WaterIllness(){}
    public static DiseaseModel.State load(Player p){
        var n=p.getPersistentData().getCompound(KEY);
        DiseaseModel.Kind kind;
        try{kind=DiseaseModel.Kind.valueOf(n.getString("kind"));}catch(IllegalArgumentException ex){kind=DiseaseModel.Kind.NONE;}
        return new DiseaseModel.State(kind,n.getDouble("dose"),n.getDouble("severity"),n.getInt("incubation"),n.getInt("elapsed"),n.getInt("support"),n.getInt("vomiting"));
    }
    public static void store(Player p,DiseaseModel.State s){
        if(s.equals(DiseaseModel.State.HEALTHY)){clear(p);return;}
        var n=new CompoundTag();n.putString("kind",s.kind().name());n.putDouble("dose",s.dose());n.putDouble("severity",s.severity());
        n.putInt("incubation",s.incubation());n.putInt("elapsed",s.elapsed());n.putInt("support",s.support());n.putInt("vomiting",s.vomiting());
        p.getPersistentData().put(KEY,n);
    }
    public static void clear(Player p){p.getPersistentData().remove(KEY);}
    public static void clonePlayer(Player from,Player to,boolean death){if(death)clear(to);else store(to,load(from));}
    public static void intake(ServerPlayer p,int volume,double risk,double salt){var before=load(p);var after=DiseaseModel.intake(before,volume,risk,salt);if(!after.equals(before))store(p,after);}
    public static boolean canTreat(Player p){return WorldLogicBridge.active(p)&&DiseaseModel.canTreat(load(p));}
    public static boolean treat(Player p){
        if(!canTreat(p))return false;
        store(p,DiseaseModel.treat(load(p)));
        if(p instanceof ServerPlayer sp&&sp.connection!=null)sp.displayClientMessage(Component.translatable("illness.traumacraft.treatment"),true);
        return true;
    }
    public static double loss(ServerPlayer p){return DiseaseModel.loss(load(p));}
    public static Component diagnosis(DiseaseModel.State s){
        if(s.stage()==DiseaseModel.Stage.HEALTHY||s.stage()==DiseaseModel.Stage.INCUBATING)return Component.translatable("illness.traumacraft.no_symptoms");
        return Component.translatable("illness.traumacraft.status",Component.translatable("illness.traumacraft.kind."+s.kind().name().toLowerCase(Locale.ROOT)),Component.translatable("illness.traumacraft.stage."+s.stage().name().toLowerCase(Locale.ROOT)));
    }
    public static void tick(ServerPlayer p){
        if(!WorldLogicBridge.active(p))return;
        var before=load(p);if(before.equals(DiseaseModel.State.HEALTHY))return;
        var physiology=HydrationBridge.read(p).orElse(DiseaseModel.Physiology.UNKNOWN);
        var step=DiseaseModel.second(before,physiology,WorldLogicBridge.resting(p));var after=step.state();store(p,after);
        if(step.vomit()){
            TreatmentManager.cancel(p);ClimbManager.cancel(p);
            if(p.connection!=null&&!(p instanceof net.neoforged.neoforge.common.util.FakePlayer)){
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION,100,0,false,false,true));
                p.level().playSound(null,p.getX(),p.getY(),p.getZ(),net.minecraft.sounds.SoundEvents.PLAYER_BURP,net.minecraft.sounds.SoundSource.PLAYERS,.65f,.7f);
                p.displayClientMessage(Component.translatable("illness.traumacraft.vomit"),true);
            }
        }else if(p.connection!=null&&!(p instanceof net.neoforged.neoforge.common.util.FakePlayer)){
            if(before.stage()!=after.stage()&&after.stage()==DiseaseModel.Stage.HEALTHY)
                p.displayClientMessage(Component.translatable("illness.traumacraft.recovered"),true);
            else if(after.symptomatic()&&(before.stage()!=after.stage()||step.diarrhea()))
                p.displayClientMessage(step.diarrhea()?Component.translatable("illness.traumacraft.diarrhea",diagnosis(after)):diagnosis(after),true);
        }
    }
}
