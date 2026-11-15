package dev.norsegear.registry;

import dev.norsegear.NorseGear;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NorseGear.MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
        TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.norsegear"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.BEARDED_AXE.get().getDefaultInstance())
            .displayItems((parameters, out) -> {
                out.accept(ModItems.BOG_IRON.get());
                out.accept(ModItems.FORGED_IRON.get());
                out.accept(ModItems.LEATHER_BINDING.get());
                out.accept(ModItems.CARVED_HORN.get());
                out.accept(ModItems.SEAX.get());
                out.accept(ModItems.BEARDED_AXE.get());
                out.accept(ModItems.DANE_AXE.get());
                out.accept(ModItems.VIKING_SWORD.get());
                out.accept(ModItems.SPEAR.get());
                out.accept(ModItems.JAVELIN.get());
                out.accept(ModItems.THROWING_AXE.get());
                out.accept(ModItems.ROUND_SHIELD.get());
                out.accept(ModItems.CELTIC_SHIELD.get());
                out.accept(ModItems.BOSSED_SHIELD.get());
                out.accept(ModItems.SLING.get());
                out.accept(ModItems.STONE_SHOT.get());
                out.accept(ModItems.NORSE_HELMET.get());
                out.accept(ModItems.NORSE_CHESTPLATE.get());
                out.accept(ModItems.NORSE_LEGGINGS.get());
                out.accept(ModItems.NORSE_BOOTS.get());
                out.accept(ModItems.FIELD_WHETSTONE.get());
                out.accept(ModItems.WAR_HORN.get());
                out.accept(ModItems.TRAVEL_RATION.get());
                out.accept(ModItems.PITCH_FLASK.get());
            }).build());
    private ModTabs() {}
}
