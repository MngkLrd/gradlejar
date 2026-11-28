package dev.minemods.aquaculture;
import dev.minemods.aquaculture.api.AquaApi;
import dev.minemods.worldlogic.api.PhysicalMaterials;
import dev.minemods.worldlogic.api.WorldLogicCapabilities;
import dev.minemods.worldlogic.api.WorldLogicRecipes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.item.Items;
import java.util.*;
final class PortionChecks{
 private static void require(boolean good,String msg){if(!good)throw new IllegalStateException("Portion: "+msg);}
 static void run(ServerLevel l){
  var api=WorldLogicCapabilities.find(AquaApi.FOOD_PORTION,0).orElseThrow();
  require(WorldLogicRecipes.processing().stream().filter(r->r.id().getNamespace().equals(Aquaculture.MOD_ID)).count()==36,"Core processing catalogue");
  for(Species s:Species.ALL){
   var fish=(Fish)Aquaculture.TYPES.get(s.id()).get().create(l);
   var whole=Aquaculture.catchStack(fish,s);fish.discard();
   var tag=whole.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putDouble("wl_fish_mass_kg",Math.min(s.maxKg(),1.137));whole.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
   require(api.foodPortion(whole).isEmpty()&&whole.getFoodProperties(null)==null,"whole non-edible "+s.id());
   var portions=FishPortions.split(whole,s);int expected=FishPortions.edibleGrams(whole,s);
   int actual=portions.stream().mapToInt(p->api.foodPortion(p).orElseThrow().edibleMassGrams()*p.getCount()).sum();
   require(actual==expected&&actual<=tag.getDouble("wl_fish_mass_kg")*1000&&!portions.isEmpty(),"mass conservation "+s.id());
   require(whole.getCount()==1,"pure split "+s.id());
   var blank=new ItemStack(Aquaculture.RAW_PORTIONS.get(s.id()).get());require(api.foodPortion(blank).isEmpty()&&blank.getFoodProperties(null)==null,"missing mass "+s.id());
   for(int g:new int[]{17,100}){
    var raw=FishPortions.stack(s,AquaApi.Preparation.RAW,g);raw.setCount(2);
    require(api.foodPortion(raw).orElseThrow().edibleMassGrams()==g&&raw.getFoodProperties(null)!=null,"raw per item "+s.id());
    require(Math.abs(PhysicalMaterials.stackResult(raw).massKg()-g*2/1000.0)<1e-8,"raw physical mass "+s.id());
    for(var type:List.of(RecipeType.SMELTING,RecipeType.SMOKING,RecipeType.CAMPFIRE_COOKING)){
     // Generic native type lookup using typed helper below.
     var cooked=cook(l,type,new SingleRecipeInput(raw));
     var p=api.foodPortion(cooked).orElseThrow();
     require(p.speciesId().equals(s.id())&&p.preparation()==AquaApi.Preparation.COOKED&&p.edibleMassGrams()==g*4/5&&cooked.getCount()==1&&cooked.getFoodProperties(null)!=null,"cooking "+s.id()+" "+type);
     require(Math.abs(PhysicalMaterials.itemResult(cooked).massKg()-p.edibleMassGrams()/1000.0)<1e-8,"cooked physical mass");
    }
   }
   for(String prefix:List.of("raw_","cooked_"))require(PortionChecks.class.getResource("/assets/aquaculture/models/item/"+prefix+s.id()+"_portion.json")!=null,"model "+s.id());
  }
  var p=new BlockPos(8,117,8);require(l.hasChunkAt(p),"loaded furnace fixture");
  var saved=l.getBlockState(p);
  try{
   l.setBlock(p,Blocks.FURNACE.defaultBlockState(),3);
   var furnace=(AbstractFurnaceBlockEntity)l.getBlockEntity(p);
   var s=Species.BY_ID.get("halibut");
   furnace.setItem(0,FishPortions.stack(s,AquaApi.Preparation.RAW,37));
   furnace.setItem(1,new ItemStack(Items.COAL));
   for(int i=0;i<220;i++)AbstractFurnaceBlockEntity.serverTick(l,p,l.getBlockState(p),furnace);
   var cooked=api.foodPortion(furnace.getItem(2)).orElseThrow();
   require(cooked.edibleMassGrams()==29&&cooked.speciesId().equals("halibut")&&furnace.getItem(0).isEmpty(),"actual furnace dynamic output");
  }finally{l.setBlock(p,saved,3);}
 }
 private static <T extends AbstractCookingRecipe> ItemStack cook(ServerLevel l,RecipeType<T> type,SingleRecipeInput input){
  return l.getRecipeManager().getRecipeFor(type,input,l).orElseThrow().value().assemble(input,l.registryAccess());
 }
}
