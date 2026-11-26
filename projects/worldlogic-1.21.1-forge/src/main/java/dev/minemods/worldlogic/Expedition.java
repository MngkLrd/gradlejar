package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.BlockEvent;
/** Expedition consequences without new recipes, resources, mandatory equipment or extra bars. */
public final class Expedition {
 private record Rest(int ticks,boolean sheltered){}
 private static final Map<Player,Rest> SLEEP=new WeakHashMap<>();
 private static final Map<Player,Long> LAST_NOISE=new WeakHashMap<>();
 private static final Map<Mob,Long> INVESTIGATING=new WeakHashMap<>();
 static double armorLoad(ItemStack stack){
  if(stack.isEmpty())return 0;
  if(Material.item(stack,"diving_heavy_armor"))return 1;
  String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
  if(id.contains("leather"))return 0;
  if(id.contains("chainmail"))return .5;
  if(id.contains("iron")||id.contains("gold")||id.contains("diamond")||id.contains("netherite"))return 1;
  return .25;
 }
 static double armorLoad(Player p){double load=0;for(ItemStack s:p.getArmorSlots())load+=armorLoad(s);return Math.min(4,load);}
 static double shieldFraction(double fatigue){return Math.max(.55,1-Math.max(0,fatigue-70)*.015);}
 static double restRecovery(boolean sheltered){return sheltered?100:35;}
 public static void damaged(LivingDamageEvent e){
  if(!Rules.on("interrupted_meals")||e.isCanceled()||e.getAmount()<1.5f||!(e.getEntity() instanceof Player p)||!Survival.active(p))return;
  if(p.isUsingItem()&&p.getUseItem().has(DataComponents.FOOD)){p.stopUsingItem();p.displayClientMessage(Component.translatable("message.worldlogic.meal_interrupted"),true);}
 }
 public static void shield(ShieldBlockEvent e){
  if(e.isCanceled()||!Rules.on("shield_fatigue")||!Rules.on("exertion")||!(e.getEntity() instanceof Player p)||!Survival.active(p)||e.getBlockedDamage()<=0)return;
  Survival.work(p,Math.min(12,2+e.getBlockedDamage()*.8));
  double fatigue=Survival.fatigue(p);
  if(fatigue>70){e.setBlockedDamage((float)(e.getBlockedDamage()*shieldFraction(fatigue)));if(fatigue>=92)p.displayClientMessage(Component.translatable("message.worldlogic.guard_exhausted"),true);}
 }
 public static void tick(TickEvent.PlayerTickEvent.Post e){
  Player p=e.player;if(!Survival.active(p)||!(p.level() instanceof ServerLevel l))return;
  if(Rules.on("armored_diving")&&p.isEyeInFluid(FluidTags.WATER)&&!p.hasEffect(MobEffects.WATER_BREATHING)){
   double load=armorLoad(p);int interval=Math.max(3,(int)Math.round(18/Math.max(.5,load*Rules.strength("air"))));
   if(load>=1&&p.getAirSupply()>0&&l.getGameTime()%interval==0)p.setAirSupply(p.getAirSupply()-1);
   if(p.isSwimming()&&load>=2&&l.getGameTime()%20==0)Survival.work(p,load*.45);
   if(load>=3&&p.getAirSupply()<80&&p.getAirSupply()>0&&l.getGameTime()%80==0)p.displayClientMessage(Component.translatable("message.worldlogic.heavy_diving"),true);
  }
  if(Rules.on("sheltered_sleep")&&p.isSleeping()){
   BlockPos bed=p.getSleepingPos().orElse(p.blockPosition());Rest previous=SLEEP.get(p);boolean sheltered=!l.canSeeSky(bed.above())&&!l.isRainingAt(bed);
   SLEEP.put(p,new Rest(previous==null?1:Math.min(200,previous.ticks+1),sheltered&&(previous==null||previous.sheltered)));
  }else if(!p.isSleeping())SLEEP.remove(p);
 }
 public static void wake(PlayerWakeUpEvent e){
  Player p=e.getEntity();Rest rest=SLEEP.remove(p);
  if(!Rules.on("sheltered_sleep")||!Rules.on("exertion")||!Survival.active(p)||rest==null||rest.ticks<80)return;
  Survival.rest(p,restRecovery(rest.sheltered));
  p.displayClientMessage(Component.translatable(rest.sheltered?"message.worldlogic.rest_sheltered":"message.worldlogic.rest_exposed"),true);
 }
 public static void mined(BlockEvent.BreakEvent e){
  if(e.isCanceled()||!Rules.on("mining_noise")||!Survival.active(e.getPlayer())||!(e.getLevel() instanceof ServerLevel l))return;
  Player p=e.getPlayer();long now=l.getGameTime();if(p.isShiftKeyDown()||now-LAST_NOISE.getOrDefault(p,-100L)<30)return;
  var state=e.getState();if(!Material.block(state,"ricochet_stone")&&!MineInstability.rock(state)&&!Material.block(state,"ricochet_metal")&&!Material.block(state,"noisy_mining"))return;
  LAST_NOISE.put(p,now);double range=Math.min(16,8*Material.armorNoise(p)*Rules.strength("ecology"));Vec3 point=p.position();int n=0;
  for(Monster mob:l.getEntitiesOfClass(Monster.class,p.getBoundingBox().inflate(range),m->m.isAlive()&&m.getTarget()==null)){
   if(n>=6)break;if(mob.distanceToSqr(point)>range*range||INVESTIGATING.getOrDefault(mob,0L)>now)continue;n++;
   INVESTIGATING.put(mob,now+100);mob.getNavigation().moveTo(point.x,point.y,point.z,1.05);
  }
 }
 public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){SLEEP.remove(e.getEntity());LAST_NOISE.remove(e.getEntity());}
}
