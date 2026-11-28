package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
public final class ImpactDynamics {
 private record Hit(Vec3 direction,double severity,long expires,boolean disarm){}
 private static final Map<Player,Hit> HITS=new WeakHashMap<>();
 private static final Map<Player,Long> CHAIN=new WeakHashMap<>(),GRABS=new WeakHashMap<>(),ROLLS=new WeakHashMap<>();
 private static final Set<Player> GRABBED=Collections.newSetFromMap(new WeakHashMap<>());
 static double hitRisk(double damage,double mass,double fatigue,boolean injured){return Math.max(0,damage*.11+Math.min(3,mass)*.20+Math.max(0,fatigue-50)*.006+(injured?.2:0));}
 static double collisionRisk(double closing,double mass){return Math.max(0,closing)*Math.sqrt(Math.max(.25,mass));}
 static double shockRadius(double mass,double distance){return Math.min(5,Math.max(0,(distance-3)*.45+mass*.55));}
 static boolean active(Player p){return Balance.eligible(p)&&Rules.on("physical_falls");}
 public static void hit(LivingDamageEvent.Post e){
  if(!(e.getEntity() instanceof Player p)||!active(p)||!(e.getSource().getDirectEntity() instanceof LivingEntity attacker)||e.getNewDamage()<2||!p.onGround()||Balance.down(p))return;
  if(attacker instanceof Player other&&!other.canHarmPlayer(p))return;
  double risk=hitRisk(e.getNewDamage(),Material.mass(attacker),Rules.on("exertion")?Survival.fatigue(p):0,Survival.injured(p,p.level().getGameTime()))*Rules.strength("impact");
  if(!Rules.on("dynamic_knockdown")||risk<1.25)return;
  Vec3 away=p.position().subtract(attacker.position()).multiply(1,0,1);
  HITS.put(p,new Hit(away,Math.min(1.5,risk*.7),p.level().getGameTime()+5,risk>=1.8&&Rules.on("combat_disarm")));
 }
 private static void disarm(Player p){
  ItemStack held=p.getMainHandItem();if(held.isEmpty()||!Material.item(held,"disarmable")||held.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)||p.getRandom().nextFloat()>=Rules.chance("impact",.30))return;
  ItemStack drop=held.split(1);var entity=p.drop(drop,false);if(entity==null){held.grow(1);return;}
  entity.setPickUpDelay(20);p.getInventory().setChanged();p.displayClientMessage(Component.translatable("message.worldlogic.disarmed"),true);
 }
 private static boolean solid(ServerLevel l,Player p,AABB box){return l.getBlockCollisions(p,box.deflate(.001)).iterator().hasNext();}
 private static boolean footing(ServerLevel l,Player p,Vec3 offset,double depth){
  AABB b=p.getBoundingBox();double x=p.getX()+offset.x,z=p.getZ()+offset.z,y=b.minY;
  return solid(l,p,new AABB(x-.13,y-depth,z-.13,x+.13,y-.015,z+.13));
 }
 static boolean edgeRisk(ServerLevel l,Player p,Vec3 previous,Vec3 delta,double risk){
  if(risk<.45||delta.horizontalDistance()<.08)return false;
  Vec3 forward=previous.multiply(1,0,1).normalize(),current=delta.multiply(1,0,1).normalize();
  if(forward.dot(current)>.78)return false;
  Vec3 side=new Vec3(-forward.z,0,forward.x).scale(.40);
  // Require actual narrow support, not a random stumble on a flat floor.
  return footing(l,p,Vec3.ZERO,.18)&&(!footing(l,p,side,.7)||!footing(l,p,side.scale(-1),.7));
 }
 static void rollDown(ServerLevel l,Player p){
  if(!Rules.on("rolling_descent")||!p.onGround()||p.isShiftKeyDown()||Balance.canCrawl(p))return;
  long now=l.getGameTime();if(ROLLS.getOrDefault(p,0L)>now)return;
  Vec3 dir=Balance.fallDirection(p);if(dir.horizontalDistance()<.05)dir=p.getLookAngle();dir=dir.multiply(1,0,1).normalize().scale(.48);
  // Only descend a nearby shallow step. Never add impulse over an open ravine.
  if(!footing(l,p,dir,.20)&&footing(l,p,dir,1.20)&&!solid(l,p,p.getBoundingBox().move(dir.x,0,dir.z))){
   Vec3 v=p.getDeltaMovement();p.setDeltaMovement(v.x+dir.x*.23,v.y,v.z+dir.z*.23);p.hurtMarked=true;ROLLS.put(p,now+8);
  }
 }
 public static boolean hanging(Player p){return Rules.on("emergency_grab")&&GRABS.getOrDefault(p,0L)>p.level().getGameTime();}
 private static void stopGrab(Player p){if(GRABS.remove(p)!=null&&p instanceof ServerPlayer sp)RuleSync.send(sp);}
 private static void grab(ServerLevel l,Player p){
  long now=l.getGameTime();
  if(!Rules.on("emergency_grab")||!active(p)||Balance.down(p)||p.onGround()){stopGrab(p);if(p.onGround())GRABBED.remove(p);return;}
  boolean hands=p.getMainHandItem().isEmpty()||p.getOffhandItem().isEmpty();
  Vec3 direction=p.getLookAngle().multiply(1,0,1).normalize();
  Vec3 ahead=direction.scale(.43);AABB b=p.getBoundingBox();double x=p.getX()+ahead.x,z=p.getZ()+ahead.z,y=b.minY;
  boolean lip=solid(l,p,new AABB(x-.15,y+.65,z-.15,x+.15,y+1.10,z+.15))&&!solid(l,p,new AABB(x-.15,y+1.12,z-.15,x+.15,y+1.9,z+.15));
  if(hanging(p)){
   if(!p.isShiftKeyDown()||!hands||!lip){stopGrab(p);return;}
   p.setDeltaMovement(direction.x*.025,-.025,direction.z*.025);p.hurtMarked=true;p.fallDistance=0;return;
  }
  if(GRABS.containsKey(p))stopGrab(p);
  if(GRABBED.contains(p)||!hands||!p.isShiftKeyDown()||!lip||p.getDeltaMovement().y>-.20||p.fallDistance<1||Survival.fatigue(p)>=90)return;
  GRABBED.add(p);GRABS.put(p,now+14);p.fallDistance=0;p.setDeltaMovement(direction.x*.025,-.025,direction.z*.025);p.hurtMarked=true;Survival.work(p,7);
  p.displayClientMessage(Component.translatable("message.worldlogic.ledge_grab"),true);if(p instanceof ServerPlayer sp)RuleSync.send(sp);
 }
 public static void tick(PlayerTickEvent.Post e){
  Player p=e.getEntity();if(!(p.level() instanceof ServerLevel l))return;long now=l.getGameTime();
  if(!active(p)){stopGrab(p);GRABBED.remove(p);HITS.remove(p);ROLLS.remove(p);return;}
  grab(l,p);
  Hit hit=HITS.get(p);
  if(hit!=null){if(now>hit.expires)HITS.remove(p);else if(p.onGround()){HITS.remove(p);if(Balance.force(p,hit.direction,"hit",hit.severity)&&hit.disarm)disarm(p);}}
  if(!Rules.on("chain_collision")||!p.onGround()||Balance.down(p)||!p.isSprinting()||CHAIN.getOrDefault(p,0L)>now)return;
  Vec3 velocity=Balance.recentMotion(p);if(velocity.horizontalDistance()<.22)return;
  int budget=8;
  for(LivingEntity target:l.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(.22),t->t!=p&&t.isAlive()&&t.onGround()&&t.isPushable()&&!t.isPassenger())){
   if(budget--<=0)break;
   if(target instanceof Player other&&(!active(other)||!p.canHarmPlayer(other)||other.isBlocking()))continue;
   Vec3 dir=target.position().subtract(p.position()).multiply(1,0,1);if(dir.lengthSqr()<.01)continue;dir=dir.normalize();
   double closing=velocity.subtract(target.getDeltaMovement()).dot(dir);
   double risk=collisionRisk(closing,Material.mass(p)+Condition.burden(p)*.12);if(risk<.29)continue;
   if(target instanceof Player other){if(!Balance.force(other,dir,"chain",Math.min(1.2,risk*2)))continue;}
   else{double push=Math.min(.30,risk*.25/Math.sqrt(Material.mass(target)));target.push(dir.x*push,.04,dir.z*push);target.hurtMarked=true;}
   p.setSprinting(false);Survival.work(p,2);CHAIN.put(p,now+30);if(target instanceof Player other)CHAIN.put(other,now+30);break;
  }
 }
 public static void landing(LivingFallEvent e){
  if(e.isCanceled()||!Rules.on("landing_shockwave")||e.getDistance()<4||e.getDamageMultiplier()<=0||!(e.getEntity().level() instanceof ServerLevel l))return;
  LivingEntity entity=e.getEntity();if(entity instanceof Player p&&!Survival.active(p))return;
  double mass=Material.mass(entity)+(entity instanceof Player p?Condition.burden(p)*.22+Expedition.armorLoad(p)*.22:0);
  if(mass<1.8)return;double radius=shockRadius(mass,e.getDistance());Vec3 center=entity.position();
  int n=0;for(Animal a:l.getEntitiesOfClass(Animal.class,entity.getBoundingBox().inflate(radius),a->a!=entity&&a.isAlive())){
   if(n++>=8)break;Vec3 d=a.position().subtract(center).multiply(1,0,1).normalize().scale(4);a.getNavigation().moveTo(a.getX()+d.x,a.getY(),a.getZ()+d.z,1.2);
  }
  BlockPos ground=entity.getOnPos();var floor=l.getBlockState(ground);
  if(!floor.isAir())l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,floor),center.x,center.y+.05,center.z,22,.5,.05,.5,.08);
  // Landing vibration reaches existing unstable gravel machinery. Fragile blocks get visible vibration only.
  Structure.landingVibration(l,ground,entity);
 }
 static void adminRestore(Player p){HITS.remove(p);CHAIN.remove(p);GRABS.remove(p);GRABBED.remove(p);ROLLS.remove(p);}
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){adminRestore(e.getEntity());}
}
