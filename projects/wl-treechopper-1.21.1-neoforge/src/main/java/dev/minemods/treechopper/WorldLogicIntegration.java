package dev.minemods.treechopper;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.ModList;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
/** Public optional API v1 only; called on the authoritative server thread. */
final class WorldLogicIntegration {
    private static final Logger LOG=LogUtils.getLogger();
    private static boolean initialized=false;
    private static Method fatigue, stamina, trauma, regions, zone, function, protection, debit, recover, fatiguePercent;
    private static boolean warnedCore,warnedTrauma;
    record Effort(double stamina,double primary,double support) {
        double power(){return ChopEffort.power(stamina,primary,support);}
    }
    private static void init(){
        if(initialized)return;
        initialized=true;
        if(ModList.get().isLoaded("worldlogic")){
            try {
                Class<?> api=Class.forName("dev.minemods.worldlogic.api.WorldLogicApi");
                if(api.getField("API_VERSION").getInt(null)!=1)throw new IllegalStateException("Unsupported Core API version");
                Method f=api.getMethod("fatigue",ServerPlayer.class);
                stamina=f.getReturnType().getMethod("remainingStaminaPercent");
                fatigue=f;
                if(api.getField("API_REVISION").getInt(null)>=21){
                    Class<?> breaks=Class.forName("dev.minemods.worldlogic.api.WorldLogicBreaks");
                    Method check=breaks.getMethod("checkProtection",net.minecraft.server.level.ServerLevel.class,ServerPlayer.class,net.minecraft.core.BlockPos.class,net.minecraft.world.level.block.state.BlockState.class);
                    Method add=api.getMethod("addExertion",ServerPlayer.class,double.class);
                    recover=api.getMethod("recoverExertion",ServerPlayer.class,double.class);
                    fatiguePercent=f.getReturnType().getMethod("fatiguePercent");
                    debit=add;protection=check;
                }
            }catch(ReflectiveOperationException|LinkageError|RuntimeException e){LOG.warn("TreeChopper: Core API unavailable; using neutral stamina",e);}
        }
        if(ModList.get().isLoaded("worldlogic_trauma")){
            try{
                Class<?> api=Class.forName("dev.minemods.traumacraft.api.TraumaApi");
                if(api.getField("API_VERSION").getInt(null)!=1)throw new IllegalStateException("Unsupported Trauma API version");
                Class<?> status=Class.forName(api.getName()+"$Status");
                Class<?> region=Class.forName(api.getName()+"$Region");
                regions=status.getMethod("regions");
                zone=region.getMethod("zone");
                function=region.getMethod("function");
                trauma=api.getMethod("status",ServerPlayer.class);
            }catch(ReflectiveOperationException|LinkageError|RuntimeException e){LOG.warn("TreeChopper: Trauma API unavailable; using neutral arm function",e);}
        }
    }
    static boolean safeStrikes(){init();return protection!=null&&debit!=null;}
    static boolean checkProtection(net.minecraft.server.level.ServerLevel level,ServerPlayer player,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
        init();
        if(protection==null)return false;
        try{return Boolean.TRUE.equals(protection.invoke(null,level,player,pos,state));}
        catch(ReflectiveOperationException|LinkageError|RuntimeException e){LOG.warn("TreeChopper: protection probe failed; denying action",e);return false;}
    }
    // One explicit gameplay effort unit cost per accepted axe action, independent of automatic leaf/log checks.
    static final double STRIKE_COST=0.6;
    static void acceptedStrike(ServerPlayer player){
        if(!safeStrikes())return; // Preserve legacy Core debit path; never double charge old versions.
        try{debit.invoke(null,player,STRIKE_COST);}
        catch(ReflectiveOperationException|LinkageError|RuntimeException e){LOG.warn("TreeChopper: Core accepted strike debit failed",e);}
    }
    static double qaFatigue(ServerPlayer player) throws ReflectiveOperationException{
        return ((Number)fatiguePercent.invoke(fatigue.invoke(null,player))).doubleValue();
    }
    static void qaResetEffort(ServerPlayer player) throws ReflectiveOperationException{recover.invoke(null,player,100d);}
    static Effort effort(ServerPlayer player){
        init();
        double energy=1,left=1,right=1;
        if(fatigue!=null)try{
            Object state=fatigue.invoke(null,player);
            energy=((Number)stamina.invoke(state)).doubleValue()/100;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError e){
            if(!warnedCore){warnedCore=true;LOG.warn("TreeChopper: cannot read Core fatigue",e);}
        }
        if(trauma!=null)try{
            Optional<?> state=(Optional<?>)trauma.invoke(null,player);
            if(state.isPresent())for(Object region:(Iterable<?>)regions.invoke(state.get())){
                String name=((Enum<?>)zone.invoke(region)).name();
                if(name.equals("LEFT_ARM"))left=((Number)function.invoke(region)).doubleValue();
                if(name.equals("RIGHT_ARM"))right=((Number)function.invoke(region)).doubleValue();
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError e){
            if(!warnedTrauma){warnedTrauma=true;LOG.warn("TreeChopper: cannot read Trauma arm function",e);}
        }
        return player.getMainArm()==HumanoidArm.LEFT?new Effort(energy,left,right):new Effort(energy,right,left);
    }
}
