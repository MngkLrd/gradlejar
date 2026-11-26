package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
public final class CombatConsequences {
 private record Recovery(long until,long staggerUntil){}
 private static final Map<Player,Recovery> RECOVERY=new WeakHashMap<>();
 private static final String HEAL_UNTIL="worldlogic_combat_heal_until";
 public static boolean canNaturallyHeal(Player p){return !Rules.on("combat_recovery")||!Survival.active(p)||!(p.level() instanceof ServerLevel l)||p.getPersistentData().getLong(HEAL_UNTIL)<=l.getServer().overworld().getGameTime();}
 static int healDelay(float damage,double strength){return (int)Math.min(240,Math.round((60+Math.min(12,damage)*8)*strength));}
 static int cargoLoss(float damage,double strength){return damage<5?0:Math.min(3,Math.max(1,(int)Math.floor((damage-3)*strength/3)));}
 static double armorWork(double load){return 1+Math.max(0,Math.min(4,load))*.18;}

 public static void damage(LivingDamageEvent e){
  if(e.isCanceled()||e.getAmount()<=0||!(e.getEntity() instanceof Player p)||!Survival.active(p)||!(p.level() instanceof ServerLevel l))return;
  long now=l.getGameTime();float amount=e.getAmount();var old=RECOVERY.getOrDefault(p,new Recovery(0,0));long until=old.until,stagger=old.staggerUntil;
  if(Rules.on("combat_recovery")&&amount>=1){long end=l.getServer().overworld().getGameTime()+healDelay(amount,Rules.strength("impact"));p.getPersistentData().putLong(HEAL_UNTIL,Math.max(end,p.getPersistentData().getLong(HEAL_UNTIL)));}
  if(Rules.on("dangerous_retreat")&&amount>=2&&e.getSource().getDirectEntity() instanceof LivingEntity attacker&&(p.isSprinting()||Body.state(p).lastSprint>0&&now-Body.state(p).lastSprint<=4)){
   Vec3 facing=p.getLookAngle().multiply(1,0,1).normalize(),toAttacker=attacker.position().subtract(p.position()).multiply(1,0,1).normalize();
   if(facing.dot(toAttacker)<-.45){stagger=Math.max(stagger,now+16);p.setSprinting(false);Survival.work(p,5);p.displayClientMessage(Component.translatable("message.worldlogic.retreat_hit"),true);}
  }
  if(Rules.on("unstable_climbing")&&amount>=2&&p.onClimbable()&&e.getSource().getEntity()!=null){
   Vec3 away=p.position().subtract(e.getSource().getEntity().position()).multiply(1,0,1).normalize();
   p.setDeltaMovement(away.x*.22,-.25,away.z*.22);p.hurtMarked=true;stagger=Math.max(stagger,now+12);
   p.displayClientMessage(Component.translatable("message.worldlogic.climb_hit"),true);
  }
  if(until>now||stagger>now)RECOVERY.put(p,new Recovery(until,stagger));
  if(e.getSource().is(DamageTypeTags.IS_FALL)){
   if(Rules.on("fragile_cargo"))breakCargo(l,p,amount);
   if(Rules.on("environmental_wear")&&amount>=4)wear(p,EquipmentSlot.FEET,Math.min(8,1+(int)(amount/2)));
  }else if(Rules.on("environmental_wear")&&amount>=1&&(e.getSource().is(DamageTypeTags.IS_FIRE)||e.getSource().is(DamageTypes.CACTUS))){
   if(l.random.nextFloat()<.25f)wear(p,EquipmentSlot.LEGS,1);
  }
 }
 private static void wear(Player p,EquipmentSlot slot,int amount){
  ItemStack stack=p.getItemBySlot(slot);if(stack.isDamageableItem()&&!stack.isEmpty())stack.hurtAndBreak(amount,p,slot);
 }
 private static void breakCargo(ServerLevel l,Player p,float damage){
  int count=cargoLoss(damage,Rules.strength("impact"));if(count<=0)return;
  int start=l.random.nextInt(p.getInventory().getContainerSize()),broken=0;
  for(int i=0;i<p.getInventory().getContainerSize()&&count>0;i++){
   ItemStack s=p.getInventory().getItem((start+i)%p.getInventory().getContainerSize());
   if(!Material.item(s,"fragile_cargo"))continue;int n=Math.min(count,s.getCount());s.shrink(n);count-=n;broken+=n;
  }
  if(broken>0){p.getInventory().setChanged();l.playSound(null,p.blockPosition(),SoundEvents.GLASS_BREAK,SoundSource.PLAYERS,.65f,1.0f);l.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()),p.getX(),p.getY()+.25,p.getZ(),7,.2,.1,.2,.02);p.displayClientMessage(Component.translatable("message.worldlogic.cargo_broken",broken),true);}
 }
 static double movementPenalty(Player p,long now){Recovery r=RECOVERY.get(p);return r!=null&&r.staggerUntil>now?.22:0;}
 public static void tick(TickEvent.PlayerTickEvent.Post e){
  Player p=e.player;if(!Survival.active(p))return;long now=p.level().getGameTime();Recovery r=RECOVERY.get(p);
  if(r==null)return;if(r.staggerUntil>now)p.setSprinting(false);
  if(r.until<=now&&r.staggerUntil<=now)RECOVERY.remove(p);
 }
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){RECOVERY.remove(e.getEntity());}
}
