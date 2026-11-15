package dev.norsegear.recipe;

import dev.norsegear.registry.ModItems;
import dev.norsegear.registry.ModRecipes;
import dev.norsegear.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** 1 bog-iron nodule + 1 stone-or-better pickaxe -> 1 iron ingot.
 * The pickaxe is returned with +1 damage (unless it breaks).
 * No ordinary furnace processing is defined, by design.
 */
public final class BogIronRefiningRecipe extends CustomRecipe {
    public BogIronRefiningRecipe(CraftingBookCategory category) { super(category); }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.size() < 2) return false;
        int bog = 0;
        int pickaxes = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.BOG_IRON.get())) bog++;
            else if (stack.is(ModTags.BOG_IRON_PICKAXES)
                    && stack.isDamageableItem()
                    && stack.getDamageValue() < stack.getMaxDamage()) pickaxes++;
            else return false;
        }
        return bog == 1 && pickaxes == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int bog = 0, pickaxes = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.BOG_IRON.get())) bog++;
            else if (stack.is(ModTags.BOG_IRON_PICKAXES) && stack.isDamageableItem()
                    && stack.getDamageValue() < stack.getMaxDamage()) pickaxes++;
            else return ItemStack.EMPTY;
        }
        return bog == 1 && pickaxes == 1 ? new ItemStack(Items.IRON_INGOT) : ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remainder = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ModTags.BOG_IRON_PICKAXES) && stack.isDamageableItem()) {
                ItemStack pickaxe = stack.copyWithCount(1);
                int damage = pickaxe.getDamageValue() + 1;
                if (damage < pickaxe.getMaxDamage()) {
                    pickaxe.setDamageValue(damage);
                    remainder.set(i, pickaxe);
                }
            }
        }
        return remainder;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipes.BOG_IRON_REFINING.get(); }
}
