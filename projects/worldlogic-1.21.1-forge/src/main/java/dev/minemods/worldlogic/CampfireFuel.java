package dev.minemods.worldlogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
public final class CampfireFuel {
 static final int INITIAL=6000,MAX=24000;
 private static int burn(ItemStack s){if(s.is(Items.COAL)||s.is(Items.CHARCOAL))return 3600;if(s.is(ItemTags.LOGS))return 2400;if(s.is(ItemTags.PLANKS))return 900;if(s.is(Items.STICK))return 300;return 0;}
 public static boolean tick(ServerLevel l,BlockPos q,BlockState s){
  if(!Rules.on("campfire_fuel")||l.getGameTime()%20!=0)return true;var data=WorldState.get(l);long k=q.asLong();
  if(!data.fuel.containsKey(k)&&data.fuel.size()>=32768)return true;
  int old=data.fuel.getOrDefault(k,INITIAL);int rate=(int)Math.max(1,Math.round(20*Rules.strength("fire")));int fuel=Math.max(0,old-rate);data.fuel.put(k,fuel);data.setDirty();
  if(fuel==0){l.setBlock(q,s.setValue(BlockStateProperties.LIT,false),3);l.playSound(null,q,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.6f,.9f);return false;}
  if(fuel<600)l.sendParticles(ParticleTypes.SMOKE,q.getX()+.5,q.getY()+.7,q.getZ()+.5,2,.1,.05,.1,.01);
  return true;
 }
 public static void feed(PlayerInteractEvent.RightClickBlock e){
  if(!Rules.on("campfire_fuel")||e.isCanceled()||!(e.getLevel() instanceof ServerLevel l))return;
  var s=l.getBlockState(e.getPos());if(!(s.getBlock() instanceof CampfireBlock))return;
  var p=e.getEntity();var data=WorldState.get(l);long k=e.getPos().asLong();int fuel=data.fuel.getOrDefault(k,INITIAL);
  if(e.getItemStack().is(Items.FLINT_AND_STEEL)&&fuel<=0){p.displayClientMessage(Component.literal("The campfire needs fuel first."),true);e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);return;}
  int add=burn(e.getItemStack());if(add==0)return;
  if(fuel<MAX&&(data.fuel.containsKey(k)||data.fuel.size()<32768)){fuel=Math.min(MAX,fuel+add);data.fuel.put(k,fuel);data.setDirty();if(!p.isCreative())e.getItemStack().shrink(1);l.playSound(null,e.getPos(),SoundEvents.WOOD_PLACE,SoundSource.BLOCKS,.7f,1f);}
  p.displayClientMessage(Component.literal("Campfire fuel: "+(fuel/20)+" seconds (normal burn rate)"),true);e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
 }
}
