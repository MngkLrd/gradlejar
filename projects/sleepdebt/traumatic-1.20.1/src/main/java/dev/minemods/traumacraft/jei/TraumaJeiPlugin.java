package dev.minemods.traumacraft.jei;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Optional JEI pages. Crafting recipes are discovered by JEI automatically. */
@JeiPlugin
public final class TraumaJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(TraumaCraft.MOD_ID, "medical");
    }
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (var item : ModItems.MEDICINE) {
            var id = item.getId().getPath();
            registration.addItemStackInfo(java.util.List.of(new ItemStack(item.get())),
                    Component.translatable("jei.traumacraft." + id));
        }
        for (var item : ModItems.COMPONENTS) {
            var id = item.getId().getPath();
            registration.addItemStackInfo(new ItemStack(item.get()),
                    Component.translatable("jei.traumacraft." + id));
        }
    }
}
