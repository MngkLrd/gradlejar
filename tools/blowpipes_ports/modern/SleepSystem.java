package dev.minemods.blowpipes;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
public final class SleepSystem {
 private SleepSystem(){}
 private record Saved(boolean noAi,long end){}
 private static final Map<Mob,Saved> SLEEPING=new WeakHashMap<>();
 public static void sleep(Mob mob,int ticks){
  if(!(mob.level() instanceof ServerLevel level)||!mob.isAlive()||mob.getMaxHealth()>=100f)return;
  if(mob.isNoAi()&&!SLEEPING.containsKey(mob))return;
  Saved original=SLEEPING.get(mob);
  long end=level.getGameTime()+ticks;
  SLEEPING.put(mob,new Saved(original!=null&&original.noAi,Math.max(end,original==null?0:original.end)));
  mob.setTarget(null);mob.getNavigation().stop();mob.setNoAi(true);
 }
 public static void wake(Mob mob){
  Saved old=SLEEPING.remove(mob);
  if(old!=null)mob.setNoAi(old.noAi);
 }
 public static void check(Mob mob){
  Saved saved=SLEEPING.get(mob);
  if(saved==null)return;
  if(!(mob.level() instanceof ServerLevel level)||!mob.isAlive()||level.getGameTime()>=saved.end){
   wake(mob);return;
  }
  mob.setTarget(null);mob.getNavigation().stop();mob.setNoAi(true);
 }
 public static void tick(MinecraftServer server){
  var it=SLEEPING.entrySet().iterator();
  while(it.hasNext()){
   var entry=it.next();
   Mob mob=entry.getKey();
   if(mob==null){it.remove();continue;}
   Saved saved=entry.getValue();
   if(!(mob.level() instanceof ServerLevel level)||!mob.isAlive()||level.getGameTime()>=saved.end){
    mob.setNoAi(saved.noAi);it.remove();continue;
   }
   mob.setTarget(null);mob.getNavigation().stop();mob.setNoAi(true);
  }
 }
 public static void clear(){
  for(var entry:new ArrayList<>(SLEEPING.entrySet()))if(entry.getKey()!=null)wake(entry.getKey());
  SLEEPING.clear();
 }
}
