package dev.ataflore.catchtheblast;
import dev.ataflore.catchtheblast.item.BlastShieldItem;
import dev.ataflore.catchtheblast.combat.*;
import dev.ataflore.catchtheblast.config.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
@Mod("catchtheblast")
public final class CatchTheBlast {
 public static final String ID="catchtheblast";
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
 public static final RegistryObject<Item> BLAST_SHIELD=ITEMS.register("blast_shield",()->new BlastShieldItem(new Item.Properties().durability(512)));
 public CatchTheBlast(){
  var bus=FMLJavaModLoadingContext.get().getModEventBus(); ITEMS.register(bus); bus.addListener(this::creative);
  ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,BlastConfig.SPEC);
  MinecraftForge.EVENT_BUS.addListener(BlastCombat::onBlock);
  MinecraftForge.EVENT_BUS.addListener(TravellingWave::tick);
  MinecraftForge.EVENT_BUS.addListener(TravellingWave::stop);
  MinecraftForge.EVENT_BUS.addListener(BlastCommands::register);
 }
 private void creative(BuildCreativeModeTabContentsEvent e){if(e.getTabKey()==CreativeModeTabs.COMBAT)e.accept(BLAST_SHIELD.get());}
}