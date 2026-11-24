package dev.ataflore.catchtheblast;
import dev.ataflore.catchtheblast.item.BlastShieldItem;
import dev.ataflore.catchtheblast.combat.*;
import dev.ataflore.catchtheblast.config.*;
import net.minecraft.world.item.*;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
@Mod("catchtheblast")
public final class CatchTheBlast {
 public static final String ID="catchtheblast";
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
 public static final DeferredItem<BlastShieldItem> BLAST_SHIELD=ITEMS.registerItem("blast_shield",BlastShieldItem::new,BlastShieldItem::configure);
 public CatchTheBlast(IEventBus bus,ModContainer container){
  ITEMS.register(bus);container.registerConfig(ModConfig.Type.SERVER,BlastConfig.SPEC);bus.addListener(this::creative);
  NeoForge.EVENT_BUS.addListener(BlastCombat::onBlock);NeoForge.EVENT_BUS.addListener(TravellingWave::tick);NeoForge.EVENT_BUS.addListener(TravellingWave::stop);NeoForge.EVENT_BUS.addListener(BlastCommands::register);
  if(FMLEnvironment.getDist()==Dist.CLIENT)dev.ataflore.catchtheblast.client.ClientSetup.register(bus);
 }
 private void creative(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.COMBAT)e.accept(BLAST_SHIELD.get());}
}