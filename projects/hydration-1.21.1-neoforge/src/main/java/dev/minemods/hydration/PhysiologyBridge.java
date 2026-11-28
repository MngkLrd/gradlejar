package dev.minemods.hydration;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.LoggerFactory;

/** Public addon APIs only; optional class resolution once, never access foreign NBT. */
public final class PhysiologyBridge {
 public record Sample(double bodyC,int bleeding,double bloodFraction,boolean thermalAvailable,boolean traumaAvailable){}
 private record Endpoint(Method status,Method a,Method b,Method c,Method regionBleed){}
 private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("hydration");
 private static Endpoint thermal,trauma;
 private static Method waterIntake,medicalLoss,renalSnapshot,renalValue,metabolicIntake;
 private static boolean renalFailed,metabolicFailed;
 private static boolean medicalFailed;
 private static boolean initialized,thermalFailed,traumaFailed;
 private PhysiologyBridge(){}
 private static Endpoint resolve(String mod,String api,boolean isThermal){
  if(!ModList.get().isLoaded(mod))return null;
  try{
   Class<?> c=Class.forName(api);
   if(c.getField("API_VERSION").getInt(null)!=1)throw new IllegalStateException("Unsupported API version");
   Class<?> status=Class.forName(api+"$Status");
   return isThermal?new Endpoint(c.getMethod("status",ServerPlayer.class),status.getMethod("bodyC"),null,null,null):
    new Endpoint(c.getMethod("status",ServerPlayer.class),status.getMethod("bloodMl"),status.getMethod("maximumBloodMl"),status.getMethod("regions"),Class.forName(api+"$Region").getMethod("bleeding"));
  }catch(ReflectiveOperationException|LinkageError|RuntimeException e){LOG.warn("Hydration optional API unavailable: {}",mod,e);return null;}
 }
 private static void init(){
  if(initialized)return;
  thermal=resolve("worldlogic_thermal","dev.minemods.thermal.api.ThermalApi",true);
  trauma=resolve("worldlogic_trauma","dev.minemods.traumacraft.api.TraumaApi",false);
  if(trauma!=null)try{
   Class<?> api=Class.forName("dev.minemods.traumacraft.api.TraumaApi");
   waterIntake=api.getMethod("reportWaterIntake",ServerPlayer.class,int.class,double.class,double.class);
   medicalLoss=api.getMethod("fluidLossPointsPerSecond",ServerPlayer.class);
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){waterIntake=null;medicalLoss=null;LOG.info("Trauma waterborne illness API not available; Hydration fallback active");}
  if(trauma!=null)try{
   Class<?> api=Class.forName("dev.minemods.traumacraft.api.TraumaApi");
   if(api.getField("API_REVISION").getInt(null)>=5){renalSnapshot=api.getMethod("physiology",ServerPlayer.class);renalValue=Class.forName(api.getName()+"$Physiology").getMethod("renalFunction");}
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){renalSnapshot=null;renalValue=null;LOG.info("Trauma renal API unavailable; neutral renal function");}
  if(ModList.get().isLoaded("metabolism"))try{
   Class<?> api=Class.forName("dev.minemods.metabolism.api.MetabolismApi");
   if(api.getField("API_VERSION").getInt(null)==1&&api.getField("API_REVISION").getInt(null)>=4)
    metabolicIntake=api.getMethod("reportWaterIntake",ServerPlayer.class,int.class,double.class,double.class);
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){LOG.warn("Metabolism water intake API unavailable",ex);}
  initialized=true;
 }
 public static boolean reportIntake(ServerPlayer p,WaterBatch b){
  init();if(waterIntake==null||medicalLoss==null||medicalFailed)return false;
  try{waterIntake.invoke(null,p,b.ml(),b.risk(),b.salinity());return true;}
  catch(ReflectiveOperationException|RuntimeException ex){medicalFailed=true;LOG.warn("Trauma water intake bridge failed",ex);return false;}
 }
 public static void reportMetabolicIntake(ServerPlayer p,WaterBatch b){
  init();if(metabolicIntake==null||metabolicFailed)return;
  try{metabolicIntake.invoke(null,p,b.mB(),b.quality()==WaterBalance.Quality.UNKNOWN?Math.max(.65,b.risk()):b.risk(),b.quality()==WaterBalance.Quality.UNKNOWN?-1:b.salinity());}
  catch(ReflectiveOperationException|RuntimeException|LinkageError ex){metabolicFailed=true;LOG.warn("Metabolism water intake bridge failed",ex);}
 }
 public static double renalFunction(ServerPlayer p){
  init();if(renalSnapshot==null||renalValue==null||renalFailed)return 1;
  try{var s=(Optional<?>)renalSnapshot.invoke(null,p);if(s.isEmpty())return 1;
   double v=((Number)renalValue.invoke(s.get())).doubleValue();return Double.isFinite(v)&&v>=0&&v<=1?v:1;}
  catch(ReflectiveOperationException|RuntimeException|LinkageError ex){renalFailed=true;LOG.warn("Trauma renal bridge failed",ex);return 1;}
 }
 public static double illnessLoss(ServerPlayer p){
  init();if(waterIntake==null||medicalLoss==null||medicalFailed)return Double.NaN;
  try{double v=((Number)medicalLoss.invoke(null,p)).doubleValue();if(!Double.isFinite(v)||v<0)throw new IllegalStateException("Invalid medical loss");return WaterBalance.clamp(v,0,1.5);}
  catch(ReflectiveOperationException|RuntimeException ex){medicalFailed=true;LOG.warn("Trauma fluid loss bridge failed",ex);return Double.NaN;}
 }
 public static Sample sample(ServerPlayer p){
  if(!p.serverLevel().getServer().isSameThread())throw new IllegalStateException("Server thread required");
  init();double body=Double.NaN,blood=Double.NaN;int bleed=0;boolean t=false,h=false;
  if(thermal!=null&&!thermalFailed)try{
   var v=(Optional<?>)thermal.status.invoke(null,p);
   if(v.isPresent()){body=((Number)thermal.a.invoke(v.get())).doubleValue();t=Double.isFinite(body)&&body>=20&&body<=50;}
  }catch(ReflectiveOperationException|RuntimeException ex){thermalFailed=true;LOG.warn("Thermal API failed",ex);}
  if(trauma!=null&&!traumaFailed)try{
   var v=(Optional<?>)trauma.status.invoke(null,p);
   if(v.isPresent()){
    Object s=v.get();double amount=((Number)trauma.a.invoke(s)).doubleValue(),max=((Number)trauma.b.invoke(s)).doubleValue();
    if(Double.isFinite(amount)&&Double.isFinite(max)&&max>0){blood=WaterBalance.clamp(amount/max,0,1);h=true;}
    for(Object r:(Iterable<?>)trauma.c.invoke(s))bleed+=Math.max(0,Math.min(2,((Number)trauma.regionBleed.invoke(r)).intValue()));
   }
  }catch(ReflectiveOperationException|RuntimeException ex){traumaFailed=true;bleed=0;h=false;blood=Double.NaN;LOG.warn("Trauma API failed",ex);}
  return new Sample(t?body:Double.NaN,Math.min(14,bleed),blood,t,h);
 }
}
