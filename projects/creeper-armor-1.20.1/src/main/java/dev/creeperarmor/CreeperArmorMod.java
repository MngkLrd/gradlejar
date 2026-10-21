package dev.creeperarmor;
import dev.creeperarmor.gameplay.CreeperGameplayEvents;
import dev.creeperarmor.registry.ModItems;
import dev.creeperarmor.registry.ModCreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
@Mod(CreeperArmorMod.MOD_ID)
public final class CreeperArmorMod {
    public static final String MOD_ID="creeperarmor";
    public CreeperArmorMod() {
        IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus); ModCreativeTabs.TABS.register(bus);
        MinecraftForge.EVENT_BUS.register(new CreeperGameplayEvents());
    }
}
