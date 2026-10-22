package dev.minemods.sleepdebt.gameplay;
import dev.minemods.sleepdebt.SleepSettings;
import dev.minemods.sleepdebt.network.SleepStatusPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SleepEngine {
 private static final String KEY="sleepdebt_v2";
 private static final int MAX=1000;
 private static int read(CompoundTag tag,String k){return tag.getInt(k);}
 private static CompoundTag data(ServerPlayer p){return p.getPersistentData().getCompound(KEY);}
 private static void save(ServerPlayer p,CompoundTag tag){p.getPersistentData().put(KEY,tag);}
 private static void sync(ServerPlayer p,CompoundTag t){
  PacketDistributor.sendToPlayer(p,new SleepStatusPayload(read(t,"debt"),read(t,"mask"),read(t,"crash"),read(t,"caffeine")));
 }
 public static void tick(PlayerTickEvent.Post e) {
  if(!(e.getEntity() instanceof ServerPlayer p)||p.isSpectator()||p.getAbilities().instabuild) return;
  CompoundTag t=data(p);
  if(p.isSleeping()){
   t.putInt("sleepTicks",Math.min(500,read(t,"sleepTicks")+1));
   save(p,t);return;
  }
  int slept=read(t,"sleepTicks");
  if(slept>0) {
   if(slept>=40) {
    int recovery=SleepSettings.BASE_RECOVERY.get();
    // Comforts and modded beds: observe the real sleeping state; never replace their sleep event logic.
    if(SleepSettings.SHELTER_BONUS.get() && !p.level().canSeeSky(p.blockPosition())) recovery=(int)(recovery*1.15);
    if(SleepSettings.MONSTER_PENALTY.get() &&
       !p.level().getEntitiesOfClass(Monster.class,p.getBoundingBox().inflate(12)).isEmpty()) recovery=(int)(recovery*0.85);
    if(read(t,"calm")>0) recovery=(int)(recovery*1.1);
    if(slept<85) recovery=Math.min(recovery,180); // brief rest
    int before=read(t,"debt");
    int after=Math.max(0,before-recovery);
    t.putInt("debt",after);t.putInt("progress",0);
    t.putInt("mask",0);t.putInt("crash",0);t.putInt("calm",0);
    t.putInt("caffeine",Math.max(0,read(t,"caffeine")-30));
    if(after==0 && before>0) p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,1600,0,true,true));
   }
   t.putInt("sleepTicks",0);
   sync(p,t);
  }
  int progress=read(t,"progress")+1;
  if(progress>=SleepSettings.TICKS_PER_POINT.get()){
   progress=0; t.putInt("debt",Math.min(MAX,read(t,"debt")+1));
  }
  t.putInt("progress",progress);
  if(p.tickCount%20==0){
   int mask=read(t,"mask");
   if(mask>0){
    mask=Math.max(0,mask-20);t.putInt("mask",mask);
    if(mask==0 && read(t,"caffeine")>=55) t.putInt("crash",1800);
   }
   if(read(t,"crash")>0)t.putInt("crash",Math.max(0,read(t,"crash")-20));
   if(read(t,"calm")>0)t.putInt("calm",Math.max(0,read(t,"calm")-20));
   if(p.tickCount%600==0 && read(t,"caffeine")>0) t.putInt("caffeine",read(t,"caffeine")-1);
  }
  if(p.tickCount%100==0){apply(p,t);sync(p,t);}
  save(p,t);
 }
 public static void drink(ServerPlayer p,int duration,int caffeine,int heal,int calm) {
  CompoundTag t=data(p);
  if(caffeine>0) {
   int load=read(t,"caffeine");
   int effective=Math.max(25,100-load);
   t.putInt("mask",Math.max(read(t,"mask"),duration*effective/100));
   t.putInt("caffeine",Math.min(100,load+caffeine));
   t.putInt("crash",0);
  } else {
   t.putInt("debt",Math.max(0,read(t,"debt")-heal));
   if(calm>0)t.putInt("calm",6000);
  }
  save(p,t);sync(p,t);
 }
 private static void apply(ServerPlayer p,CompoundTag t) {
  if(!SleepSettings.PENALTIES.get()) return;
  int debt=read(t,"debt");
  boolean masked=read(t,"mask")>0;
  if(!masked && debt>=250) p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,125,debt>=620?1:0,true,false,true));
  if(!masked && debt>=420) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,125,debt>=830?1:0,true,false,true));
  if((!masked && debt>=620)||read(t,"crash")>0)
   p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,125,0,true,false,true));
  if(read(t,"caffeine")>=95)
   p.addEffect(new MobEffectInstance(MobEffects.CONFUSION,125,0,true,false,true));
 }
 public static void clonePlayer(PlayerEvent.Clone e){
  if(!(e.getEntity() instanceof ServerPlayer p))return;
  CompoundTag old=e.getOriginal().getPersistentData().getCompound(KEY).copy();
  if(e.isWasDeath()&&!SleepSettings.KEEP_ON_DEATH.get()){old.putInt("debt",0);old.putInt("progress",0);}
  old.putInt("sleepTicks",0);
  p.getPersistentData().put(KEY,old);
 }
 public static void onLogin(PlayerEvent.PlayerLoggedInEvent e){
  if(e.getEntity() instanceof ServerPlayer p)sync(p,data(p));
 }
 private SleepEngine(){}
}
