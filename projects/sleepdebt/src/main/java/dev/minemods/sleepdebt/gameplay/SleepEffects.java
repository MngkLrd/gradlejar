package dev.minemods.sleepdebt.gameplay;
import dev.minemods.sleepdebt.SleepSettings;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import dev.minemods.sleepdebt.registry.ModEffects;
final class SleepEffects {
 static double load(int value){return Math.max(0,Math.min(1,(value-250)/750.0));}
 private static void set(ServerPlayer p,Holder<Attribute> a,String key,double amount){
  AttributeInstance i=p.getAttribute(a);if(i==null)return;
  ResourceLocation id=ResourceLocation.fromNamespaceAndPath("sleepdebt",key);
  var old=i.getModifier(id);
  if(old!=null&&Math.abs(old.amount()-amount)<.0001)return;
  i.removeModifier(id);
  if(Math.abs(amount)>.0001)i.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
 }
 static int[] percentages(ServerPlayer p,CompoundTag t){
  boolean enabled=SleepSettings.PENALTIES.get()&&!p.isCreative()&&!p.isSpectator();
  double sleep=enabled?load(SleepEngine.effectiveDebt(t))*SleepSettings.SLEEP_PENALTY.get():0;
  double body=enabled?load(t.getInt("fatigue"))*SleepSettings.BODY_PENALTY.get():0;
  if(enabled&&t.getInt("crash")>0)sleep+=.12;
  if(enabled&&t.getInt("micro")>0)sleep+=.45;
  return new int[]{(int)Math.round(100*Math.min(.65,sleep*.40)),(int)Math.round(100*Math.min(.75,body*.50)),(int)Math.round(100*Math.min(.48,body*.30)),(int)Math.round(100*Math.min(.48,body*.30)),(int)Math.round(100*Math.min(1,enabled?load(t.getInt("stress"))*SleepSettings.STRESS_PENALTY.get():0))};
 }
 private static void status(ServerPlayer p,Holder<MobEffect> effect,int ticks){
  var current=p.getEffect(effect);
  if(ticks<=0||p.isSpectator()){if(current!=null)p.removeEffect(effect);return;}
  if(current==null||Math.abs(current.getDuration()-ticks)>40){
   if(current!=null)p.removeEffect(effect);
   p.addEffect(new MobEffectInstance(effect,ticks,0,false,false,true));
  }
 }
 static void apply(ServerPlayer p,CompoundTag t){
  status(p,ModEffects.ALERTNESS,t.getInt("mask"));
  status(p,ModEffects.CRASH,t.getInt("crash"));
  status(p,ModEffects.CALM,t.getInt("calm"));
  int[] v=percentages(p,t);
  set(p,Attributes.ATTACK_SPEED,"sleep_attack",-v[0]/100.0);
  set(p,Attributes.BLOCK_BREAK_SPEED,"body_mining",-v[1]/100.0);
  set(p,Attributes.MOVEMENT_SPEED,"body_move",-v[2]/100.0);
  set(p,Attributes.ATTACK_DAMAGE,"body_damage",-v[3]/100.0);
  set(p,Attributes.MOVEMENT_SPEED,"micro_move",SleepSettings.PENALTIES.get()&&!p.isCreative()&&!p.isSpectator()&&t.getInt("micro")>0?-.45:0);
 }
 private SleepEffects(){}
}
