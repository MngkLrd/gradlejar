package dev.norsegear.integration.jei;

import dev.norsegear.NorseGear;
import dev.norsegear.registry.ModItems;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/** Optional JEI integration; the regular crafting recipe works without JEI installed.
 * We expose a VIEW of the special tool-preserving recipe in JEI's crafting category.
 */
@JeiPlugin
public final class NorseJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(NorseGear.MODID, "jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        NonNullList<Ingredient> inputs = NonNullList.create();
        inputs.add(Ingredient.of(ModItems.BOG_IRON.get()));
        inputs.add(Ingredient.of(Items.STONE_PICKAXE, Items.IRON_PICKAXE,
            Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE));
        ShapelessRecipe display = new ShapelessRecipe("norse_bog_iron", CraftingBookCategory.MISC,
            new ItemStack(Items.IRON_INGOT), inputs);
        registration.addRecipes(RecipeTypes.CRAFTING, List.of(new RecipeHolder<CraftingRecipe>(
            ResourceLocation.fromNamespaceAndPath(NorseGear.MODID, "jei_bog_iron_refining"), display)));
        registration.addIngredientInfo(new ItemStack(ModItems.BOG_IRON.get()), VanillaTypes.ITEM_STACK,
            Component.translatable("jei.norsegear.bog_iron_info"));
    }
}
