package dev.worldfold;
import dev.worldfold.item.FoldCompass;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;
@Mod("worldfold")
public final class WorldFold {
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("worldfold");
 public static final DeferredItem<FoldCompass> COMPASS=ITEMS.register("fold_compass",()->new FoldCompass(new Item.Properties().stacksTo(1)));
 public WorldFold(IEventBus bus){ITEMS.register(bus);bus.addListener(this::tab);}
 private void tab(BuildCreativeModeTabContentsEvent e){if(e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES))e.accept(COMPASS.get());}
}