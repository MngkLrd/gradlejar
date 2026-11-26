package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
public final class TravelPhysics {
 private record Step(Vec3 position,Vec3 velocity,boolean sprint,long time){}
 private static final Map<Player,Step> STEPS=new WeakHashMap<>();
 private static final Map<Player,Long> IMPACT=new WeakHashMap<>(),SWING=new WeakHashMap<>(),TENSION=new WeakHashMap<>();
 static double boatFactor(double load){return Math.max(.55,1-Math.max(0,load-2)*.12);}
 static double crashDamage(double speed){return Math.max(0,Math.min(5,(speed-.32)*10));}
 static double cargoWeight(ItemStack s){return s.isEmpty()?0:Material.weight(s)*s.getCount()/64.0;}
 static double load(Boat b){
  double result=0;
  for(Entity x:b.getPassengers()){result+=Material.mass(x);if(x instanceof Player p)result+=Expedition.armorLoad(p)*.18;}
  if(b instanceof ChestBoat c){for(int i=0;i<c.getContainerSize();i++)result+=cargoWeight(c.getItem(i))*.32;}
  return result;
 }
 public static double boatLimit(Player p){
  if(!Rules.on("boat_overload")||!Survival.active(p)||!(p.getVehicle() instanceof Boat b)||b.getControllingPassenger()!=p)return 0;
  double factor=boatFactor(load(b));return factor>=.999?0:.42*factor;
 }
 public static void capBoat(Boat b,double limit){
  if(limit<=0||!b.isInWater())return;Vec3 v=b.getDeltaMovement();
  if(v.horizontalDistance()>limit){double scale=limit/v.horizontalDistance();b.setDeltaMovement(v.x*scale,v.y,v.z*scale);}
 }
 public static void attack(AttackEntityEvent e){
  Player p=e.getEntity();if(e.isCanceled()||!Rules.on("heavy_swing")||!Survival.active(p)||p.getAttackStrengthScale(.5f)<.85f||!Material.item(p.getMainHandItem(),"heavy_weapons")||!(e.getTarget() instanceof LivingEntity))return;
  long now=p.level().getGameTime();SWING.put(p,now+(long)(12*Rules.strength("impact")));Survival.work(p,3.5);p.setSprinting(false);
 }
 static double penalty(Player p,long now){double a=Rules.on("heavy_swing")&&SWING.getOrDefault(p,0L)>now?.16:0;double b=Rules.on("leash_resistance")&&TENSION.getOrDefault(p,0L)>now?.18:0;return Math.max(a,b);}
 public static void animal(LivingEvent.LivingTickEvent e){
  if(!Rules.on("leash_resistance")||!(e.getEntity() instanceof Animal a)||!(a.level() instanceof ServerLevel l)||a.tickCount%5!=0||!(a.getLeashHolder() instanceof Player p)||!Survival.active(p))return;
  double distance=a.distanceTo(p);if(distance<3||distance>9||!p.isSprinting())return;
  TENSION.put(p,l.getGameTime()+8);Vec3 direction=a.position().subtract(p.position()).multiply(1,0,1).normalize();
  if(a.getDeltaMovement().dot(direction)<0)a.setDeltaMovement(a.getDeltaMovement().multiply(.85,1,.85));
  if(a.tickCount%40==0){Survival.work(p,Math.min(2,Material.mass(a)));p.displayClientMessage(Component.translatable("message.worldlogic.leash_tension"),true);}
 }
 public static void tick(TickEvent.PlayerTickEvent.Post e){
  Player p=e.player;if(!Survival.active(p)||!(p.level() instanceof ServerLevel l))return;long now=l.getGameTime();
  Step old=STEPS.get(p);Vec3 delta=old==null?Vec3.ZERO:p.position().subtract(old.position);
  if(Rules.on("heavy_swing")&&SWING.getOrDefault(p,0L)>now)p.setSprinting(false);
  boolean valid=old!=null&&old.time==now-1&&delta.lengthSqr()<4&&!p.isPassenger()&&!p.isFallFlying()&&!p.onClimbable()&&!p.isInWater()&&!p.getAbilities().flying;
  if(!Rules.on("physical_falls")&&Rules.on("wall_collision")&&valid&&old.sprint&&old.velocity.horizontalDistance()>.38&&delta.horizontalDistance()<old.velocity.horizontalDistance()*.35&&IMPACT.getOrDefault(p,0L)<=now){
   Vec3 ahead=old.velocity.multiply(1,0,1).normalize().scale(.22);
   // Detect an actual blocking collision, not releasing the sprint key or climbing a step.
   if(!l.noCollision(p,p.getBoundingBox().move(ahead.x,0,ahead.z))&& !l.noCollision(p,p.getBoundingBox().move(ahead.x,.6,ahead.z))){
    float damage=(float)(crashDamage(old.velocity.horizontalDistance())*Rules.strength("impact"));
    if(damage>=.5){IMPACT.put(p,now+30);p.hurt(l.damageSources().generic(),damage);p.setSprinting(false);Survival.work(p,4);l.playSound(null,p.blockPosition(),SoundEvents.PLAYER_HURT,SoundSource.PLAYERS,.45f,.85f);}
   }
  }
  STEPS.put(p,new Step(p.position(),valid?delta:Vec3.ZERO,p.isSprinting(),now));
  if(Rules.on("boat_overload")&&p.getVehicle() instanceof Boat b&&b.getControllingPassenger()==p&&b.isInWater()&&now%4==0){
   double limit=boatLimit(p),factor=limit>0?limit/.42:1;capBoat(b,limit);
   if(factor<.85&&now%100==0)p.displayClientMessage(Component.translatable("message.worldlogic.boat_loaded",Math.round(factor*100)),true);
  }
 }
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){Player p=e.getEntity();STEPS.remove(p);IMPACT.remove(p);SWING.remove(p);TENSION.remove(p);}
}
