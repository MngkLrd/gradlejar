package dev.minemods.redstonevision;

import dev.minemods.redstonevision.menu.GogglesChargeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private ModMenus() {}
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RedstoneVision.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<GogglesChargeMenu>> CHARGER =
        MENUS.register("goggles_charger", () -> new MenuType<>(GogglesChargeMenu::new, FeatureFlags.DEFAULT_FLAGS));
}
