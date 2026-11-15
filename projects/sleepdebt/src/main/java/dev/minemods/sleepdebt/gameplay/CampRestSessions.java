package dev.minemods.sleepdebt.gameplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Short rest requires stillness. Damage or movement interrupts it. */
public final class CampRestSessions {
 private static final int DURATION=140;
 private static final Map<UUID,Session> SESSIONS=new HashMap<>();
 private record Session(BlockPos bedroll,Vec3 origin,String dimension,long started,double health){}
 public static void begin(ServerPlayer player,BlockPos pos){
  if(player.isSpectator()){return;}
  if(player.distanceToSqr(Vec3.atCenterOf(pos))>16){
   player.displayClientMessage(Component.translatable("message.sleepdebt.camp_far"),true);
   return;
  }
  long now=player.serverLevel().getGameTime();
  var tag=player.getPersistentData().getCompound("sleepdebt_v2");
  if(tag.contains("lastCampRest")&&now>=tag.getLong("lastCampRest")
      &&now-tag.getLong("lastCampRest")<2400){
   player.displayClientMessage(Component.translatable("message.sleepdebt.camp_cooldown"),true);
   return;
  }
  if(SESSIONS.remove(player.getUUID())!=null){
   player.displayClientMessage(Component.translatable("message.sleepdebt.camp_cancel"),true);
   return;
  }
  SESSIONS.put(player.getUUID(),new Session(pos.immutable(),player.position(),
    player.level().dimension().location().toString(),now,player.getHealth()));
  player.displayClientMessage(Component.translatable("message.sleepdebt.camp_begin"),true);
 }
 public static void tick(ServerPlayer player){
  Session session=SESSIONS.get(player.getUUID());
  if(session==null)return;
  boolean invalid=player.isRemoved()||player.isSpectator()||player.isSleeping()
      ||!session.dimension.equals(player.level().dimension().location().toString())
      ||!player.level().getBlockState(session.bedroll).is(
        dev.minemods.sleepdebt.registry.ModBlocks.CAMP_BEDROLL.get())
      ||player.position().distanceToSqr(session.origin)>3.24
      ||player.getHealth()<session.health||player.hurtTime>0;
  if(invalid){
   SESSIONS.remove(player.getUUID());
   player.displayClientMessage(Component.translatable("message.sleepdebt.camp_interrupted"),true);
   return;
  }
  long elapsed=player.serverLevel().getGameTime()-session.started;
  if(elapsed>=DURATION){
   SESSIONS.remove(player.getUUID());
   int restored=SleepEngine.finishCampRest(player,session.bedroll);
   player.displayClientMessage(Component.translatable("message.sleepdebt.camp_complete",restored),true);
   player.level().playSound(null,session.bedroll,
     SoundEvents.WOOL_PLACE,SoundSource.PLAYERS,.7f,.8f);
  }else if(elapsed>=0&&elapsed%20==0){
   player.displayClientMessage(Component.translatable("message.sleepdebt.camp_progress",
     Math.min(99,(int)(100*elapsed/DURATION))),true);
  }
 }
 public static void discard(ServerPlayer player){SESSIONS.remove(player.getUUID());}
 public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event){
  if(event.getEntity() instanceof ServerPlayer player)discard(player);
 }
 private CampRestSessions(){}
}
