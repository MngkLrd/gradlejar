package dev.creeperarmor.integration.jei;

import dev.creeperarmor.CreeperArmorMod;
import dev.creeperarmor.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;

@JeiPlugin
public final class CreeperJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(CreeperArmorMod.MOD_ID, "jei_information");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(
                List.of(new ItemStack(ModItems.CREEPER_HELMET.get()),
                        new ItemStack(ModItems.CREEPER_CHESTPLATE.get()),
                        new ItemStack(ModItems.CREEPER_LEGGINGS.get()),
                        new ItemStack(ModItems.CREEPER_BOOTS.get())),
                Component.translatable("jei.creeperarmor.armor_info"));
        registration.addItemStackInfo(
                List.of(new ItemStack(ModItems.CREEPER_SHIELD.get())),
                Component.translatable("jei.creeperarmor.shield_info"));
        registration.addItemStackInfo(
                List.of(new ItemStack(ModItems.CREEPER_PLATE.get()),
                        new ItemStack(ModItems.CREEPER_POWDER.get())),
                Component.translatable("jei.creeperarmor.material_info"));
    }
}
