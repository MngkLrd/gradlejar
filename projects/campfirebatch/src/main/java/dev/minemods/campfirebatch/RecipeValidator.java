package dev.minemods.campfirebatch;

import java.util.OptionalInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;

final class RecipeValidator {
    private RecipeValidator() {}
    static OptionalInt cookingTime(CampfireBlockEntity fire, ItemStack stack) {
        if (stack.isEmpty()) return OptionalInt.empty();
        var recipe = fire.getCookableRecipe(stack);
        return recipe.isPresent()
                ? OptionalInt.of(recipe.get().value().getCookingTime())
                : OptionalInt.empty();
    }
}
