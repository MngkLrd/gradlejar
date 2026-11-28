package dev.minemods.aquaculture;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public final class Fish extends Cod {
 private net.minecraft.world.phys.Vec3 riverTarget;
 private double riverSpeed;
 private int riverUntil;
 void clearRiverIntent(){riverTarget=null;}
 void setRiverIntent(net.minecraft.world.phys.Vec3 target,double speed){riverTarget=target;riverSpeed=speed;riverUntil=tickCount+12;}
 private void swimRiver(net.minecraft.server.level.ServerLevel l){
  if(riverTarget==null||tickCount>riverUntil||ecologyPanic()||!FishWater.contact(this)){riverTarget=null;return;}
  var here=position().add(0,Math.min(.2,getBbHeight()*.5),0);var delta=riverTarget.subtract(here);
  if(delta.lengthSqr()<.16||!FishEcology.clearWaterPath(l,here,riverTarget)){riverTarget=null;return;}
  var motion=delta.normalize().scale(riverSpeed);
  if(!l.noCollision(this,getBoundingBox().move(motion))){riverTarget=null;return;}
  // Active propulsion replaces this tick's swim intention, never adds Core's passive current.
  getNavigation().stop();setDeltaMovement(motion);
  float yaw=(float)(Math.atan2(-motion.x,motion.z)*180/Math.PI);setYRot(yaw);yBodyRot=yaw;yHeadRot=yaw;
 }
 private static final EntityDataAccessor<Boolean> ECO_PANIC=SynchedEntityData.defineId(Fish.class,EntityDataSerializers.BOOLEAN);
 public boolean ecologyPanic(){return entityData.get(ECO_PANIC);}
 public void setEcologyPanic(boolean value){if(!level().isClientSide)entityData.set(ECO_PANIC,value);}
 private static final EntityDataAccessor<Float> VISUAL_LENGTH=SynchedEntityData.defineId(Fish.class,EntityDataSerializers.FLOAT);
 @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
  super.defineSynchedData(builder);builder.define(VISUAL_LENGTH,0f);builder.define(ECO_PANIC,false);
 }
 public static float visualLengthOf(Species s,double length){
  double value=Double.isFinite(length)&&length>0?length:s.adultLength()*.8;
  return (float)Math.max(.16,Math.min(3.5,value));
 }
 public float visualLength(Species s){return visualLengthOf(s,entityData.get(VISUAL_LENGTH));}
 public void syncVisualLength(Species s,CompoundTag traits){
  if(!level().isClientSide)entityData.set(VISUAL_LENGTH,visualLengthOf(s,traits.getDouble("wl_fish_length_m")));
 }
 public Fish(EntityType<? extends Cod> type,Level level){super(type,level);}
 @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
  // Transport is not implemented; prevent vanilla cod bucket identity conversion.
  if(player.getItemInHand(hand).is(Items.WATER_BUCKET))return InteractionResult.FAIL;
  Species s=Aquaculture.species(this);
  var food=player.getItemInHand(hand);
  if(s!=null&&food.isEmpty()&&player.isShiftKeyDown()){
   if(!level().isClientSide){
    var tag=Aquaculture.traits(this,s);
    player.displayClientMessage(Component.translatable("aquaculture.ecology.inspect",getName(),
     String.format(java.util.Locale.ROOT,"%.2f",tag.getDouble("wl_fish_length_m")),
     String.format(java.util.Locale.ROOT,"%.2f",tag.getDouble("wl_fish_mass_kg")),
     FishFeeding.satiety(this,level().getGameTime()),Math.round(tag.getDouble("wl_fish_stress")*100)),true);
   }
   return level().isClientSide?InteractionResult.SUCCESS:InteractionResult.CONSUME;
  }
  if(s!=null&&!food.isEmpty()&&food.is(FishFeeding.foodTag(s))){
   if(level().isClientSide)return InteractionResult.SUCCESS;
   var result=FishFeeding.offer(this,s,food,player.getAbilities().instabuild);
   if(result==FishFeeding.Result.FED){
    player.displayClientMessage(Component.translatable("aquaculture.feed.fed",FishFeeding.satiety(this,level().getGameTime())),true);
    return InteractionResult.CONSUME;
   }
   player.displayClientMessage(Component.translatable("aquaculture.feed."+result.name().toLowerCase(java.util.Locale.ROOT)),true);
   return InteractionResult.FAIL;
  }
  return super.mobInteract(player,hand);
 }
 @Override public void remove(RemovalReason reason){
  if(reason.shouldDestroy()&&level() instanceof net.minecraft.server.level.ServerLevel l)PopulationData.get(l).remove(getUUID());
  super.remove(reason);
 }
 @Override public void tick(){
  super.tick();
  if(level() instanceof net.minecraft.server.level.ServerLevel l){
   if(tickCount==1||(tickCount+getId())%100==0){
    Species s=Aquaculture.species(this);
    if(s!=null&&isAlive()){
     setPersistenceRequired();
     FishLife.age(this,s,l.getGameTime());
     var h=Aquaculture.habitat(l,blockPosition());
     if(h!=null&&PopulationData.valid(h.kind()))PopulationData.get(l).observe(getUUID(),PopulationData.area(blockPosition(),h.kind()),s);
    }
   }
   if((tickCount+getId())%10==0){
    Species s=Aquaculture.species(this);clearRiverIntent();if(s!=null)FishEcology.step(l,this,s);
   }
   swimRiver(l);
  }
 }
}

