package dev.minemods.blowpipes;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
public final class BlowpipeItem extends BlowpipeInfoItem {
 private static final int DURATION=72000, BREATH=10;
 private final int tier;
 private static final String[] TYPES={"plain_dart","iron_dart","poison_dart","slowness_dart","fire_dart","sleep_dart"};
 private static final Map<UUID,String> SELECTED=new HashMap<>();
 public BlowpipeItem(Properties properties,int tier,String name){super(properties,name);this.tier=tier;}
 @Override public ItemUseAnimation getUseAnimation(ItemStack stack){return ItemUseAnimation.SPYGLASS;}
 @Override public int getUseDuration(ItemStack stack,LivingEntity user){return DURATION;}
 @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
  if(player.isShiftKeyDown()){if(!level.isClientSide())cycle(player);return InteractionResult.SUCCESS;}
  if(!player.getAbilities().instabuild&&ammo(player).isEmpty()){
   if(!level.isClientSide())player.sendSystemMessage(Component.translatable("message.blowpipes.no_ammo"));
   return InteractionResult.FAIL;
  }
  player.startUsingItem(hand);
  return InteractionResult.CONSUME;
 }
 @Override public boolean releaseUsing(ItemStack held,Level level,LivingEntity user,int remaining){
  if(!(user instanceof Player player)||!(level instanceof ServerLevel server))return false;
  if(DURATION-remaining<BREATH)return false;
  ItemStack ammunition=ammo(player);
  if(ammunition.isEmpty()&&!player.getAbilities().instabuild)return false;
  String kind=ammunition.isEmpty()?"plain_dart":id(ammunition);
  DartProjectile dart=new DartProjectile(Blowpipes.dartType(),level);
  dart.setOwner(player);
  dart.setPipeTier(tier);
  dart.setItem(new ItemStack(Blowpipes.flightItem(kind)));
  Vec3 look=player.getLookAngle(),eye=player.getEyePosition();
  dart.setPos(eye.x+look.x*.55,eye.y-.12+look.y*.55,eye.z+look.z*.55);
  float speed=tier==0?1.5f:tier==1?2.45f:3.25f;
  float spread=tier==0?2.2f:tier==1?.7f:.15f;
  dart.shootFromRotation(player,player.getXRot(),player.getYRot(),0,speed,spread);
  server.addFreshEntity(dart);
  if(!player.getAbilities().instabuild&&!ammunition.isEmpty())ammunition.shrink(1);
  player.getCooldowns().addCooldown(held,tier==0?12:tier==1?16:22);
  server.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.SNOWBALL_THROW,SoundSource.PLAYERS,.25f,1.5f);
  return true;
 }
 private static String id(ItemStack s){
  if(s.isEmpty())return "";
  Identifier key=BuiltInRegistries.ITEM.getKey(s.getItem());
  if(key==null||!Blowpipes.ID.equals(key.getNamespace()))return "";
  return key.getPath();
 }
 private static boolean valid(String s){for(String k:TYPES)if(k.equals(s))return true;return false;}
 private static ItemStack ammo(Player p){
  String want=SELECTED.get(p.getUUID());
  ItemStack off=p.getOffhandItem();
  if(want!=null&&want.equals(id(off)))return off;
  for(int i=0;i<p.getInventory().getContainerSize();i++){ItemStack s=p.getInventory().getItem(i);if(want!=null&&want.equals(id(s)))return s;}
  if(valid(id(off)))return off;
  for(int i=0;i<p.getInventory().getContainerSize();i++){ItemStack s=p.getInventory().getItem(i);if(valid(id(s)))return s;}
  return ItemStack.EMPTY;
 }
 private static void cycle(Player p){
  int old=-1;for(int i=0;i<TYPES.length;i++)if(TYPES[i].equals(SELECTED.get(p.getUUID())))old=i;
  for(int step=1;step<=TYPES.length;step++){
   String next=TYPES[(old+step)%TYPES.length];
   if(next.equals(id(p.getOffhandItem()))){SELECTED.put(p.getUUID(),next);p.sendSystemMessage(Component.translatable("item.blowpipes."+next));return;}
   for(int i=0;i<p.getInventory().getContainerSize();i++)if(next.equals(id(p.getInventory().getItem(i)))){
    SELECTED.put(p.getUUID(),next);p.sendSystemMessage(Component.translatable("item.blowpipes."+next));return;
   }
  }
  p.sendSystemMessage(Component.translatable("message.blowpipes.no_ammo"));
 }
}
