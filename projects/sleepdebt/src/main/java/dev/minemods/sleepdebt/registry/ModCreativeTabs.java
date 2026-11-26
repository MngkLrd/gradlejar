package dev.minemods.sleepdebt.registry;

import dev.minemods.sleepdebt.SleepDebtMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The full Sleep Debt catalog in one discoverable Creative Mode tab.
 * The order follows the natural loop: farming -> brewing -> camping -> dreams.
 * This is a registry entry, not a BuildCreativeModeTabContentsEvent injection.
 */
public final class ModCreativeTabs {
 public static final DeferredRegister<CreativeModeTab> TABS =
  DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SleepDebtMod.MOD_ID);

 public static final DeferredHolder<CreativeModeTab,CreativeModeTab> SLEEP_DEBT =
  TABS.register("main", () -> CreativeModeTab.builder()
    .title(Component.translatable("itemGroup.sleepdebt.main"))
    .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
    .icon(() -> new ItemStack(ModItems.SLEEP_JOURNAL.get()))
    .displayItems((params, output) -> {
     // Planting, harvesting and initial production.
     output.accept(ModItems.COFFEE_SEEDS.get());
     output.accept(ModItems.COFFEE_BERRIES.get());
     output.accept(ModItems.ROASTED_COFFEE_BEANS.get());
     output.accept(ModItems.ROASTED.get());
     output.accept(ModItems.GROUNDS.get());
     output.accept(ModItems.DRIED_HERBS.get());

     // Furniture and field camp: actual interactive blocks first.
     output.accept(ModItems.COFFEE_MACHINE.get());
     output.accept(ModItems.BEDROLL.get());
     output.accept(ModItems.AWNING.get());
     output.accept(ModItems.CAMP_LANTERN.get());
     output.accept(ModItems.INCENSE_BURNER.get());

     // Equipment, consumables and the personal report.
     output.accept(ModItems.SLEEP_JOURNAL.get());
     output.accept(ModItems.PILLOW.get());
     output.accept(ModItems.MASK.get());
     output.accept(ModItems.INCENSE_BUNDLE.get());
     output.accept(ModItems.HERBAL_POULTICE.get());
     output.accept(ModItems.COFFEE.get());
     output.accept(ModItems.STRONG.get());
     output.accept(ModItems.LONG.get());
     output.accept(ModItems.CARAMEL_COFFEE.get());
     output.accept(ModItems.ENERGY.get());
     output.accept(ModItems.ENERGY_LONG.get());
     output.accept(ModItems.ENERGY_STRONG.get());
     output.accept(ModItems.WARM_MILK.get());
     output.accept(ModItems.HERBAL_TEA.get());
     output.accept(ModItems.DREAM_TEA.get());
     output.accept(ModItems.BITTER_TONIC.get());
     output.accept(ModItems.COCOA_BISCUIT.get());

     // Dream-specific content and testing the hostile mob.

    }).build());

 private ModCreativeTabs(){}
}
