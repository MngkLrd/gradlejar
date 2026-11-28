package dev.minemods.metabolism;
/** No second stamina pool: a bounded rate budget accounting for observed recovery by Core or other legitimate sources. */
final class RestRecovery {
 static final class Clock {
  private double previous=Double.NaN,credit;
  double request(double fatigue,boolean resting,double rate){
   if(!Double.isFinite(fatigue)||fatigue<=1e-8||!resting||!Double.isFinite(rate)||rate<=0){previous=fatigue;credit=0;return 0;}
   if(Double.isFinite(previous)&&fatigue>previous+1e-6){previous=fatigue;credit=0;return 0;}
   if(Double.isFinite(previous))credit-=Math.max(0,previous-fatigue);
   previous=fatigue;credit+=rate/20;
   return Math.min(fatigue,Math.max(0,credit));
  }
  void settled(double before,double after){credit-=Math.max(0,before-after);previous=after;}
 }
 private static final java.lang.reflect.Method CORE_POLICY=find();
 private static java.lang.reflect.Method find(){try{return dev.minemods.worldlogic.api.WorldLogicApi.class.getMethod("usesMetabolicRestRecovery",net.minecraft.server.level.ServerPlayer.class);}catch(ReflectiveOperationException|LinkageError e){return null;}}
 static boolean coreOwns(net.minecraft.server.level.ServerPlayer p){if(CORE_POLICY==null)return false;try{return Boolean.TRUE.equals(CORE_POLICY.invoke(null,p));}catch(ReflectiveOperationException|LinkageError e){return false;}}
 private RestRecovery(){}
}
