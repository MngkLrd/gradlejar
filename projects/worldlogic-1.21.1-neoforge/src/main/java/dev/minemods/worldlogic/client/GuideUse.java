package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.Content;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
/** The actual guide item opens the same book as the inventory navigation. */
@EventBusSubscriber(modid="worldlogic",value=Dist.CLIENT)
public final class GuideUse{
 @SubscribeEvent public static void use(PlayerInteractEvent.RightClickItem e){
  var mc=Minecraft.getInstance();
  if(e.getEntity()==mc.player&&e.getLevel().isClientSide&&e.getItemStack().is(Content.GUIDE.get())&&mc.screen==null)
   GuideOpener.open(null);
 }
}
