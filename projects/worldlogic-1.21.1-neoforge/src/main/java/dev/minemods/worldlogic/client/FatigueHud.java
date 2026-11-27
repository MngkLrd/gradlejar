package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid="worldlogic",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FatigueHud {
 private static final ResourceLocation EMPTY=id("energy_empty"),HALF=id("energy_half"),FULL=id("energy_full");
 private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath("worldlogic","hud/"+name);}
 private FatigueHud(){}
 @SubscribeEvent public static void register(RegisterGuiLayersEvent e){
  e.registerAbove(VanillaGuiLayers.ARMOR_LEVEL,id("stamina"),FatigueHud::render);
 }
 public static void render(GuiGraphics g,DeltaTracker tick){
  var mc=Minecraft.getInstance();var p=mc.player;
  if(p==null||!p.isAlive()||p.isCreative()||p.isSpectator()||mc.options.hideGui||mc.gameMode==null||!mc.gameMode.canHurtPlayer())return;
  var value=WorldLogicApi.clientFatiguePercent();if(value.isEmpty())return;
  int units=dev.minemods.worldlogic.StaminaScale.halfUnits(value.getAsInt());
  int x=g.guiWidth()/2-91,y=g.guiHeight()-mc.gui.leftHeight;
  for(int i=0;i<10;i++){
   int u=units-i*2;
   g.blitSprite(u>=2?FULL:u==1?HALF:EMPTY,x+i*8,y,9,9);
  }
  mc.gui.leftHeight+=10;
 }
}
