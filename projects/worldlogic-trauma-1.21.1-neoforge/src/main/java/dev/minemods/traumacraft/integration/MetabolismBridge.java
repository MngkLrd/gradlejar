package dev.minemods.traumacraft.integration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.lang.reflect.Method;
import java.util.WeakHashMap;
/** Optional public contract; Trauma retains exclusive ownership of anatomical repair and medical consequences. */
public final class MetabolismBridge {
 public record Factors(float tissue,float bone,float blood,float fracture,float resistance,float toxin){
  public static final Factors NEUTRAL=new Factors(1,1,1,1,1,0);
  public float treatment(){return .5f+.5f*tissue;}
  public double illnessLoss(){return Math.max(0,toxin-.55)*.6;}
 }
 private record Access(Method read,Method[] values){
  static Access find(){try{var api=Class.forName("dev.minemods.metabolism.api.MetabolismApi");if(api.getField("API_VERSION").getInt(null)!=1)return null;var record=Class.forName(api.getName()+"$Clinical");String[] names={"tissue","bone","blood","fractureThreshold","resistance","toxin"};Method[] m=new Method[names.length];for(int i=0;i<m.length;i++)m[i]=record.getMethod(names[i]);return new Access(api.getMethod("clinical",ServerPlayer.class),m);}catch(ReflectiveOperationException|LinkageError e){return null;}}
  Factors read(ServerPlayer p){try{Object s=read.invoke(null,p);float[] v=new float[6];for(int i=0;i<6;i++){v[i]=((Number)values[i].invoke(s)).floatValue();if(!Float.isFinite(v[i]))return Factors.NEUTRAL;v[i]=Math.max(i==5?0:.1f,Math.min(1,v[i]));}return new Factors(v[0],v[1],v[2],v[3],v[4],v[5]);}catch(ReflectiveOperationException|LinkageError|ClassCastException e){return Factors.NEUTRAL;}}
 }
 private static final Access API=Access.find();
 public static Factors read(Player p){return API!=null&&p instanceof ServerPlayer sp&&WorldLogicBridge.active(sp)?API.read(sp):Factors.NEUTRAL;}
 private static final WeakHashMap<ServerPlayer,Rate> RATES=new WeakHashMap<>();
 private static final class Rate{final float[] seconds=new float[60];int index,count;float sum;}
 public static void record(ServerPlayer p,float repaired){var r=RATES.computeIfAbsent(p,k->new Rate());r.sum-=r.seconds[r.index];r.seconds[r.index]=Math.max(0,repaired);r.sum+=r.seconds[r.index];r.index=(r.index+1)%60;r.count=Math.min(60,r.count+1);}
 public static double rate(ServerPlayer p){var r=RATES.get(p);return r==null||r.count<5?-1:Math.max(0,r.sum*60/r.count);}
 public static void forget(ServerPlayer p){RATES.remove(p);}
 private MetabolismBridge(){}
}
