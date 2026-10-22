package dev.minemods.sleepdebt.integration;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@JeiPlugin
public final class SleepJeiPlugin implements IModPlugin {
 private static final RecipeType<BrewRecipes.Brew> TYPE=
  RecipeType.create(SleepDebtMod.MOD_ID,"coffee_maker",BrewRecipes.Brew.class);
 @Override public ResourceLocation getPluginUid(){
  return ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"jei");
 }
 @Override public void registerCategories(IRecipeCategoryRegistration reg){
  reg.addRecipeCategories(new Category(reg.getJeiHelpers().getGuiHelper()));
 }
 @Override public void registerRecipes(IRecipeRegistration reg){
  reg.addRecipes(TYPE,BrewRecipes.all());
 }
 @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration reg){
  reg.addRecipeCatalyst(new ItemStack(ModItems.COFFEE_MACHINE.get()),TYPE);
 }
 private static final class Category implements IRecipeCategory<BrewRecipes.Brew> {
  private final IDrawable bg,icon;
  private Category(IGuiHelper gui) {
   bg=gui.createBlankDrawable(152,58);
   icon=gui.createDrawableItemStack(new ItemStack(ModItems.COFFEE_MACHINE.get()));
  }
  @Override public RecipeType<BrewRecipes.Brew> getRecipeType(){return TYPE;}
  @Override public Component getTitle(){return Component.translatable("container.sleepdebt.coffee_machine");}
  @Override public IDrawable getBackground(){return bg;}
  @Override public IDrawable getIcon(){return icon;}
  @Override public void setRecipe(IRecipeLayoutBuilder b,BrewRecipes.Brew recipe,IFocusGroup focuses){
   b.addSlot(RecipeIngredientRole.INPUT,8,19).addItemStack(new ItemStack(Items.WATER_BUCKET));
   b.addSlot(RecipeIngredientRole.INPUT,32,19).addItemStack(new ItemStack(recipe.ingredient()));
   b.addSlot(RecipeIngredientRole.INPUT,56,19).addItemStacks(
    java.util.List.of(new ItemStack(Items.COAL),new ItemStack(Items.CHARCOAL)));
   b.addSlot(RecipeIngredientRole.OUTPUT,118,19).addItemStack(new ItemStack(recipe.output()));
  }
 }
}

