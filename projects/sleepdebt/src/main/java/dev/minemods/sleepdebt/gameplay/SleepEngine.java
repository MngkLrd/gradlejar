package dev.minemods.sleepdebt.gameplay;

import dev.minemods.sleepdebt.SleepSettings;
import dev.minemods.sleepdebt.network.SleepStatusPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Long-term player state. Saves under the legacy key for world migration. */
public final class SleepEngine {
 private static final String KEY="sleepdebt_v2";
 private static final int MAX=1000;
 private static int clamp(int v){return Math.max(0,Math.min(MAX,v));}
 private static CompoundTag data(ServerPlayer p){return p.getPersistentData().getCompound(KEY);}
 private static void save(ServerPlayer p,CompoundTag state){p.getPersistentData().put(KEY,state);}
 public static int[] history(ServerPlayer p){return data(p).getIntArray("history");}
 public static int getTolerance(ServerPlayer p){return data(p).getInt("tolerance");}
 public static int getMaskPower(ServerPlayer p){return data(p).getInt("maskPower");}
 public static int getMaskTicks(ServerPlayer p){return data(p).getInt("mask");}
 public static int getCrashTicks(ServerPlayer p){return data(p).getInt("crash");}
 public static int[] activity(ServerPlayer p){return data(p).getIntArray("activity");}
 public static int[] penalties(ServerPlayer p){return SleepEffects.percentages(p,data(p));}
 private static void sync(ServerPlayer p,CompoundTag state){
  PacketDistributor.sendToPlayer(p,new SleepStatusPayload(
   state.getInt("debt"),state.getInt("mask"),state.getInt("crash"),
   state.getInt("caffeine"),state.getInt("fatigue"),state.getInt("stress")));
 }
 public static int getDebt(ServerPlayer p){return data(p).getInt("debt");}
 public static int getFatigue(ServerPlayer p){return data(p).getInt("fatigue");}
 public static int getStress(ServerPlayer p){return data(p).getInt("stress");}
 public static int getCaffeine(ServerPlayer p){return data(p).getInt("caffeine");}
 public static int getLastQuality(ServerPlayer p){return data(p).getInt("lastSleepQuality");}
 public static int getLastRecovery(ServerPlayer p){return data(p).getInt("lastSleepRecovery");}
 public static int getLastReasons(ServerPlayer p){return data(p).getInt("lastSleepReasons");}
 public static int getCurrentQuality(ServerPlayer p){
  return RestEnvironment.evaluate(p,p.blockPosition()).quality();
 }
 public static int daysSinceSleep(ServerPlayer p){
  CompoundTag state=data(p);
  if(!state.contains("lastSleepAt"))return 0;
  long elapsed=Math.max(0,p.serverLevel().getGameTime()-state.getLong("lastSleepAt"));
  return (int)Math.min(99,elapsed/24000L);
 }

 public static void setNeeds(ServerPlayer p,int debt,int fatigue,int stress){
  CompoundTag t=data(p);
  t.putInt("debt",clamp(debt));
  t.putInt("fatigue",clamp(fatigue));
  t.putInt("stress",clamp(stress));
  save(p,t);SleepEffects.apply(p,t);sync(p,t);
 }
 public static void addStress(ServerPlayer p,int amount){
  CompoundTag t=data(p);
  t.putInt("stress",clamp(t.getInt("stress")+amount));
  save(p,t);sync(p,t);
 }
 public static void addFatigue(ServerPlayer p,int amount){
  CompoundTag t=data(p);
  t.putInt("fatigue",clamp(t.getInt("fatigue")+amount));
  save(p,t);
 }
 public static void soothe(ServerPlayer p,int stress,int fatigue){
  CompoundTag t=data(p);
  if(t.getInt("stress")==0&&t.getInt("fatigue")==0)return;
  t.putInt("stress",clamp(t.getInt("stress")-stress));
  t.putInt("fatigue",clamp(t.getInt("fatigue")-fatigue));
  save(p,t);
 }
 private static void exert(CompoundTag t,int category,int amount){
  if(amount<=0)return;
  int[] a=java.util.Arrays.copyOf(t.getIntArray("activity"),6);
  a[category]=(int)Math.min(1000000L,(long)a[category]+amount);
  t.putIntArray("activity",a);
  t.putInt("fatigue",clamp(t.getInt("fatigue")+amount));
 }
 private static boolean survival(ServerPlayer p){return !p.isCreative()&&!p.isSpectator();}
 public static void onBlockBreak(BlockEvent.BreakEvent e){
  if(e.getPlayer() instanceof ServerPlayer p&&survival(p)&&!e.isCanceled()){
   float hardness=e.getState().getDestroySpeed(e.getLevel(),e.getPos());
   CompoundTag t=data(p);
   exert(t,3,Math.max(1,Math.min(12,2+(int)Math.max(0,hardness))));
   save(p,t);
  }
 }
 public static void onAttack(AttackEntityEvent e){
  if(e.getEntity() instanceof ServerPlayer p&&survival(p)&&!e.isCanceled()&&p.getAttackStrengthScale(0)>.8f){
   CompoundTag t=data(p);exert(t,4,7);
   t.putInt("stress",clamp(t.getInt("stress")+4));save(p,t);
  }
 }
 public static void onJump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent e){
  if(e.getEntity() instanceof ServerPlayer p&&survival(p)){
   CompoundTag t=data(p);exert(t,2,2+p.getArmorValue()/10);save(p,t);
  }
 }
 public static void incense(ServerPlayer p){
  CompoundTag t=data(p);long now=p.serverLevel().getGameTime();
  if(t.contains("incenseAt")&&now-t.getLong("incenseAt")<40)return;
  t.putLong("incenseAt",now);save(p,t);soothe(p,6,4);
 }
 private static void historySample(ServerPlayer p,CompoundTag t){
  int[] old=t.getIntArray("history");int length=Math.min(old.length/5*5,235);
  int[] next=new int[length+5];
  System.arraycopy(old,old.length-length,next,0,length);
  next[length]=t.getInt("debt");next[length+1]=t.getInt("fatigue");next[length+2]=t.getInt("stress");
  next[length+3]=t.getInt("caffeine");next[length+4]=t.getInt("tolerance");
  t.putIntArray("history",next);
 }
 public static boolean usePoultice(ServerPlayer p){
  CompoundTag t=data(p);
  long now=p.serverLevel().getGameTime();
  if(t.contains("poulticeAt")&&now>=t.getLong("poulticeAt")
      &&now-t.getLong("poulticeAt")<1800)return false;
  if(t.getInt("stress")<60&&t.getInt("fatigue")<60)return false;
  t.putInt("stress",clamp(t.getInt("stress")-215));
  t.putInt("fatigue",clamp(t.getInt("fatigue")-135));
  t.putLong("poulticeAt",now);
  save(p,t);sync(p,t);
  return true;
 }
 public static int useDreamcatcher(ServerPlayer p,BlockPos pos){
  CompoundTag t=data(p);
  long now=p.serverLevel().getGameTime();
  if(t.contains("dreamcatcherLast")&&now>=t.getLong("dreamcatcherLast")
      &&now-t.getLong("dreamcatcherLast")<3600)return 0;
  if(pos.distSqr(p.blockPosition())>64)return 0;
  int removed=Math.min(225,t.getInt("stress"));
  t.putInt("stress",clamp(t.getInt("stress")-225));
  t.putInt("fatigue",clamp(t.getInt("fatigue")-80));
  t.putLong("dreamcatcherLast",now);
  save(p,t);sync(p,t);
  p.level().playSound(null,pos,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
   net.minecraft.sounds.SoundSource.BLOCKS,.6f,1.1f);
  return removed;
 }

 /** Called by the multi-second, interruptible rest session, not block use. */
 public static int finishCampRest(ServerPlayer p,BlockPos pos){
  if(!SleepSettings.ENABLE_CAMP_REST.get())return 0;
  var site=RestEnvironment.evaluate(p,pos);
  CompoundTag t=data(p);
  int fatigueRecovery=(int)(Math.min(520,165*site.quality()/100+(site.warm()?55:0))*(1-.30*Math.min(1,SleepEffects.load(t.getInt("stress"))*SleepSettings.STRESS_PENALTY.get())));
  int stressRecovery=Math.min(420,100*site.quality()/100+(site.incense()?80:0));
  // A short camp break restores the body, not accumulated sleep debt.
  int debtRecovery=0;
  t.putInt("fatigue",clamp(t.getInt("fatigue")-fatigueRecovery));
  t.putInt("stress",clamp(t.getInt("stress")-stressRecovery));
  t.putInt("debt",clamp(t.getInt("debt")-debtRecovery));
  t.putLong("lastCampRest",p.serverLevel().getGameTime());
  save(p,t);sync(p,t);
  return fatigueRecovery;
 }

 public static void drink(ServerPlayer p,int duration,int caffeine,int relief,int calm){
  CompoundTag t=data(p);
  if(caffeine>0){
   int load=t.getInt("caffeine");
   int tolerance=t.getInt("tolerance");
   int effect=Math.max(25,100-tolerance/2-load/3);
   int potency=caffeine>=80?85:caffeine>=55?70:caffeine>=35?55:42;
   int active=duration*effect/100;
   if(active>=t.getInt("mask")){t.putInt("mask",active);t.putInt("maskPower",potency*effect/100);}
   t.putInt("caffeine",Math.min(100,load+caffeine));
   t.putInt("tolerance",Math.min(100,tolerance+Math.max(2,caffeine/5)));
   t.putInt("crashPending",Math.max(t.getInt("crashPending"),caffeine>=80?3600:duration>=16000?3200:caffeine>=55?2400:caffeine>=35?1400:700));
   if(calm>0)t.putInt("calm",Math.max(t.getInt("calm"),6000+calm*1800));
   if(load+caffeine>=100){t.putInt("stress",clamp(t.getInt("stress")+140));t.putInt("crash",Math.max(t.getInt("crash"),1200));}

  }else{
   // Tea and warm milk calm you; they do not magically undo hours awake.
   t.putInt("fatigue",clamp(t.getInt("fatigue")-relief*2));
   t.putInt("stress",clamp(t.getInt("stress")-relief-calm*55));
   // Calming drinks cannot erase sleep debt.
   if(calm>0)t.putInt("calm",6000+calm*1800);
  }
  save(p,t);sync(p,t);
 }

 public static void tick(PlayerTickEvent.Post e){
  if(!(e.getEntity() instanceof ServerPlayer p))return;
  if(p.isSpectator()){SleepEffects.apply(p,data(p));return;}
  CampRestSessions.tick(p);
  if(p.getAbilities().instabuild){SleepEffects.apply(p,data(p));return;}
  CompoundTag t=data(p);
  if(!t.contains("lastSleepAt"))t.putLong("lastSleepAt",p.serverLevel().getGameTime());
  if(p.isSleeping()){
   if(t.getInt("sleepTicks")==0)t.putLong("sleepStartDay",p.level().getDayTime());
   t.putInt("sleepTicks",Math.min(500,t.getInt("sleepTicks")+1));
   save(p,t);return;
  }
  int slept=t.getInt("sleepTicks");
  if(slept>0){
   boolean skipped=p.level().getDayTime()-t.getLong("sleepStartDay")>1000;
   if(slept>=40||skipped)recoverFromSleep(p,t,skipped?100:slept);
   t.putInt("sleepTicks",0);
   sync(p,t);
  }
  // Measure horizontal movement every tick; teleport-sized steps are ignored.
  double dx=p.getX()-p.xo,dz=p.getZ()-p.zo;
  double distance=Math.sqrt(dx*dx+dz*dz);
  if(distance<2&&!p.isPassenger()&&!p.isFallFlying())
   t.putDouble("walkDistance",t.getDouble("walkDistance")+distance);
  int progress=t.getInt("progress")+1;
  int interval=SleepSettings.TICKS_PER_POINT.get();
  if(p.isSprinting()||p.isSwimming())interval=Math.max(20,(int)(interval*.78f));
  if(progress>=interval){
   progress=0;
   t.putInt("debt",clamp(t.getInt("debt")+1));
  }
  t.putInt("progress",progress);
  if(p.tickCount%20==0){
   double distanceMoved=t.getDouble("walkDistance");t.putDouble("walkDistance",0);
   boolean exerting=distanceMoved>.35||p.isSwimming()||p.isSprinting();
   double stressLoad=Math.min(1,SleepEffects.load(t.getInt("stress"))*SleepSettings.STRESS_PENALTY.get());
   if(exerting){
    int weight=p.getArmorValue()/6;
    int hunger=p.getFoodData().getFoodLevel()<7?2:0;
    int activity=p.isSwimming()?1:0;
    int amount=(p.isSwimming()?5:p.isSprinting()?4:1)+weight+hunger;
    exert(t,activity,amount);
    if(p.isSprinting()&&!p.isSwimming())exert(t,5,2);
    if(SleepSettings.PENALTIES.get())p.causeFoodExhaustion((float)(.10*stressLoad));
   }else if(p.hurtTime==0){
    int recovery=p.getFoodData().getFoodLevel()<7?0:stressLoad>.55?1:2;
    t.putInt("fatigue",clamp(t.getInt("fatigue")-recovery));
   }
   boolean threatened=p.hurtTime>0||RestEnvironment.threatened(p,9);
   boolean dark=!p.level().isDay()&&p.level().getMaxLocalRawBrightness(p.blockPosition())<5;
   int calming=t.getInt("calm")>0?2:0;
   t.putInt("stress",clamp(t.getInt("stress")+(threatened?5:dark?1:-2-calming)));
   int mask=t.getInt("mask");
   if(mask>0){
    mask=Math.max(0,mask-20);
    t.putInt("mask",mask);
    if(mask==0){t.putInt("crash",Math.max(t.getInt("crash"),t.getInt("crashPending")));t.putInt("crashPending",0);t.putInt("maskPower",0);}
   }
   if(t.getInt("crash")>0)t.putInt("crash",Math.max(0,t.getInt("crash")-20));
   if(t.getInt("calm")>0)t.putInt("calm",Math.max(0,t.getInt("calm")-20));
   if(p.tickCount%120==0&&t.getInt("caffeine")>0)t.putInt("caffeine",t.getInt("caffeine")-1);
   if(p.tickCount%2400==0&&t.getInt("tolerance")>0)t.putInt("tolerance",t.getInt("tolerance")-1);
   if(p.tickCount%1200==0)historySample(p,t);
  }
  if(t.getInt("micro")>0)t.putInt("micro",t.getInt("micro")-1);
  if(p.tickCount%20==0){SleepEffects.apply(p,t);sync(p,t);}
  if(SleepSettings.PENALTIES.get()&&SleepSettings.MICROSLEEPS.get()&&p.tickCount%400==0&&t.getInt("debt")>=900&&t.getInt("mask")==0&&p.getRandom().nextInt(3)==0){
   t.putInt("micro",40);
   p.displayClientMessage(Component.translatable("message.sleepdebt.micro"),true);
   SleepEffects.apply(p,t);
  }
  save(p,t);
 }

 private static void recoverFromSleep(ServerPlayer p,CompoundTag t,int slept){
  var site=RestEnvironment.evaluate(p,p.blockPosition());
  int quality=site.quality();
  int base=SleepSettings.BASE_RECOVERY.get();
  int recovery=base*quality/100;
  if(slept<85)recovery=Math.min(recovery,180);
  long now=p.serverLevel().getGameTime();
  // Anti-spam: multiple partial bed sleeps cannot replace a real night.
  boolean repeated=t.contains("lastFullSleepAt")&&now>=t.getLong("lastFullSleepAt")&&now-t.getLong("lastFullSleepAt")<6000;
  if(repeated)recovery=Math.min(recovery,90);
  int before=t.getInt("debt");
  int after=clamp(before-recovery);
  t.putLong("lastFullSleepAt",now);
  t.putInt("lastSleepQuality",quality);
  t.putInt("lastSleepRecovery",before-after);
  t.putInt("lastSleepReasons",site.reasons());
  t.putLong("lastSleepAt",p.serverLevel().getGameTime());
  t.putInt("debt",after);
  t.putInt("progress",0);
  t.putInt("mask",0);
  t.putInt("crash",0);t.putInt("crashPending",0);t.putInt("maskPower",0);t.putInt("micro",0);
  t.putInt("calm",0);
  t.putInt("fatigue",clamp(t.getInt("fatigue")-(repeated?Math.min(90,650*quality/100):slept<85?Math.min(180,650*quality/100):650*quality/100)));
  t.putInt("stress",clamp(t.getInt("stress")-(repeated?Math.min(70,500*quality/100):slept<85?Math.min(140,500*quality/100):500*quality/100)));
  t.putInt("caffeine",Math.max(0,t.getInt("caffeine")-30));
  p.displayClientMessage(Component.translatable("message.sleepdebt.sleep_report",
   quality,before-after),true);

 }

 public static void clonePlayer(PlayerEvent.Clone e){
  if(!(e.getEntity() instanceof ServerPlayer p))return;
  CampRestSessions.discard(p);
  CompoundTag old=e.getOriginal().getPersistentData().getCompound(KEY).copy();
  if(e.isWasDeath()){
   old.putInt("stress",Math.max(0,old.getInt("stress")-180));
   old.putInt("fatigue",Math.max(0,old.getInt("fatigue")-120));
  }
  if(e.isWasDeath()&&!SleepSettings.KEEP_ON_DEATH.get()){
   old.putInt("debt",0);old.putInt("progress",0);
   old.putInt("fatigue",0);old.putInt("stress",0);
   old.putInt("mask",0);old.putInt("crash",0);old.putInt("maskPower",0);old.putInt("crashPending",0);old.putInt("caffeine",0);old.putInt("tolerance",0);
  }
  old.putInt("sleepTicks",0);old.putInt("micro",0);old.putDouble("walkDistance",0);
  p.getPersistentData().put(KEY,old);
 }
 public static void onLogin(PlayerEvent.PlayerLoggedInEvent e){
  if(e.getEntity() instanceof ServerPlayer p){CampRestSessions.discard(p);sync(p,data(p));}
 }
 private SleepEngine(){}
}
