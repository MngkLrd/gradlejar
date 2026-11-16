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
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class OrdinaryFurnaceMixin {
 @Inject(method="canBurn",at=@At("HEAD"),cancellable=true)
 private static void nofoodinfurnace$deny(RegistryAccess registry,RecipeHolder<?> recipe,NonNullList<ItemStack> items,int maxStack,AbstractFurnaceBlockEntity furnace,CallbackInfoReturnable<Boolean> cir) {
  if(!furnace.getBlockState().is(Blocks.FURNACE)||recipe==null)return;
  if(!(recipe.value() instanceof AbstractCookingRecipe cooking))return;
  boolean foodIn=!items.isEmpty()&&items.get(0).has(DataComponents.FOOD);
  if(foodIn||cooking.category()==CookingBookCategory.FOOD||cooking.getResultItem(registry).has(DataComponents.FOOD))cir.setReturnValue(false);
 }
}
