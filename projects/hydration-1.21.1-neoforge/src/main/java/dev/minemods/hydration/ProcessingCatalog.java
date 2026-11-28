package dev.minemods.hydration;
import dev.minemods.worldlogic.api.WorldLogicRecipes;
import dev.minemods.worldlogic.api.WorldLogicRecipes.Role;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;
/** Presentation only: use actual treatment functions to build example inputs and outputs. */
public final class ProcessingCatalog {
 private ProcessingCatalog(){}
 private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("hydration",path);}
 public static void register(){WorldLogicRecipes.registerProcessingProvider(id("water_treatment"),ProcessingCatalog::recipes);}
 private static WorldLogicRecipes.Slot slot(Role r,ItemStack item){return new WorldLogicRecipes.Slot(r,List.of(item));}
 public static List<WorldLogicRecipes.ProcessingRecipe> recipes(){
  var result=new ArrayList<WorldLogicRecipes.ProcessingRecipe>();
  VesselItem[] vessels={HydrationItems.CUP.get(),HydrationItems.SKIN.get(),HydrationItems.JUG.get(),HydrationItems.CANTEEN.get()};
  String[] ids={"cup","skin","jug","canteen"};
  for(int i=0;i<vessels.length;i++){
   var v=vessels[i];var input=new WaterBatch(Math.min(v.capacity,1000),WaterBalance.Quality.TREAT_RECOMMENDED,.2,.9,Set.of("WETLAND"));
   add(result,ids[i]+"_filter","filter",v,input,WaterTreatment.filter(input),HydrationItems.FILTER.get().getDefaultInstance(),false);
   if(v.heatproof){
    add(result,ids[i]+"_boil","boil",v,input,WaterTreatment.boil(input),null,true);
    var salt=new WaterBatch(input.mB(),WaterBalance.Quality.DESALINATE,35,.3,Set.of("SEA"));
    add(result,ids[i]+"_distill","distill",v,salt,WaterTreatment.distill(salt),HydrationItems.CONDENSER.get().getDefaultInstance(),true);
   }
  }
  return List.copyOf(result);
 }
 private static void add(List<WorldLogicRecipes.ProcessingRecipe> out,String path,String type,VesselItem v,WaterBatch in,WaterBatch result,ItemStack tool,boolean fire){
  var input=new ItemStack(v);v.write(input,in);var output=new ItemStack(v);v.write(output,result);
  var slots=new ArrayList<WorldLogicRecipes.Slot>();slots.add(slot(Role.INPUT,input));
  if(fire)slots.add(slot(Role.SUPPORT,new ItemStack(Items.CAMPFIRE)));
  if(tool!=null)slots.add(slot(Role.TOOL,tool));
  slots.add(slot(Role.OUTPUT,output));
  out.add(new WorldLogicRecipes.ProcessingRecipe(id(path),"hydration.processing."+type,List.copyOf(slots),List.of("hydration.processing."+type+".step")));
 }
 public static void verify(){
  var list=WorldLogicRecipes.processing().stream().filter(x->x.id().getNamespace().equals("hydration")).toList();
  if(list.size()!=8||list.stream().map(x->x.id()).distinct().count()!=8)throw new IllegalStateException("Hydration processing registration");
  for(var r:list){var in=r.slots().stream().filter(x->x.role()==Role.INPUT).findFirst().orElseThrow().alternatives().getFirst();var out=r.slots().stream().filter(x->x.role()==Role.OUTPUT).findFirst().orElseThrow().alternatives().getFirst();var v=(VesselItem)in.getItem();var b=v.read(in);var actual=v.read(out);
   var expect=r.id().getPath().endsWith("_filter")?WaterTreatment.filter(b):r.id().getPath().endsWith("_boil")?WaterTreatment.boil(b):WaterTreatment.distill(b);
   if(!actual.equals(expect))throw new IllegalStateException("Processing output differs from actual treatment "+r.id());
  }
 }
}
