package dev.minemods.sleepdebt.gameplay;

import dev.minemods.sleepdebt.SleepSettings;
import dev.minemods.sleepdebt.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;

/**
 * Integrates the vanilla phantom mechanic with our sleep debt instead of
 * making two incompatible notions of insomnia coexist.
 */
public final class VanillaSleepIntegration {
 public static void onPhantoms(PlayerSpawnPhantomsEvent event){
  if(!SleepSettings.PHANTOM_SYNC.get()||
      !(event.getEntity() instanceof ServerPlayer player))return;
  int debt=SleepEngine.getDebt(player);
  if(debt<250){
   event.setResult(PlayerSpawnPhantomsEvent.Result.DENY);
   return;
  }
  // A properly defended sleeping area also helps with vanilla phantoms.
  BlockPos at=player.blockPosition();
  for(BlockPos pos:BlockPos.betweenClosed(at.offset(-7,-3,-7),at.offset(7,4,7))){
   if(player.level().getBlockState(pos).is(ModBlocks.DREAMCATCHER.get())){
    event.setResult(PlayerSpawnPhantomsEvent.Result.DENY);
    return;
   }
  }
  if(debt>=850){
   event.setPhantomsToSpawn(Math.min(4,event.getPhantomsToSpawn()+1));
  }
  // The regular vanilla world rules (skylight, insomnia gamerule, difficulty)
  // still apply; no phantom is forced into safe daytime worlds.
 }
 private VanillaSleepIntegration(){}
}
