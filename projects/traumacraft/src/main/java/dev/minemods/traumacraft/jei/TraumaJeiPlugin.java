package dev.minemods.traumacraft.jei;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Optional JEI plugin. Standard crafting recipes are discovered automatically by JEI. */
@JeiPlugin
public final class TraumaJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID, "medical");
    }
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(new ItemStack(ModItems.BANDAGE.get()),
                Component.translatable("jei.traumacraft.bandage"));
        registration.addItemStackInfo(new ItemStack(ModItems.SPLINT.get()),
                Component.translatable("jei.traumacraft.splint"));
        registration.addItemStackInfo(new ItemStack(ModItems.MEDKIT.get()),
                Component.translatable("jei.traumacraft.medkit"));
    }
}
