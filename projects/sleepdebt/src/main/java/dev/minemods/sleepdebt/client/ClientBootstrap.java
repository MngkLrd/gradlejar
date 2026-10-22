package dev.minemods.sleepdebt.client;
import dev.minemods.sleepdebt.SleepDebtMod;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
public final class ClientBootstrap {
 public static void register(IEventBus bus,ModContainer container){
  container.registerConfig(ModConfig.Type.CLIENT,ClientSettings.SPEC);
  bus.addListener(ClientBootstrap::onGui);
 }
 private static void onGui(RegisterGuiLayersEvent e){
  e.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
   ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"sleep_hud"),SleepHud::render);
 }
 private ClientBootstrap(){}
}
