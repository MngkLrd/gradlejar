package dev.minemods.redstonevision;

import dev.minemods.redstonevision.network.ToggleVisionPayload;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(RedstoneVision.MOD_ID)
public final class RedstoneVision {
    public static final String MOD_ID = "redstonevision";
    public RedstoneVision(IEventBus bus) {
        ModDataComponents.COMPONENTS.register(bus);
        ModItems.ARMOR_MATERIALS.register(bus);
        ModItems.ITEMS.register(bus);
        ModMenus.MENUS.register(bus);
        bus.addListener(ToggleVisionPayload::register);
        bus.addListener(this::creative);
    }
    private void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(ModItems.GOGGLES.get());
    }
}
