package dev.minemods.sleepdebt.client;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.registry.ModEntities;
import dev.minemods.sleepdebt.registry.ModMenus;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
public final class ClientBootstrap {
 public static void register(IEventBus bus,ModContainer container){
  container.registerConfig(ModConfig.Type.CLIENT,ClientSettings.SPEC);
  container.registerExtensionPoint(IConfigScreenFactory.class,ConfigurationScreen::new);
  bus.addListener(ClientBootstrap::onGui);
  bus.addListener(ClientBootstrap::screens);
  bus.addListener(ClientBootstrap::renderers);
  net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(JournalInventoryTab::init);
 }
 private static void onGui(RegisterGuiLayersEvent e){
  e.registerAbove(VanillaGuiLayers.FOOD_LEVEL,ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"sleep_hud"),SleepHud::render);
 }
 private static void screens(RegisterMenuScreensEvent e){e.register(ModMenus.COFFEE.get(),CoffeeScreen::new);}
 private static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(ModEntities.SLEEPLESS.get(),NoopRenderer::new);}
 private ClientBootstrap(){}
}
