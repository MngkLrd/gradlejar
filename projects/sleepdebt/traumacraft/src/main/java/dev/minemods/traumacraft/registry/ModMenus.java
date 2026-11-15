package dev.minemods.traumacraft.registry;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.menu.MedicalPouchMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS=
        DeferredRegister.create(Registries.MENU,TraumaCraft.MOD_ID);
    public static final DeferredHolder<MenuType<?>,MenuType<MedicalPouchMenu>> MEDICAL_POUCH=
        MENUS.register("medical_pouch",()->new MenuType<>(MedicalPouchMenu::new,FeatureFlags.DEFAULT_FLAGS));
    private ModMenus(){}
    public static void register(IEventBus bus){MENUS.register(bus);}
}
