package dev.minemods.worldlogic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Core stamina consequences. Only own transient attribute IDs; never remove a potion effect. */
public final class ExertionModifiers {
 private ExertionModifiers(){}
 private static final ResourceLocation ATTACK=ResourceLocation.fromNamespaceAndPath("worldlogic","exertion_attack_speed");
 private static final ResourceLocation MINING=ResourceLocation.fromNamespaceAndPath("worldlogic","exertion_block_break_speed");
 public record Factors(double movement,double attack,double mining,boolean sprintAllowed){}
 public static Factors factors(double fatigue){
  double f=Double.isFinite(fatigue)?Math.max(0,Math.min(100,fatigue)):0;
  double load=Math.max(0,(f-40)/60);
  return new Factors(1-.30*load,1-.35*load,1-.45*load,f<92);
 }
 public static Factors current(Player p){
  return factors(Survival.active(p)&&Rules.on("exertion")?Survival.fatigue(p):0);
 }
 static void apply(Player p){
  var f=current(p);
  set(p.getAttribute(Attributes.ATTACK_SPEED),ATTACK,f.attack());
  set(p.getAttribute(Attributes.BLOCK_BREAK_SPEED),MINING,f.mining());
  if(!f.sprintAllowed()&&p.isSprinting())p.setSprinting(false);
 }
 static void clear(Player p){
  set(p.getAttribute(Attributes.ATTACK_SPEED),ATTACK,1);
  set(p.getAttribute(Attributes.BLOCK_BREAK_SPEED),MINING,1);
 }
 private static void set(AttributeInstance attr,ResourceLocation id,double factor){
  if(attr==null)return;
  var old=attr.getModifier(id);double amount=factor-1;
  if(old!=null&&Math.abs(old.amount()-amount)<1e-8)return;
  if(old!=null)attr.removeModifier(id);
  if(amount< -1e-8)attr.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
 }
 static void verify(Player p){
  double old=Survival.fatigue(p);boolean sprint=p.isSprinting();
  var attack=p.getAttribute(Attributes.ATTACK_SPEED);var mining=p.getAttribute(Attributes.BLOCK_BREAK_SPEED);
  if(attack==null||mining==null)throw new IllegalStateException("missing exertion attributes");
  try{
   clear(p);
   var existing=p.getEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN);
   p.getPersistentData().putDouble("worldlogic_exertion",100);
   p.setSprinting(true);apply(p);
   if(p.isSprinting()||Math.abs(attack.getModifier(ATTACK).amount()+.35)>1e-8||Math.abs(mining.getModifier(MINING).amount()+.45)>1e-8)throw new IllegalStateException("exhausted attributes");
   apply(p);
   if(p.getEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN)!=existing)throw new IllegalStateException("exertion must not mutate mining fatigue potion");
   p.getPersistentData().putDouble("worldlogic_exertion",70);apply(p);
   if(Math.abs(attack.getModifier(ATTACK).amount()+.175)>1e-8||Math.abs(mining.getModifier(MINING).amount()+.225)>1e-8)throw new IllegalStateException("progressive exertion");
   p.getPersistentData().putDouble("worldlogic_exertion",0);apply(p);
   if(attack.getModifier(ATTACK)!=null||mining.getModifier(MINING)!=null)throw new IllegalStateException("recovery clears only exertion");
   if(factors(0).movement()!=1||Math.abs(factors(100).movement()-.7)>1e-8)throw new IllegalStateException("movement factor");
  }finally{p.getPersistentData().putDouble("worldlogic_exertion",old);p.setSprinting(sprint);apply(p);}
 }
}
