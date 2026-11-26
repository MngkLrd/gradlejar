package dev.minemods.worldlogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
public final class Soil {
 private static int depletion(WorldState d,long k,long now){int v=d.soil.getOrDefault(k,0);long last=d.soilTime.getOrDefault(k,now);return Math.max(0,v-(int)Math.min(8,Math.max(0,now-last)/24000));}
 public static void harvest(BlockEvent.BreakEvent e){
  if(e.isCanceled()||!Rules.on("soil_fatigue")||!(e.getLevel() instanceof ServerLevel l)||e.getPlayer().isCreative()||!(e.getState().getBlock() instanceof CropBlock crop)||!crop.isMaxAge(e.getState()))return;
  long k=e.getPos().below().asLong();var d=WorldState.get(l);if(!d.soil.containsKey(k)&&d.soil.size()>=32768)return;
  String id=BuiltInRegistries.BLOCK.getKey(crop).toString();int v=depletion(d,k,l.getGameTime());
  if(!id.equals(d.lastCrop.getOrDefault(k,id)))v=Math.max(0,v-3);else v=Math.min(8,v+1);
  d.soil.put(k,v);d.lastCrop.put(k,id);d.soilTime.put(k,l.getGameTime());d.setDirty();
  if(v>=5)e.getPlayer().displayClientMessage(Component.literal("Soil exhausted. Rotate crops or fertilize the farmland."),true);
 }
 public static void grow(BlockEvent.CropGrowEvent.Pre e){
  if(!Rules.on("soil_fatigue")||!(e.getLevel() instanceof ServerLevel l)||!(e.getState().getBlock() instanceof CropBlock))return;
  long k=e.getPos().below().asLong();var d=WorldState.get(l);int v=depletion(d,k,l.getGameTime());if(v<=0)return;
  String id=BuiltInRegistries.BLOCK.getKey(e.getState().getBlock()).toString();if(!id.equals(d.lastCrop.getOrDefault(k,id)))v=Math.max(0,v-3);
  if(l.random.nextFloat()<Math.min(.8,v*.09*Rules.strength("ecology")))e.setResult(Event.Result.DENY);
 }
 public static void fertilize(PlayerInteractEvent.RightClickBlock e){
  if(!Rules.on("soil_fatigue")||e.isCanceled()||!(e.getLevel() instanceof ServerLevel l)||!e.getItemStack().is(Items.BONE_MEAL)||!l.getBlockState(e.getPos()).is(Blocks.FARMLAND))return;
  var d=WorldState.get(l);long k=e.getPos().asLong();int v=depletion(d,k,l.getGameTime());if(v<=0)return;
  d.soil.put(k,Math.max(0,v-3));d.soilTime.put(k,l.getGameTime());d.setDirty();if(!e.getEntity().isCreative())e.getItemStack().shrink(1);l.levelEvent(1505,e.getPos(),0);e.getEntity().displayClientMessage(Component.literal("Soil fertility restored."),true);e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
 }
}
