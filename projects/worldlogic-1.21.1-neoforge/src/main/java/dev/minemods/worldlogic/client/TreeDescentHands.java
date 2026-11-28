package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.TreeDescentSync;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
@EventBusSubscriber(modid="worldlogic",value=Dist.CLIENT)
public final class TreeDescentHands{
 @SubscribeEvent public static void hands(RenderHandEvent e){
  var p=Minecraft.getInstance().player;
  if(p==null||p.onGround()||!p.isShiftKeyDown()||!e.getItemStack().isEmpty()||!TreeDescentSync.clientGripping(p))return;
  float phase=(float)Math.sin((p.tickCount+e.getPartialTick())*.22);
  e.getPoseStack().translate(0,.12+.04*phase,-.12);
  e.getPoseStack().mulPose(Axis.XP.rotationDegrees(-18+5*phase));
 }
}
