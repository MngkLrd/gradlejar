package dev.ataflore.huss;

import dev.ataflore.huss.training.Training;
import dev.ataflore.huss.item.DisciplineWhistle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(HussMod.ID)
public final class HussMod {
    public static final String ID = "husshusshuss";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ID);
    public static final RegistryObject<Item> WHISTLE = ITEMS.register("discipline_whistle", () -> new DisciplineWhistle(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<SoundEvent> HUSS = SOUNDS.register("huss", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ID, "huss")));
    public HussMod(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        ITEMS.register(bus);
        SOUNDS.register(bus);
        bus.addListener(this::creative);
        MinecraftForge.EVENT_BUS.addListener(Training::onTick);
        MinecraftForge.EVENT_BUS.addListener(Training::onInteract);
    }
    private void creative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(WHISTLE.get());
    }
}
