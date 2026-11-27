package dev.minemods.traumacraft.registry;

import dev.minemods.traumacraft.TraumaCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TraumaCraft.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MEDICINE =
            TABS.register("medical", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.traumacraft.medical"))
                    .icon(() -> ModItems.MEDKIT.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        for (var supply : ModItems.COMPONENTS) output.accept(supply.get());
                        for (var treatment : ModItems.MEDICINE) output.accept(treatment.get());
                    })
                    .build());

    private ModCreativeTabs() {}
    public static void register(IEventBus bus) { TABS.register(bus); }
}
