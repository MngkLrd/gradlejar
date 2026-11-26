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
  // Do not modify counts; other phantom mods retain their own spawn rules.
  // The regular vanilla world rules (skylight, insomnia gamerule, difficulty)
  // still apply; no phantom is forced into safe daytime worlds.
 }
 private VanillaSleepIntegration(){}
}
