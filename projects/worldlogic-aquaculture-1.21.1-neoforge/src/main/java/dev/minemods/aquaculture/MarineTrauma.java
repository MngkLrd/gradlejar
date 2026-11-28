package dev.minemods.aquaculture;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.fml.ModList;
import java.lang.reflect.Method;

/** Optional public API integration. No Trauma internal NBT, no doubled anatomical injury. */
public final class MarineTrauma {
 private MarineTrauma(){}
 private static boolean initialized=false;
 private static Method injury;
 private static Object zone,burn;
 @SuppressWarnings({"rawtypes","unchecked"})
 private static void init(){
  if(initialized)return;initialized=true;
  if(!ModList.get().isLoaded("worldlogic_trauma"))return;
  try{
   Class<?> api=Class.forName("dev.minemods.traumacraft.api.TraumaApi");
   if(api.getField("API_VERSION").getInt(null)!=1)return;
   Class z=Class.forName(api.getName()+"$Zone"),i=Class.forName(api.getName()+"$Injury");
   zone=Enum.valueOf(z,"LEFT_LEG");burn=Enum.valueOf(i,"BURN");
   injury=api.getMethod("reportInjury",ServerPlayer.class,z,i,float.class,int.class);
  }catch(ReflectiveOperationException|LinkageError ex){injury=null;}
 }
 public static boolean reportShock(ServerPlayer player){
  init();
  if(injury==null)return false;
  try{return Boolean.TRUE.equals(injury.invoke(null,player,zone,burn,3f,0));}
  catch(ReflectiveOperationException|LinkageError ex){return false;}
 }
 public static void shock(ServerPlayer p,MarineAnimal source){
  if(!p.isAlive()||p.isCreative()||p.isSpectator())return;
  boolean accepted=reportShock(p);
  if(!accepted)p.hurt(p.damageSources().mobAttack(source),3f);
  p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,2));
  p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,1));
 }
}
