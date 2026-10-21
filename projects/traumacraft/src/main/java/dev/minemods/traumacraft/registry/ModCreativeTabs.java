package dev.minemods.traumacraft.registry;

import dev.minemods.traumacraft.TraumaCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TraumaCraft.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MEDICINE =
            TABS.register("medical", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.traumacraft.medical"))
                    .icon(() -> ModItems.MEDKIT.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.BANDAGE.get());
                        output.accept(ModItems.SPLINT.get());
                        output.accept(ModItems.MEDKIT.get());
                    })
                    .build());

    private ModCreativeTabs() {}
    public static void register(IEventBus bus) { TABS.register(bus); }
}
