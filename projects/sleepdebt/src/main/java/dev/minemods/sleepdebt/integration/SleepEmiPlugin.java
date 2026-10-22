package dev.minemods.sleepdebt.integration;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.registry.ModItems;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import java.util.List;

@EmiEntrypoint
public final class SleepEmiPlugin implements EmiPlugin {
 private static final EmiRecipeCategory CATEGORY=new EmiRecipeCategory(
  ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"brewing"),
  EmiStack.of(ModItems.COFFEE_MACHINE.get()));
 @Override public void register(EmiRegistry reg){
  reg.addCategory(CATEGORY);
  reg.addWorkstation(CATEGORY,EmiStack.of(ModItems.COFFEE_MACHINE.get()));
  for(BrewRecipes.Brew recipe:BrewRecipes.all())reg.addRecipe(new Entry(recipe));
 }
 private static final class Entry implements EmiRecipe {
  private final BrewRecipes.Brew data;
  private Entry(BrewRecipes.Brew data){this.data=data;}
  @Override public EmiRecipeCategory getCategory(){return CATEGORY;}
  @Override public ResourceLocation getId(){
   return ResourceLocation.fromNamespaceAndPath(SleepDebtMod.MOD_ID,"machine/"+data.name());
  }
  @Override public List<EmiIngredient> getInputs(){
   return List.of(EmiStack.of(Items.WATER_BUCKET),EmiStack.of(data.ingredient()),
      EmiIngredient.of(List.of(EmiStack.of(Items.COAL),EmiStack.of(Items.CHARCOAL))));
  }
  @Override public List<EmiStack> getOutputs(){return List.of(EmiStack.of(data.output()));}
  @Override public int getDisplayWidth(){return 142;}
  @Override public int getDisplayHeight(){return 55;}
  @Override public void addWidgets(WidgetHolder holder){
   holder.addSlot(EmiStack.of(Items.WATER_BUCKET),4,17);
   holder.addSlot(EmiStack.of(data.ingredient()),29,17);
   holder.addSlot(EmiIngredient.of(List.of(EmiStack.of(Items.COAL),EmiStack.of(Items.CHARCOAL))),54,17);
   holder.addSlot(EmiStack.of(data.output()),117,17).recipeContext(this);
  }
 }
}

