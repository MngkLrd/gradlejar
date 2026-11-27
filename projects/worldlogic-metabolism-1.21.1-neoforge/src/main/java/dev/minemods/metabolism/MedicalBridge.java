package dev.minemods.metabolism;
import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.Method;
import java.util.Optional;
final class MedicalBridge {
 record Health(float current,float maximum,boolean anatomical){}
 private static Method status,regions,health,maximum;
 private static boolean initialized=false;
 private static void init(){
  if(initialized)return;initialized=true;
  try{
   Class<?> api=Class.forName("dev.minemods.traumacraft.api.TraumaApi");
   status=api.getMethod("status",ServerPlayer.class);
   regions=Class.forName(api.getName()+"$Status").getMethod("regions");
   Class<?> region=Class.forName(api.getName()+"$Region");health=region.getMethod("health");maximum=region.getMethod("maximum");
  }catch(ReflectiveOperationException|LinkageError e){status=null;}
 }
 static Health read(ServerPlayer p){
  init();
  if(status!=null)try{
   var result=(Optional<?>)status.invoke(null,p);
   if(result.isPresent()){float h=0,m=0;for(Object r:(Iterable<?>)regions.invoke(result.get())){h+=((Number)health.invoke(r)).floatValue();m+=((Number)maximum.invoke(r)).floatValue();}if(m>0)return new Health(h,m,true);}
  }catch(ReflectiveOperationException|LinkageError|ClassCastException e){status=null;}
  return new Health(p.getHealth(),p.getMaxHealth(),false);
 }
}
