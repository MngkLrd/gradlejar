package dev.minemods.aquaculture;
import dev.minemods.aquaculture.api.AquaApi;
import dev.minemods.worldlogic.api.WorldLogicRecipes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import java.util.*;

final class FishPortions {
 private FishPortions(){}
 static final int PORTION_G=100;
 // Gameplay fillet-yield priors, not chemical analyses. Bones, skin and viscera are discarded.
 static double edibleYield(Species s){return switch(s.id()){
  case "halibut","flounder","tuna","mahi_mahi","arapaima"->.60;
  case "carp","crucian_carp","bream","pacu","piranha","loach","rotan"->.40;
  default->.50;
 };}
 static int edibleGrams(ItemStack catchStack,Species s){
  if(!catchStack.is(Aquaculture.RAW.get(s.id()).get())||catchStack.getCount()!=1)return 0;
  var tag=catchStack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
  if(!tag.contains("wl_fish_mass_kg",Tag.TAG_ANY_NUMERIC))return 0;
  double kg=tag.getDouble("wl_fish_mass_kg");
  if(!Double.isFinite(kg)||kg<=0||kg>s.maxKg()*1.01)return 0;
  return (int)Math.floor(kg*1000*edibleYield(s));
 }
 static ItemStack stack(Species s,AquaApi.Preparation prep,int grams){
  var map=prep==AquaApi.Preparation.RAW?Aquaculture.RAW_PORTIONS:Aquaculture.COOKED_PORTIONS;
  if(grams<1||grams>(prep==AquaApi.Preparation.RAW?PORTION_G:80))return ItemStack.EMPTY;
  ItemStack item=new ItemStack(map.get(s.id()).get());
  var tag=new CompoundTag();tag.putInt("wl_portion_grams",grams);item.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
  // Vanilla hunger is a bounded compatibility fallback, NOT the nutrition/calorie model.
  int food=Math.max(1,(int)Math.ceil(grams*(prep==AquaApi.Preparation.RAW?.02:.05)));
  item.set(DataComponents.FOOD,new FoodProperties.Builder().nutrition(food).saturationModifier(prep==AquaApi.Preparation.RAW?.15f:.6f).build());
  return item;
 }
 static List<ItemStack> split(ItemStack whole,Species s){
  int grams=edibleGrams(whole,s);if(grams<1)return List.of();
  List<ItemStack> out=new ArrayList<>();int full=grams/PORTION_G;
  while(full>0){int n=Math.min(full,64);ItemStack st=stack(s,AquaApi.Preparation.RAW,PORTION_G);st.setCount(n);out.add(st);full-=n;}
  if(grams%PORTION_G>0)out.add(stack(s,AquaApi.Preparation.RAW,grams%PORTION_G));
  return List.copyOf(out);
 }
 static ItemStack cook(ItemStack raw,ItemStack expected){
  var p=AquaApi.foodPortion(raw);if(p.isEmpty()||p.get().preparation()!=AquaApi.Preparation.RAW)return ItemStack.EMPTY;
  Species s=Species.BY_ID.get(p.get().speciesId());
  if(s==null||!expected.is(Aquaculture.COOKED_PORTIONS.get(s.id()).get()))return ItemStack.EMPTY;
  return stack(s,AquaApi.Preparation.COOKED,Math.max(1,p.get().edibleMassGrams()*4/5));
 }
 static ItemStack preview(ItemStack expected){
  if(!(expected.getItem() instanceof FishPortion p)||p.preparation!=AquaApi.Preparation.COOKED)return ItemStack.EMPTY;
  return stack(p.species,AquaApi.Preparation.COOKED,80);
 }
 static List<WorldLogicRecipes.ProcessingRecipe> processing(){
  List<WorldLogicRecipes.ProcessingRecipe> out=new ArrayList<>();
  for(Species s:Species.ALL)out.add(new WorldLogicRecipes.ProcessingRecipe(
    ResourceLocation.fromNamespaceAndPath(Aquaculture.MOD_ID,"cut_"+s.id()),"aquaculture.processing.cut",
    List.of(new WorldLogicRecipes.Slot(WorldLogicRecipes.Role.INPUT,List.of(new ItemStack(Aquaculture.RAW.get(s.id()).get()))),
     new WorldLogicRecipes.Slot(WorldLogicRecipes.Role.TOOL,List.of(new ItemStack(Items.SHEARS))),
     new WorldLogicRecipes.Slot(WorldLogicRecipes.Role.OUTPUT,List.of(stack(s,AquaApi.Preparation.RAW,100)))),
    List.of("aquaculture.processing.cut_step","aquaculture.processing.variable_yield")));
  return List.copyOf(out);
 }
}
