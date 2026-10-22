package dev.minemods.sleepdebt.client;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.registry.ModEntities;
import dev.minemods.sleepdebt.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
public final class ClientBootstrap {
 public static void register(IEventBus bus,ModContainer container){
  container.registerConfig(ModConfig.Type.CLIENT,ClientSettings.SPEC);
  bus.addListener(ClientBootstrap::onGui);
  bus.addListener(ClientBootstrap::renderers);
  bus.addListener(ClientBootstrap::layers);
  bus.addListener(ClientBootstrap::screens);
  NeoForge.EVENT_BUS.addListener(ClientHallucinations::tick);
 }
 private static void onGui(RegisterGuiLayersEvent e){
  e.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
    ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"sleep_hud"),SleepHud::render);
 }
 private static void screens(RegisterMenuScreensEvent e){
  e.register(ModMenus.COFFEE.get(),CoffeeScreen::new);
 }
 private static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){
  e.registerLayerDefinition(SleeplessModel.LAYER,SleeplessModel::createLayer);
 }
 private static void renderers(EntityRenderersEvent.RegisterRenderers e){
  e.registerEntityRenderer(ModEntities.SLEEPLESS.get(),SleeplessRenderer::new);
 }
 private ClientBootstrap(){}
}


