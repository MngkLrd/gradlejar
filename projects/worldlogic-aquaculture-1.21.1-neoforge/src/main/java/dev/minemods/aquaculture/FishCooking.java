package dev.minemods.aquaculture;
import dev.minemods.aquaculture.api.AquaApi;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/** Native cooking types with a dynamic per-item output, including actual mass and food components. */
final class FishCooking {
 static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,Aquaculture.ID);
 static final Supplier<RecipeSerializer<?>> SMELT=SERIALIZERS.register("portion_smelting",()->new SimpleCookingSerializer<>(Smelt::new,200));
 static final Supplier<RecipeSerializer<?>> SMOKE=SERIALIZERS.register("portion_smoking",()->new SimpleCookingSerializer<>(Smoke::new,100));
 static final Supplier<RecipeSerializer<?>> CAMP=SERIALIZERS.register("portion_campfire",()->new SimpleCookingSerializer<>(Camp::new,600));
 private static boolean valid(SingleRecipeInput input){
  return AquaApi.foodPortion(input.item()).filter(p->p.preparation()==AquaApi.Preparation.RAW).isPresent();
 }
 static final class Smelt extends SmeltingRecipe {
  Smelt(String group,CookingBookCategory category,Ingredient in,ItemStack out,float xp,int ticks){super(group,category,in,out,xp,ticks);}
  @Override public RecipeSerializer<?> getSerializer(){return SMELT.get();}
  @Override public boolean matches(SingleRecipeInput input,Level level){return super.matches(input,level)&&valid(input);}
  @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider lookup){return FishPortions.cook(input.item(),super.getResultItem(lookup));}
  @Override public ItemStack getResultItem(HolderLookup.Provider lookup){return FishPortions.preview(super.getResultItem(lookup));}
 }
 static final class Smoke extends SmokingRecipe {
  Smoke(String group,CookingBookCategory category,Ingredient in,ItemStack out,float xp,int ticks){super(group,category,in,out,xp,ticks);}
  @Override public RecipeSerializer<?> getSerializer(){return SMOKE.get();}
  @Override public boolean matches(SingleRecipeInput input,Level level){return super.matches(input,level)&&valid(input);}
  @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider lookup){return FishPortions.cook(input.item(),super.getResultItem(lookup));}
  @Override public ItemStack getResultItem(HolderLookup.Provider lookup){return FishPortions.preview(super.getResultItem(lookup));}
 }
 static final class Camp extends CampfireCookingRecipe {
  Camp(String group,CookingBookCategory category,Ingredient in,ItemStack out,float xp,int ticks){super(group,category,in,out,xp,ticks);}
  @Override public RecipeSerializer<?> getSerializer(){return CAMP.get();}
  @Override public boolean matches(SingleRecipeInput input,Level level){return super.matches(input,level)&&valid(input);}
  @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider lookup){return FishPortions.cook(input.item(),super.getResultItem(lookup));}
  @Override public ItemStack getResultItem(HolderLookup.Provider lookup){return FishPortions.preview(super.getResultItem(lookup));}
 }
}
