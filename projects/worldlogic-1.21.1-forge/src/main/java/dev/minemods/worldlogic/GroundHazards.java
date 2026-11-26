package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
public final class GroundHazards {
 private static final Map<Player,Long> LANDING=new WeakHashMap<>(),SLIP=new WeakHashMap<>(),GRIP=new WeakHashMap<>();
 static int recoveryExtra(double fatigue,double armor,double burden){return (int)Math.min(28,Math.max(0,fatigue-35)*.22+armor*2+burden*1.2);}
 static int recoveryExtra(Player p){return recoveryExtra(Rules.on("exertion")?Survival.fatigue(p):0,Expedition.armorLoad(p),Condition.burden(p));}
 static double landingRisk(double distance,double load,boolean injured,double fatigue){return Math.max(0,distance-2.5)*(.30+Math.min(1,load*.15))+(injured?.6:0)+Math.max(0,fatigue-60)*.012;}
 public static void landed(LivingFallEvent e){
  if(e.isCanceled()||!Rules.on("heavy_landing")||!Rules.on("physical_falls")||e.getDistance()<3||e.getDamageMultiplier()<=0||!(e.getEntity() instanceof Player p)||!Survival.active(p)||p.isShiftKeyDown())return;
  double load=Condition.burden(p)+Expedition.armorLoad(p)*.65;
  double risk=landingRisk(e.getDistance(),load,Survival.injured(p,p.level().getGameTime()),Rules.on("exertion")?Survival.fatigue(p):0);
  if(load>=1.5&&risk>=1.2)LANDING.put(p,p.level().getGameTime()+4);
 }
 public static void dropOnImpact(Player p,double severity,String reason){
  if(!Rules.on("unstable_carrying")||severity<.85||!(reason.equals("collision")||reason.equals("landing"))||p.isShiftKeyDown())return;
  if(p.getMainHandItem().isEmpty()||!Material.item(p.getMainHandItem(),"loose_hand_cargo")||p.getMainHandItem().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME))return;
  if(p.getRandom().nextFloat()>=Rules.chance("impact",.25))return;
  ItemStack held=p.getMainHandItem();ItemStack dropped=held.split(1);
  var entity=p.drop(dropped,false);if(entity==null){held.grow(1);return;}entity.setPickUpDelay(15);
  p.getInventory().setChanged();p.displayClientMessage(Component.translatable("message.worldlogic.dropped_grip"),true);
 }
 static double wetGrip(Player p){return Rules.on("tool_grip")&&Material.item(p.getMainHandItem(),"heavy_weapons")&&Rules.on("wet_gear")?Body.state(p).wet/100.0:0;}
 public static void attack(AttackEntityEvent e){
  Player p=e.getEntity();if(e.isCanceled()||!Survival.active(p)||p.getAttackStrengthScale(.5f)<.8||wetGrip(p)<.65)return;
  if(p.getRandom().nextFloat()<Rules.chance("movement",.18)){GRIP.put(p,p.level().getGameTime()+12);Survival.work(p,2);p.displayClientMessage(Component.translatable("message.worldlogic.wet_grip"),true);}
 }
 public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent e){
  if(e.isCanceled()||!Rules.on("tool_grip")||!(e.getSource().getDirectEntity() instanceof Player p)||!Survival.active(p)||!Material.item(p.getMainHandItem(),"heavy_weapons")||GRIP.getOrDefault(p,0L)<=p.level().getGameTime())return;
  e.setAmount(e.getAmount()*.85f);
 }
 static double penalty(Player p,long now){return Rules.on("tool_grip")&&GRIP.getOrDefault(p,0L)>now?.12:0;}
 public static void tick(TickEvent.PlayerTickEvent.Post e){
  Player p=e.player;if(!Survival.active(p)||!(p.level() instanceof ServerLevel l)||!p.isAlive()){LANDING.remove(p);SLIP.remove(p);return;}
  long now=l.getGameTime();Long land=LANDING.get(p);
  if(land!=null){if(now>land)LANDING.remove(p);else if(p.onGround()){LANDING.remove(p);Balance.forceLanding(p,1.05);}}
  if(!Rules.on("desperate_climb")||!p.onClimbable()||p.isPassenger()||p.getAbilities().flying||p.isInWater()){SLIP.remove(p);return;}
  Long slipping=SLIP.get(p);if(slipping!=null&&slipping>now){Vec3 v=p.getDeltaMovement();p.setDeltaMovement(v.x*.65,Math.min(v.y,-.18),v.z*.65);p.hurtMarked=true;return;}
  if(slipping!=null&&now<slipping+100)return;
  double fatigue=Rules.on("exertion")?Survival.fatigue(p):0;
  boolean injured=Survival.injured(p,now);
  if(p.tickCount%20==0&&!p.onGround()&&!p.isShiftKeyDown()&&p.getDeltaMovement().y>.025&&(fatigue>=80||injured&&fatigue>=50)){
   SLIP.put(p,now+8);p.displayClientMessage(Component.translatable("message.worldlogic.climb_slip"),true);
   Vec3 v=p.getDeltaMovement();p.setDeltaMovement(v.x,-.20,v.z);p.hurtMarked=true;Survival.work(p,3);
  }
 }
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){Player p=e.getEntity();LANDING.remove(p);SLIP.remove(p);GRIP.remove(p);}
}
