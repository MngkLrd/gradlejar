package dev.minemods.nofoodinfurnace;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
public final class FoodRules {
  private FoodRules() {}
  public static boolean forbidden(Level level, ItemStack item) {
    if(item.isEmpty()) return false;
    if(item.has(DataComponents.FOOD)) return true;
    if(!(level instanceof ServerLevel server)) return false;
    var recipe=server.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(item),level);
    if(recipe.isEmpty()) return false;
    var r=recipe.get().value();
    return r.category()==CookingBookCategory.FOOD || r.assemble(new SingleRecipeInput(item)).has(DataComponents.FOOD);
  }
}
