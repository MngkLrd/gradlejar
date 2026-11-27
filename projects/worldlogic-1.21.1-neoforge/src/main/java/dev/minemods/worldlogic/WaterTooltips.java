package dev.minemods.worldlogic;
import java.util.Locale;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BucketItem;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="worldlogic")
public final class WaterTooltips{
 @SubscribeEvent public static void tooltip(ItemTooltipEvent e){
  for(var f:NaturalFluids.families())if(e.getItemStack().is(f.bucket.get())){
   var w=WorldLogicApi.waterProfile(f.source.get());
   e.getToolTip().add(Component.translatable("worldlogic.water.quality."+w.quality().name().toLowerCase(Locale.ROOT)));
   break;
  }
 }
}
