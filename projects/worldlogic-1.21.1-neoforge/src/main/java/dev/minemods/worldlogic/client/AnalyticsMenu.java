package dev.minemods.worldlogic.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
@EventBusSubscriber(modid="worldlogic",value=Dist.CLIENT)
public final class AnalyticsMenu{
 @SubscribeEvent public static void init(ScreenEvent.Init.Post e){
  if(e.getScreen() instanceof PauseScreen s&&Minecraft.getInstance().level!=null){
   e.addListener(Button.builder(Component.translatable("worldlogic.analytics.title"),b->Minecraft.getInstance().setScreen(new AnalyticsScreen(s))).bounds(s.width/2-100,Math.max(4,s.height-28),98,20).build());
   e.addListener(Button.builder(Component.literal(Minecraft.getInstance().getLanguageManager().getSelected().startsWith("ru")?"Справочник":"Guidebook"),b->Minecraft.getInstance().setScreen(new GuideScreen(s))).bounds(s.width/2+2,Math.max(4,s.height-28),98,20).build());
  }
 }
}
