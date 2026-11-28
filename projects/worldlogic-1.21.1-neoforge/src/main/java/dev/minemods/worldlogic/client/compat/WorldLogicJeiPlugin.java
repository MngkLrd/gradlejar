package dev.minemods.worldlogic.client.compat;
import dev.minemods.worldlogic.api.WorldLogicRecipes;
import dev.minemods.worldlogic.api.WorldLogicRecipes.ProcessingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
@JeiPlugin
public final class WorldLogicJeiPlugin implements IModPlugin {
 private static final RecipeType<ProcessingRecipe> TYPE=RecipeType.create("worldlogic","field_processing",ProcessingRecipe.class);
 @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime){
  dev.minemods.worldlogic.client.GuideJeiLookup.install((stack,uses)->{
   runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(uses?RecipeIngredientRole.INPUT:RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,stack));return true;
  });
 }
 @Override public void onRuntimeUnavailable(){dev.minemods.worldlogic.client.GuideJeiLookup.install(null);}
 @Override public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath("worldlogic","processing");}
 @Override public void registerCategories(IRecipeCategoryRegistration registration){registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));}
 @Override public void registerRecipes(IRecipeRegistration registration){registration.addRecipes(TYPE,WorldLogicRecipes.processing());}
 private static final class Category implements IRecipeCategory<ProcessingRecipe>{
  private final IDrawable bg,icon;
  Category(IGuiHelper helper){bg=helper.createBlankDrawable(166,154);icon=helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,new ItemStack(Items.CRAFTING_TABLE));}
  @Override public RecipeType<ProcessingRecipe> getRecipeType(){return TYPE;}
  @Override public Component getTitle(){return Component.translatable("worldlogic.jei.processing");}
  @Override public IDrawable getBackground(){return bg;}
  @Override public IDrawable getIcon(){return icon;}
  @Override public void setRecipe(IRecipeLayoutBuilder builder,ProcessingRecipe recipe,IFocusGroup focuses){
   for(int i=0;i<recipe.slots().size();i++){
    var s=recipe.slots().get(i);var role=switch(s.role()){
     case INPUT->RecipeIngredientRole.INPUT;case OUTPUT->RecipeIngredientRole.OUTPUT;case SUPPORT,TOOL->RecipeIngredientRole.CATALYST;
    };
    builder.addSlot(role,7+(i%6)*26,19+(i/6)*34).addItemStacks(s.alternatives());
   }
  }
  @Override public void draw(ProcessingRecipe recipe,IRecipeSlotsView slots,GuiGraphics g,double mx,double my){
   var f=Minecraft.getInstance().font;
   g.drawString(f,f.plainSubstrByWidth(Component.translatable(recipe.titleKey()).getString(),162),2,2,0xff333333,false);
   for(int i=0;i<recipe.slots().size();i++){
    var s=recipe.slots().get(i);int x=7+(i%6)*26,y=19+(i/6)*34;
    g.drawString(f,f.plainSubstrByWidth(Component.translatable("worldlogic.jei.role."+s.role().name().toLowerCase(java.util.Locale.ROOT)).getString(),26),x-2,y+18,0xff333333,false);
   }
   int y=88;
   for(String key:recipe.stepKeys()){
    for(var line:f.split(Component.translatable(key),162)){if(y>143)return;g.drawString(f,line,2,y,0xff333333,false);y+=10;}
   }
  }
 }
}
