package dev.minemods.hydration;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class VesselTreatment {
 private record Job(ItemStack stack,WaterBatch initial,BlockPos pos,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,long due,boolean distill){}
 private static final WeakHashMap<ServerPlayer,Job> JOBS=new WeakHashMap<>();
 private static boolean lit(net.minecraft.world.level.block.state.BlockState b){return b.getBlock() instanceof CampfireBlock&&b.getValue(CampfireBlock.LIT);}
 public static void interact(PlayerInteractEvent.RightClickBlock e){
  if(e.getHand()!=InteractionHand.MAIN_HAND||!e.getEntity().isShiftKeyDown()||!(e.getItemStack().getItem() instanceof VesselItem item)||!lit(e.getLevel().getBlockState(e.getPos())))return;
  e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
  if(!(e.getEntity() instanceof ServerPlayer p)||!Hydration.active(p))return;
  if(!item.heatproof){p.displayClientMessage(Component.translatable("hydration.treatment.not_heatproof"),true);return;}
  var stack=e.getItemStack();var b=item.read(stack);if(b.ml()<100)return;
  if(JOBS.containsKey(p))return;
  boolean distill=p.getOffhandItem().is(HydrationItems.CONDENSER.get());
  long ticks=(distill?160:60)+b.ml()/(distill?5:15);
  JOBS.put(p,new Job(stack,b,e.getPos().immutable(),p.level().dimension(),p.serverLevel().getGameTime()+ticks,distill));
  p.displayClientMessage(Component.translatable(distill?"hydration.treatment.distilling":"hydration.treatment.boiling",(ticks+19)/20),true);
 }
 public static void tick(PlayerTickEvent.Post e){
  if(!(e.getEntity() instanceof ServerPlayer p))return;
  Job job=JOBS.get(p);if(job==null)return;
  if(!Hydration.active(p)||!p.level().dimension().equals(job.dimension)||p.distanceToSqr(job.pos.getX()+.5,job.pos.getY()+.5,job.pos.getZ()+.5)>16||
   p.getMainHandItem()!=job.stack||!(job.stack.getItem() instanceof VesselItem item)||!item.read(job.stack).equals(job.initial)||
   !p.serverLevel().hasChunkAt(job.pos)||!lit(p.serverLevel().getBlockState(job.pos))||
   (job.distill&&!p.getOffhandItem().is(HydrationItems.CONDENSER.get()))){
   JOBS.remove(p);p.displayClientMessage(Component.translatable("hydration.treatment.cancelled"),true);return;
  }
  if(p.serverLevel().getGameTime()<job.due)return;
  JOBS.remove(p);item.write(job.stack,job.distill?WaterTreatment.distill(job.initial):WaterTreatment.boil(job.initial));p.getInventory().setChanged();
  p.displayClientMessage(Component.translatable("hydration.treatment.done"),true);
 }
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)JOBS.remove(p);}
}
