package dev.anvilborn.registry;

import dev.anvilborn.Anvilborn;
import dev.anvilborn.forge.ForgeMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import java.util.function.Supplier;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER =
        DeferredRegister.create(Registries.MENU, Anvilborn.MODID);
    public static final Supplier<MenuType<ForgeMenu>> FORGE = REGISTER.register(
        "forge", () -> new MenuType<>(ForgeMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    private ModMenus() {}
}
