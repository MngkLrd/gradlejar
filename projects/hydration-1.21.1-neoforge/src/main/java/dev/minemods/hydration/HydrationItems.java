package dev.minemods.hydration;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HydrationItems {
 private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("hydration");
 private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,"hydration");
 public static final net.neoforged.neoforge.registries.DeferredItem<VesselItem> CUP=ITEMS.register("wooden_cup",()->new VesselItem(500,false));
 public static final net.neoforged.neoforge.registries.DeferredItem<VesselItem> SKIN=ITEMS.register("waterskin",()->new VesselItem(1500,false));
 public static final net.neoforged.neoforge.registries.DeferredItem<VesselItem> JUG=ITEMS.register("clay_jug",()->new VesselItem(3000,true));
 public static final net.neoforged.neoforge.registries.DeferredItem<VesselItem> CANTEEN=ITEMS.register("copper_canteen",()->new VesselItem(2000,true));
 public static final net.neoforged.neoforge.registries.DeferredItem<Item> FILTER=ITEMS.registerSimpleItem("charcoal_filter",new Item.Properties());
 public static final net.neoforged.neoforge.registries.DeferredItem<Item> CONDENSER=ITEMS.registerSimpleItem("copper_condenser",new Item.Properties().stacksTo(1));
 private static final net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab,CreativeModeTab> TAB=TABS.register("hydration",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.hydration")).icon(()->SKIN.get().getDefaultInstance()).displayItems((p,o)->{o.accept(CUP.get());o.accept(SKIN.get());o.accept(JUG.get());o.accept(CANTEEN.get());o.accept(FILTER.get());o.accept(CONDENSER.get());WaterInfrastructure.items().forEach(x->o.accept(x.get()));}).build());
 public static void register(IEventBus bus){WaterInfrastructure.register(bus);ITEMS.register(bus);TABS.register(bus);}
}
