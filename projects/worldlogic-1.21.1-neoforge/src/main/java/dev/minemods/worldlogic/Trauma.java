package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
public final class Trauma {
 private static final String ARM="worldlogic_arm_until",LOAD="worldlogic_impact_load";
 private static final Map<Player,Long> DODGE=new WeakHashMap<>(),CONTACT=new WeakHashMap<>(),BLAST=new WeakHashMap<>();
 private static final Map<Player,Vec3> MOTION=new WeakHashMap<>();
 private static final Map<Player,Integer> GUARD=new WeakHashMap<>();
 static long clock(Player p){return p.level() instanceof ServerLevel l?l.getServer().overworld().getGameTime():p.level().getGameTime();}
 static double armFactor(boolean injured){return injured?.75:1;}
 static double guardRisk(double blocked,double mass,double fatigue){return blocked*.12+Math.min(3,mass)*.15+Math.max(0,fatigue-50)*.009;}
 static int aggravation(double fatigue){return (int)Math.min(18,6+Math.max(0,fatigue-40)*.2);}
 static double load(Player p){return Rules.on("repeated_impact")?Math.max(0,Math.min(100,p.getPersistentData().getDouble(LOAD))):0;}
 static double balancePenalty(Player p){return load(p)*.00035;}
 static boolean arm(Player p){return Survival.active(p)&&Rules.on("shoulder_impact")&&p.getPersistentData().getLong(ARM)>clock(p);}
 static double mining(Player p){return armFactor(arm(p));}
 static void accumulate(Player p,double amount){if(Rules.on("repeated_impact")&&Survival.active(p))p.getPersistentData().putDouble(LOAD,Math.min(100,load(p)+amount));}
 static void impact(Player p,double severity,String reason){
  accumulate(p,Math.min(18,severity*9));
  if(!Rules.on("shoulder_impact")||severity<.75||!reason.equals("collision"))return;
  long end=clock(p)+(long)Math.min(900,120+severity*220);
  boolean had=arm(p);p.getPersistentData().putLong(ARM,Math.max(end,p.getPersistentData().getLong(ARM)));
  if(!had)p.displayClientMessage(Component.translatable("message.worldlogic.shoulder_injury"),true);
  if(p instanceof ServerPlayer sp)RuleSync.send(sp);
 }
 public static void shield(LivingShieldBlockEvent e){if(!e.getBlocked())return;
  if(e.isCanceled()||!Rules.on("broken_guard")||!(e.getEntity() instanceof Player p)||!Survival.active(p)||e.getBlockedDamage()<4||!p.getUseItem().is(Items.SHIELD))return;
  double mass=e.getDamageSource().getDirectEntity() instanceof LivingEntity attacker?Material.mass(attacker):.5;
  double fatigue=Rules.on("exertion")?Survival.fatigue(p):0;
  double risk=guardRisk(e.getBlockedDamage(),mass,fatigue)*Rules.strength("impact");
  if(risk<1.4)return;
  int cooldown=(int)Math.min(60,20+risk*10);
  e.setBlockedDamage(e.getBlockedDamage()*.75f);
  // The vanilla shield durability calculation runs AFTER this event. Apply the cooldown next tick.
  GUARD.merge(p,cooldown,Math::max);
  accumulate(p,8);Survival.work(p,5);
  p.displayClientMessage(Component.translatable("message.worldlogic.guard_broken"),true);
 }
 public static void damage(LivingDamageEvent.Post e){
  if(e.getNewDamage()<1||!(e.getEntity() instanceof Player p)||!Survival.active(p)||!p.isAlive())return;
  accumulate(p,Math.min(20,e.getNewDamage()*1.7));
  if(!Rules.on("blast_concussion")||!e.getSource().is(DamageTypeTags.IS_EXPLOSION)||e.getNewDamage()<3)return;
  int duration=(int)Math.min(100,30+e.getNewDamage()*5*Rules.strength("impact"));
  long end=p.level().getGameTime()+duration;BLAST.put(p,end);
  p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,Math.min(50,duration),0,false,false,false));
  p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,duration,0,false,false,false));
  p.displayClientMessage(Component.translatable("message.worldlogic.concussion"),true);
  Survival.work(p,6);
 }
 public static void melee(LivingIncomingDamageEvent e){
  if(!e.isCanceled()&&e.getSource().getDirectEntity() instanceof Player p&&Survival.active(p)&&arm(p))e.setAmount(e.getAmount()*.85f);
 }
 static double penalty(Player p,long now){return Math.max(Rules.on("desperate_dodge")&&DODGE.getOrDefault(p,0L)>now?.15:0,Rules.on("blast_concussion")&&BLAST.getOrDefault(p,0L)>now?.14:0);}
 private static void mobContact(ServerLevel l,Player p,long now){
  if(!Rules.on("mob_weight")||!Balance.eligible(p)||!p.onGround()||Balance.down(p)||CONTACT.getOrDefault(p,0L)>now||now%2!=0)return;
  int n=0;
  for(Animal a:l.getEntitiesOfClass(Animal.class,p.getBoundingBox().inflate(.20),a->a.isAlive()&&a.onGround()&&!a.isPassenger()&&a.isPushable())){
   if(n++>=8)break;
   double mass=Material.mass(a);if(mass<1.2||a.getDeltaMovement().horizontalDistance()<.12)continue;
   Vec3 direction=p.position().subtract(a.position()).multiply(1,0,1);if(direction.lengthSqr()<.01)continue;direction=direction.normalize();
   double closing=a.getDeltaMovement().subtract(Balance.recentMotion(p)).dot(direction);
   double risk=ImpactDynamics.collisionRisk(closing,mass)*Rules.strength("impact");
   if(risk<.30||p.isShiftKeyDown()&&p.isBlocking())continue;
   if(Balance.force(p,direction,"animal",Math.min(1.2,risk*2))){CONTACT.put(p,now+60);break;}
  }
 }
 public static void tick(PlayerTickEvent.Post e){
  Player p=e.getEntity();if(!Survival.active(p)||!p.isAlive()||!(p.level() instanceof ServerLevel l)){MOTION.remove(p);GUARD.remove(p);return;}
  Integer guard=GUARD.remove(p);if(guard!=null&&Rules.on("broken_guard")){if(p.getUseItem().is(Items.SHIELD))p.stopUsingItem();p.getCooldowns().addCooldown(Items.SHIELD,guard);}
  long now=l.getGameTime();double fatigue=Rules.on("exertion")?Survival.fatigue(p):0;
  if(now%20==0){
   if(Rules.on("injury_aggravation")&&Survival.injured(p,now)&&p.isSprinting()&&p.onGround())Survival.aggravate(p,aggravation(fatigue));
   if(Rules.on("repeated_impact")&&load(p)>0&&!p.isSprinting()&&!Balance.down(p)){
    double decay=p.onGround()&&Balance.recentMotion(p).horizontalDistance()<.045?2:.5;
    p.getPersistentData().putDouble(LOAD,Math.max(0,load(p)-decay));
   }
  }
  Vec3 current=Balance.recentMotion(p),old=MOTION.put(p,current);
  if(Rules.on("desperate_dodge")&&old!=null&&Balance.eligible(p)&&!Balance.down(p)&&p.onGround()&&p.isSprinting()&&!p.isShiftKeyDown()&&DODGE.getOrDefault(p,0L)<=now&&old.horizontalDistance()>.22&&current.horizontalDistance()>.15){
   double turn=old.multiply(1,0,1).normalize().dot(current.multiply(1,0,1).normalize());
   if(turn<.15){DODGE.put(p,now+20);Survival.work(p,3);
    if((fatigue>=75||Survival.injured(p,now))&&Balance.slick(l,p))Balance.force(p,current,"dodge",.7);
   }
  }
  mobContact(l,p,now);
 }
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){Player p=e.getEntity();MOTION.remove(p);DODGE.remove(p);CONTACT.remove(p);BLAST.remove(p);GUARD.remove(p);}
}
