package dev.minemods.aquaculture;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Fixed, non-colliding entity: never replaces native water or vegetation. */
public final class RoeClutch extends Entity {
 private static final EntityDataAccessor<String> SPECIES=SynchedEntityData.defineId(RoeClutch.class,EntityDataSerializers.STRING);
 private static final EntityDataAccessor<Integer> EGGS=SynchedEntityData.defineId(RoeClutch.class,EntityDataSerializers.INT);
 private static final EntityDataAccessor<Integer> STAGE=SynchedEntityData.defineId(RoeClutch.class,EntityDataSerializers.INT);
 private WaterKind kind=WaterKind.NONE;
 private long progress;
 private int badTicks;
 public RoeClutch(EntityType<? extends RoeClutch> type,Level level){super(type,level);setNoGravity(true);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(SPECIES,"");b.define(EGGS,0);b.define(STAGE,0);}
 public Species species(){return Species.BY_ID.get(entityData.get(SPECIES));}
 public int eggs(){return entityData.get(EGGS);}
 public int stage(){return entityData.get(STAGE);}
 public String nursery(){Species s=species();return s==null?"gravel":s.nursery();}
 void initialize(Species s,WaterKind k,int count){entityData.set(SPECIES,s.id());kind=k;entityData.set(EGGS,count);progress=0;badTicks=0;}
 long duration(){Species s=species();return s==null?24000L:Math.max(1,s.incubationDays())*24000L;}
 @Override public boolean isPickable(){return true;}
 @Override public boolean hurt(DamageSource source,float amount){
  if(isInvulnerableTo(source)||amount<=0||isRemoved())return false;
  if(!level().isClientSide)discard();return true;
 }
 @Override public InteractionResult interact(Player player,InteractionHand hand){
  if(!level().isClientSide&&species()!=null)player.displayClientMessage(Component.translatable("aquaculture.roe.inspect",Component.translatable("entity.aquaculture."+species().id()),eggs(),(int)Math.min(100,progress*100/duration())),true);
  return level().isClientSide?InteractionResult.SUCCESS:InteractionResult.CONSUME;
 }
 @Override protected void addAdditionalSaveData(CompoundTag n){
  n.putString("species",entityData.get(SPECIES));n.putInt("eggs",eggs());n.putString("water",kind.name());n.putLong("progress",progress);n.putInt("bad",badTicks);
 }
 @Override protected void readAdditionalSaveData(CompoundTag n){
  entityData.set(SPECIES,n.getString("species"));entityData.set(EGGS,Math.max(0,n.getInt("eggs")));
  try{kind=WaterKind.valueOf(n.getString("water"));}catch(IllegalArgumentException e){kind=WaterKind.NONE;}
  progress=Math.max(0,n.getLong("progress"));badTicks=Math.max(0,n.getInt("bad"));entityData.set(STAGE,(int)Math.min(3,progress*4/duration()));
 }
 @Override public void remove(RemovalReason reason){
  if(reason.shouldDestroy()&&level() instanceof ServerLevel l)PopulationData.get(l).remove(getUUID());
  super.remove(reason);
 }
 @Override public void tick(){
  super.tick();
  if(!(level() instanceof ServerLevel l)||tickCount%100!=0)return;
  advance(l,100);
 }
 void advance(ServerLevel l,int elapsed){
  Species s=species();if(s==null||eggs()<=0||!PopulationData.valid(kind)){discard();return;}
  var area=PopulationData.area(blockPosition(),kind);var pop=PopulationData.get(l);
  if(!pop.restoreClutch(getUUID(),area,s,eggs())){discard();return;}
  var h=Aquaculture.habitat(l,blockPosition());
  if(h==null||h.kind()!=kind||!FishLife.nursery(l,blockPosition(),s)){
   badTicks+=elapsed;
   if(badTicks>=2400){badTicks=0;pop.takeEgg(getUUID());entityData.set(EGGS,eggs()-1);if(eggs()==0)discard();}
   return;
  }
  badTicks=Math.max(0,badTicks-elapsed);progress=Math.min(duration(),progress+elapsed);
  entityData.set(STAGE,(int)Math.min(3,progress*4/duration()));
  if(progress<duration())return;
  if(!pop.takeEgg(getUUID())){discard();return;}
  Fish baby=Aquaculture.spawn(l,blockPosition(),s,h);
  if(baby==null){pop.returnEgg(getUUID(),area,s);return;}
  FishLife.juvenile(baby,s,l.getGameTime());
  baby.getPersistentData().putUUID("wl_roe_origin",getUUID());
  entityData.set(EGGS,eggs()-1);
  if(eggs()==0)discard();
 }
}
