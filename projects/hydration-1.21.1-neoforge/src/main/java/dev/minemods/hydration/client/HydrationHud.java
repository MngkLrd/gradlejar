package dev.minemods.hydration.client;
import dev.minemods.hydration.HydrationSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid="hydration",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HydrationHud {
 @SubscribeEvent public static void register(RegisterGuiLayersEvent e){
  e.registerAbove(VanillaGuiLayers.FOOD_LEVEL,ResourceLocation.fromNamespaceAndPath("hydration","thirst"),HydrationHud::render);
 }
 private static void render(GuiGraphics g,DeltaTracker tick){
  var mc=Minecraft.getInstance();var p=mc.player;int units=HydrationSync.clientUnits;
  if(p==null||!p.isAlive()||p.isCreative()||p.isSpectator()||mc.options.hideGui||mc.gameMode==null||!mc.gameMode.canHurtPlayer()||units<0)return;
  int x=g.guiWidth()/2+91-9,y=g.guiHeight()-mc.gui.rightHeight;
  for(int i=0;i<10;i++)drop(g,x-i*8,y,Math.max(0,Math.min(2,units-i*2)));
  mc.gui.rightHeight+=10;
 }
 // Deliberate 7x8 pixel silhouette; no antialiasing or scaled textures.
 private static void drop(GuiGraphics g,int x,int y,int u){
  int edge=0xff162432,empty=0xff3d4651,water=0xff2589c7,shadow=0xff225992,light=0xff9adcf0;
  g.fill(x+3,y,x+4,y+1,edge);g.fill(x+2,y+1,x+5,y+2,edge);
  g.fill(x+1,y+2,x+6,y+4,edge);g.fill(x,y+4,x+7,y+7,edge);g.fill(x+1,y+7,x+6,y+8,edge);
  g.fill(x+3,y+1,x+4,y+2,u==2?light:empty);
  g.fill(x+2,y+2,x+5,y+4,u==2?water:empty);
  g.fill(x+1,y+4,x+6,y+7,u>0?water:empty);
  if(u>0){g.fill(x+1,y+4,x+2,y+6,light);g.fill(x+5,y+5,x+6,y+7,shadow);g.fill(x+3,y+6,x+6,y+7,shadow);}
 }
}
