package dev.minemods.metabolism;
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
/** Read the owner's actual container snapshot, never its NBT or guessed vessel capacity. */
final class WaterIntakeBridge {
 record Water(int volume,double risk,double salt){}
 private record Access(Method read,Method volume,Method risk,Method salt,int revision){
  static Access find(){try{var c=Class.forName("dev.minemods.hydration.api.HydrationApi");if(c.getField("API_VERSION").getInt(null)!=1)return null;var r=Class.forName(c.getName()+"$LiquidStatus");return new Access(c.getMethod("liquidWater",ItemStack.class),r.getMethod("volumeMb"),r.getMethod("contaminationRisk"),r.getMethod("salinityGkg"),c.getField("API_REVISION").getInt(null));}catch(ReflectiveOperationException|LinkageError e){return null;}}
  Water sample(ItemStack s){try{var o=(Optional<?>)read.invoke(null,s);if(o.isPresent()){var v=o.get();int n=((Number)volume.invoke(v)).intValue();double r=((Number)risk.invoke(v)).doubleValue(),t=((Number)salt.invoke(v)).doubleValue();if(n>=0&&Double.isFinite(r)&&Double.isFinite(t))return new Water(n,r,t);}}catch(ReflectiveOperationException|LinkageError|ClassCastException e){}return null;}
 }
 private static final Access API=Access.find();
 static boolean callbackOwnsWater(){return API!=null&&API.revision()>=5;}
 static boolean useItemFallback(boolean authoritativeCallback,boolean ownedWater){return !authoritativeCallback||!ownedWater;}
 static Water read(ItemStack stack){return API==null?null:API.sample(stack);}
 private WaterIntakeBridge(){}
}
