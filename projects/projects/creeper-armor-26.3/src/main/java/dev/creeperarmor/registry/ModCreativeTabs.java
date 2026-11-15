package dev.creeperarmor.registry;

import dev.creeperarmor.CreeperArmorMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreeperArmorMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREEPER_ARMOR =
            TABS.register("creeper_armor", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.creeperarmor.creeper_armor"))
                    .icon(() -> new ItemStack(ModItems.CREEPER_CHESTPLATE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CREEPER_PLATE.get());
                        output.accept(ModItems.CREEPER_POWDER.get());
                        output.accept(ModItems.CREEPER_HELMET.get());
                        output.accept(ModItems.CREEPER_CHESTPLATE.get());
                        output.accept(ModItems.CREEPER_LEGGINGS.get());
                        output.accept(ModItems.CREEPER_BOOTS.get());
                        output.accept(ModItems.CREEPER_SHIELD.get());
                    }).build());

    private ModCreativeTabs() {}
}
