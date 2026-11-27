package dev.minemods.hydration.client;
import dev.minemods.hydration.HydrationSync;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="hydration",value=Dist.CLIENT)
public final class HydrationInput {
 @SubscribeEvent public static void use(InputEvent.InteractionKeyMappingTriggered e){
  var p=Minecraft.getInstance().player;
  if(p==null||p.isCreative()||p.isSpectator()||!e.isUseItem()||!p.isShiftKeyDown()||!p.getMainHandItem().isEmpty()||!p.getOffhandItem().isEmpty())return;
  var hit=p.pick(Math.min(4.5,p.blockInteractionRange()),1f,true);
  if(hit instanceof BlockHitResult b&&hit.getType()==HitResult.Type.BLOCK&&p.level().getFluidState(b.getBlockPos()).is(FluidTags.WATER)){
   PacketDistributor.sendToServer(new HydrationSync.Sip());e.setCanceled(true);e.setSwingHand(false);
  }
 }
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){HydrationSync.clientUnits=-1;}
}
