package dev.minemods.thermal.content;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import java.lang.reflect.Method;
/** Direct emergency gathering uses the Core protection-only probe before changing a block. */
public final class HarvestPermission {
 private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("WorldLogic Thermal");
 private static final Method CHECK=find();
 private static boolean warned;
 private HarvestPermission(){}
 private static Method find(){
  try{return Class.forName("dev.minemods.worldlogic.api.WorldLogicBreaks").getMethod("checkProtection",ServerLevel.class,ServerPlayer.class,BlockPos.class,BlockState.class);}
  catch(ReflectiveOperationException|LinkageError ex){return null;}
 }
 public static boolean allowed(ServerLevel l,BlockPos pos,Player p,BlockState expected){
  if(!(p instanceof ServerPlayer player)||!p.isAlive()||p.isCreative()||p.isSpectator()||!p.mayBuild()||!l.hasChunkAt(pos)||!l.mayInteract(p,pos)||!l.getBlockState(pos).equals(expected))return false;
  if(CHECK==null){
   try{if(WorldLogicApi.class.getField("API_REVISION").getInt(null)>=21)return false;}
   catch(ReflectiveOperationException ex){return false;}
   return true; // historical local permission checks on old Core; no pretend claim integration.
  }
  try{return Boolean.TRUE.equals(CHECK.invoke(null,l,player,pos,expected))&&l.getBlockState(pos).equals(expected);}
  catch(ReflectiveOperationException|LinkageError|RuntimeException ex){if(!warned){LOG.warn("Core harvest protection probe failed; denying harvest",ex);warned=true;}return false;}
 }
 public static void accepted(Player p,double exertion){
  if(CHECK!=null&&p instanceof ServerPlayer player&&WorldLogicApi.fatigue(player).enabled())WorldLogicApi.addExertion(player,exertion);
 }
}
