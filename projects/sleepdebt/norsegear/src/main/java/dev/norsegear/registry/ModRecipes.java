package dev.norsegear.registry;

import dev.norsegear.NorseGear;
import dev.norsegear.recipe.BogIronRefiningRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, NorseGear.MODID);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<BogIronRefiningRecipe>>
        BOG_IRON_REFINING = SERIALIZERS.register("bog_iron_refining",
            () -> new SimpleCraftingRecipeSerializer<>(BogIronRefiningRecipe::new));
    private ModRecipes() {}
}
