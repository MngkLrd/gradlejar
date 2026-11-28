package dev.minemods.worldlogic.api;
import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Protection-only probe. No block mutation, drops, effort or Core break side effects. External listeners must respect the marker. */
public final class WorldLogicBreaks {
 private WorldLogicBreaks(){}
 private static final ThreadLocal<ArrayDeque<BlockEvent.BreakEvent>> PROBES=new ThreadLocal<>();
 public static boolean isProtectionProbe(BlockEvent.BreakEvent event){
  var scope=PROBES.get();return scope!=null&&scope.contains(event);
 }
 public static boolean checkProtection(ServerLevel level,ServerPlayer player,BlockPos pos,BlockState expected){
  if(!level.getServer().isSameThread())throw new IllegalStateException("server thread required");
  if(player.level()!=level||!player.isAlive()||player.isSpectator()||!player.mayBuild()
   ||level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos)||!level.mayInteract(player,pos)
   ||expected==null||expected.isAir()||!level.getBlockState(pos).equals(expected))return false;
  var scope=PROBES.get();
  if(scope!=null)for(var active:scope)if(active.getPlayer()==player&&active.getLevel()==level&&active.getPos().equals(pos))return false;
  if(scope==null){scope=new ArrayDeque<>();PROBES.set(scope);}
  var event=new BlockEvent.BreakEvent(level,pos.immutable(),expected,player);
  scope.addLast(event);
  try{return !NeoForge.EVENT_BUS.post(event).isCanceled();}
  finally{scope.removeLast();if(scope.isEmpty())PROBES.remove();}
 }
}
