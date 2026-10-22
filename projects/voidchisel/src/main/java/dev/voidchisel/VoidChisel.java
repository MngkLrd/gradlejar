package dev.voidchisel;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(VoidChisel.MOD_ID)
public final class VoidChisel {
    public static final String MOD_ID = "voidchisel";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final Supplier<Item> VOID_CHISEL = ITEMS.register("void_chisel",
            () -> new VoidChiselItem(new Item.Properties().durability(5).rarity(Rarity.EPIC)));

    public VoidChisel(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(this::addCreativeTab);
        NeoForge.EVENT_BUS.addListener(BedrockMiningEvents::onLeftClickBlock);
    }

    private void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(VOID_CHISEL.get());
        }
    }
}
