package dev.minemods.nofoodinfurnace.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops edible recipes before the ordinary furnace consumes new fuel or advances cooking.
 * Does not remove shared recipes, so smokers/campfires and modded machines keep working.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class OrdinaryFurnaceMixin {
    @Inject(method = "canBurn", at = @At("HEAD"), cancellable = true)
    private static void nofoodinfurnace$rejectFood(
            RegistryAccess registryAccess,
            RecipeHolder<?> holder,
            NonNullList<ItemStack> inventory,
            int maximumStackSize,
            AbstractFurnaceBlockEntity furnace,
            CallbackInfoReturnable<Boolean> callback) {

        // The same block entity base class is also used by smokers and blast furnaces.
        if (!furnace.getBlockState().is(Blocks.FURNACE) || holder == null) {
            return;
        }

        // A custom smelting recipe must still be allowed unless it handles food.
        if (!(holder.value() instanceof AbstractCookingRecipe cooking)) {
            return;
        }

        // Some modded recipes omit the FOOD category, so check actual item
        // components as well. Conversely, category FOOD catches non-edible ingredients.
        boolean foodCategory = cooking.category() == CookingBookCategory.FOOD;
        boolean edibleInput = !inventory.isEmpty()
                && inventory.get(0).has(DataComponents.FOOD);
        boolean edibleOutput = cooking.getResultItem(registryAccess).has(DataComponents.FOOD);

        if (foodCategory || edibleInput || edibleOutput) {
            callback.setReturnValue(false);
        }
    }
}
