package dev.minemods.thermal.server;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import java.lang.reflect.Method;
import java.util.Optional;
import dev.minemods.thermal.PhysiologyInputs;
import org.slf4j.LoggerFactory;
/** Optional, versioned public APIs only. No private addon data or hard dependencies. */
final class AddonBridge {
 private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("WorldLogic Thermal integration");
 private record Trauma(Method status,Method report,Class<?> zone,Class<?> injury){}
 private static final Trauma TRAUMA=trauma();
 private record Reader(Method read,Method first,Method second,boolean optional){}
 private static final Reader HYDRATION=hydrationReader();
 private static final Reader METABOLISM=reader("metabolism","dev.minemods.metabolism.api.MetabolismApi","physiology","Physiology","availableEnergy","dietQuality",false);
 private static final Reader BLOOD=reader("worldlogic_trauma","dev.minemods.traumacraft.api.TraumaApi","status","Status","bloodMl","maximumBloodMl",true);
 private static final Reader PERFUSION=reader("worldlogic_trauma","dev.minemods.traumacraft.api.TraumaApi","physiology","Physiology","perfusion",null,true);
 private static boolean warnedMetabolism,warnedBlood,warnedPerfusion;
 private static Reader reader(String mod,String type,String method,String record,String first,String second,boolean optional){
  if(!ModList.get().isLoaded(mod))return null;
  try{
   Class<?> api=Class.forName(type);if(api.getField("API_VERSION").getInt(null)!=1)return null;
   Class<?> data=Class.forName(type+"$"+record);
   return new Reader(api.getMethod(method,ServerPlayer.class),data.getMethod(first),second==null?null:data.getMethod(second),optional);
  }catch(ReflectiveOperationException|LinkageError|RuntimeException e){LOG.warn("Optional {} public endpoint {} unavailable",mod,method);return null;}
 }
 private static Reader hydrationReader(){
  var current=reader("hydration","dev.minemods.hydration.api.HydrationApi","physiology","Physiology","hydrationPercent","electrolyteIndex",true);
  return current!=null?current:reader("hydration","dev.minemods.hydration.api.HydrationApi","status","Status","hydrationPercent",null,true);
 }
 private static double[] read(Reader r,ServerPlayer p)throws ReflectiveOperationException{
  if(r==null)return null;
  Object v=r.read().invoke(null,p);
  if(r.optional())v=((Optional<?>)v).orElse(null);
  if(v==null)return null;
  double a=((Number)r.first().invoke(v)).doubleValue(),b=r.second()==null?50:((Number)r.second().invoke(v)).doubleValue();
  if(!Double.isFinite(a)||!Double.isFinite(b))return null;
  return new double[]{a,b};
 }
 static PhysiologyInputs physiology(ServerPlayer p){
  if(!p.isAlive()||p.isCreative()||p.isSpectator())return PhysiologyInputs.absent();
  double[] water=null,energy=null,blood=null,perfusion=null;
  try{water=read(HYDRATION,p);}catch(ReflectiveOperationException|LinkageError|RuntimeException ex){if(!warnedHydration){LOG.warn("Hydration physiology read failed",ex);warnedHydration=true;}}
  try{energy=read(METABOLISM,p);}catch(ReflectiveOperationException|LinkageError|RuntimeException ex){if(!warnedMetabolism){LOG.warn("Metabolism physiology read failed",ex);warnedMetabolism=true;}}
  try{blood=read(BLOOD,p);}catch(ReflectiveOperationException|LinkageError|RuntimeException ex){if(!warnedBlood){LOG.warn("Trauma blood read failed",ex);warnedBlood=true;}}
  try{perfusion=read(PERFUSION,p);}catch(ReflectiveOperationException|LinkageError|RuntimeException ex){if(!warnedPerfusion){LOG.warn("Trauma perfusion read failed",ex);warnedPerfusion=true;}}
  if(perfusion!=null&&(perfusion[0]<0||perfusion[0]>1))perfusion=null;
  if(water!=null&&(water[0]<0||water[0]>100||water[1]<0||water[1]>100))water=null;
  if(energy!=null&&(energy[0]<0||energy[0]>1||energy[1]<0||energy[1]>1))energy=null;
  if(blood!=null&&(blood[1]<=0||blood[0]<0||blood[0]>blood[1]))blood=null;
  return new PhysiologyInputs(energy!=null,water!=null,blood!=null||perfusion!=null,energy==null?1:energy[0],energy==null?1:energy[1],
   water==null?100:water[0],water==null?50:water[1],blood==null?1:blood[0]/blood[1],perfusion==null?Double.NaN:perfusion[0]);
 }
 private static final Method WEATHER=method("dev.minemods.worldlogic.api.WorldLogicApi","localWeather",ServerLevel.class,BlockPos.class);
 private static boolean warnedTrauma,warnedHydration,warnedWeather;
 private AddonBridge(){}
 private static Method method(String type,String name,Class<?>... params){
  try{return Class.forName(type).getMethod(name,params);}catch(ReflectiveOperationException|LinkageError e){return null;}
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
 private static final Method WATER=method("dev.minemods.worldlogic.api.WorldLogicApi","waterProfile",ServerLevel.class,BlockPos.class);
 private static final Method SEASON=method("dev.minemods.worldlogic.api.WorldLogicApi","season",ServerLevel.class,BlockPos.class);
 private static boolean warnedWater,warnedSeason;
 static String waterKind(ServerLevel level,BlockPos pos){
   if(WATER==null)return "UNKNOWN";
   try{
     Object profile=WATER.invoke(null,level,pos);
     return profile==null?"UNKNOWN":profile.getClass().getMethod("kind").invoke(profile).toString();
   }catch(ReflectiveOperationException|LinkageError|RuntimeException e){if(!warnedWater){LOG.warn("Core water profile unavailable",e);warnedWater=true;}return "UNKNOWN";}
 }
 static double seasonalOffset(ServerLevel level,BlockPos pos){
   if(SEASON==null)return 0;
   try{
     Object season=((Optional<?>)SEASON.invoke(null,level,pos)).orElse(null);
     if(season==null)return 0;
     double offset=number(season,"temperatureOffsetC");
     return Double.isFinite(offset)?Math.max(-35,Math.min(35,offset)):0;
   }catch(ReflectiveOperationException|LinkageError|RuntimeException e){if(!warnedSeason){LOG.warn("Core season unavailable",e);warnedSeason=true;}return 0;}
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
