package dev.minemods.sleepdebt.registry;

import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.menu.CoffeeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
 public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,SleepDebtMod.MOD_ID);
 public static final DeferredHolder<MenuType<?>,MenuType<CoffeeMenu>> COFFEE=
   MENUS.register("coffee_machine",()->new MenuType<>(CoffeeMenu::new,FeatureFlags.DEFAULT_FLAGS));
 private ModMenus(){}
}

