package dev.minemods.thermal.server;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import java.lang.reflect.Method;
import java.util.Optional;
import org.slf4j.LoggerFactory;
/** Optional, versioned public APIs only. No private addon data or hard dependencies. */
final class AddonBridge {
 private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("WorldLogic Thermal integration");
 private record Trauma(Method status,Method report,Class<?> zone,Class<?> injury){}
 private static final Trauma TRAUMA=trauma();
 private static final Method HYDRATION=statusMethod("dev.minemods.hydration.api.HydrationApi");
 private static final Method WEATHER=method("dev.minemods.worldlogic.api.WorldLogicApi","localWeather",ServerLevel.class,BlockPos.class);
 private static boolean warnedTrauma,warnedHydration,warnedWeather;
 private AddonBridge(){}
 private static Method method(String type,String name,Class<?>... params){
  try{return Class.forName(type).getMethod(name,params);}catch(ReflectiveOperationException|LinkageError e){return null;}
 }
 private static Method statusMethod(String type){
  try{Class<?> c=Class.forName(type);if(c.getField("API_VERSION").getInt(null)!=1)return null;return c.getMethod("status",ServerPlayer.class);}catch(ReflectiveOperationException|LinkageError e){return null;}
 }
 private static Trauma trauma(){
  if(!ModList.get().isLoaded("worldlogic_trauma"))return null;
  try{
   Class<?> a=Class.forName("dev.minemods.traumacraft.api.TraumaApi");
   if(a.getField("API_VERSION").getInt(null)!=1)return null;
   Class<?> z=Class.forName(a.getName()+"$Zone"),i=Class.forName(a.getName()+"$Injury");
   return new Trauma(a.getMethod("status",ServerPlayer.class),a.getMethod("reportInjury",ServerPlayer.class,z,i,float.class,int.class),z,i);
  }catch(ReflectiveOperationException|LinkageError e){LOG.warn("Trauma public API v1 unavailable",e);return null;}
 }
 @SuppressWarnings({"rawtypes","unchecked"})
 static boolean frostbite(ServerPlayer p,String limb,float amount){
  if(TRAUMA==null)return false;
  try{
   if(((Optional<?>)TRAUMA.status().invoke(null,p)).isEmpty())return false;
   Object zone=Enum.valueOf((Class)TRAUMA.zone(),limb),injury=Enum.valueOf((Class)TRAUMA.injury(),"FROSTBITE");
   return Boolean.TRUE.equals(TRAUMA.report().invoke(null,p,zone,injury,amount,0));
  }catch(ReflectiveOperationException|RuntimeException e){if(!warnedTrauma){LOG.warn("Trauma injury API failed",e);warnedTrauma=true;}return false;}
 }
 static double hydration(ServerPlayer p){
  if(HYDRATION==null)return 1;
  try{
   var v=((Optional<?>)HYDRATION.invoke(null,p)).orElse(null);
   if(v==null)return 1;
   double percent=((Number)v.getClass().getMethod("hydrationPercent").invoke(v)).doubleValue();
   return Double.isFinite(percent)?Math.max(0,Math.min(1,percent/100)):1;
  }catch(ReflectiveOperationException|RuntimeException e){if(!warnedHydration){LOG.warn("Hydration status API failed",e);warnedHydration=true;}return 1;}
 }
 record Local(String kind,double gust,double pressure,double cloud,double fog,double intensity){}
 static Optional<Local> weather(ServerLevel l,BlockPos p){
  if(WEATHER==null)return Optional.empty();
  try{
   Object w=((Optional<?>)WEATHER.invoke(null,l,p)).orElse(null);
   if(w==null)return Optional.empty();
   var c=w.getClass();
   return Optional.of(new Local(c.getMethod("kind").invoke(w).toString(),number(w,"gustMps"),number(w,"pressureHpa"),number(w,"cloudFraction"),number(w,"fogDensity"),number(w,"intensity")));
  }catch(ReflectiveOperationException|RuntimeException e){if(!warnedWeather){LOG.warn("Core local weather API failed",e);warnedWeather=true;}return Optional.empty();}
 }
 private static double number(Object x,String key)throws ReflectiveOperationException{return ((Number)x.getClass().getMethod(key).invoke(x)).doubleValue();}
}
