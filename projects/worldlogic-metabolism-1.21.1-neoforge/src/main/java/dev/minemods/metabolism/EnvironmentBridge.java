package dev.minemods.metabolism;
import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.Method;
import java.util.Optional;
final class EnvironmentBridge {
 private record Access(Method read,Method first,Method second) {
  static Access find(String type,String method,String record,String first,String second){
   try{var api=Class.forName(type);if(api.getField("API_VERSION").getInt(null)!=1)return null;var r=Class.forName(type+"$"+record);return new Access(api.getMethod(method,ServerPlayer.class),r.getMethod(first),second==null?null:r.getMethod(second));}
   catch(ReflectiveOperationException|LinkageError e){return null;}
  }
  double[] sample(ServerPlayer p){try{var o=(Optional<?>)read.invoke(null,p);if(o.isPresent()){double a=((Number)first.invoke(o.get())).doubleValue(),b=second==null?-1:((Number)second.invoke(o.get())).doubleValue();if(Double.isFinite(a)&&Double.isFinite(b))return new double[]{a,b};}}catch(ReflectiveOperationException|LinkageError|ClassCastException e){}return new double[]{-1,-1};}
 }
 private static final Access WATER=Access.find("dev.minemods.hydration.api.HydrationApi","physiology","Physiology","hydrationPercent","electrolyteIndex");
 private static final Access TEMP=Access.find("dev.minemods.thermal.api.ThermalApi","status","Status","bodyC",null);
 static void sample(ServerPlayer p,MetabolicModel.State s){
  var h=WATER==null?new double[]{-1,-1}:WATER.sample(p);var t=TEMP==null?new double[]{-1,-1}:TEMP.sample(p);
  s.water=h[0]>=0&&h[0]<=100?h[0]:-1;s.salts=h[1]>=0&&h[1]<=100?h[1]:-1;s.bodyC=t[0]>=25&&t[0]<=45?t[0]:-1;
 }
 private EnvironmentBridge(){}
}
