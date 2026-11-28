package dev.minemods.worldlogic.api;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
/** Read-only presentation contract for non-RecipeManager processing. Does not execute crafting. */
public final class WorldLogicRecipes {
 private WorldLogicRecipes(){}
 public static final int API_VERSION=1,API_REVISION=0;
 @FunctionalInterface public interface Catalog{List<ProcessingRecipe> recipes();}
 public static final WorldLogicCapabilities.Key<Catalog> CAPABILITY=new WorldLogicCapabilities.Key<>(ResourceLocation.fromNamespaceAndPath("worldlogic","field_processing"),API_VERSION,Catalog.class);
 public enum Role { INPUT,SUPPORT,TOOL,OUTPUT }
 public record Slot(Role role,List<ItemStack> alternatives){
  public Slot{Objects.requireNonNull(role);alternatives=alternatives.stream().filter(s->!s.isEmpty()).map(ItemStack::copy).toList();if(alternatives.isEmpty()||alternatives.size()>4096)throw new IllegalArgumentException("alternatives");}
  @Override public List<ItemStack> alternatives(){return alternatives.stream().map(ItemStack::copy).toList();}
 }
 public record ProcessingRecipe(ResourceLocation id,String titleKey,List<Slot> slots,List<String> stepKeys){
  public ProcessingRecipe{
   Objects.requireNonNull(id);Objects.requireNonNull(titleKey);slots=List.copyOf(slots);stepKeys=List.copyOf(stepKeys);
   if(titleKey.isBlank()||slots.size()>12||stepKeys.isEmpty()||stepKeys.size()>12||stepKeys.stream().anyMatch(String::isBlank)
    ||slots.stream().noneMatch(s->s.role()==Role.INPUT)||slots.stream().noneMatch(s->s.role()==Role.OUTPUT))throw new IllegalArgumentException("processing recipe");
  }
 }
 private static final Map<ResourceLocation,Supplier<List<ProcessingRecipe>>> PROVIDERS=new ConcurrentHashMap<>();
 /** Common setup. Supplier is called client-side after registry/tag sync; no gameplay mutation. */
 public static void registerProcessingProvider(ResourceLocation provider,Supplier<List<ProcessingRecipe>> recipes){
  if(PROVIDERS.putIfAbsent(Objects.requireNonNull(provider),Objects.requireNonNull(recipes))!=null)throw new IllegalArgumentException("duplicate recipe provider "+provider);
 }
 public static List<ProcessingRecipe> processing(){
  Map<ResourceLocation,ProcessingRecipe> result=new TreeMap<>();
  PROVIDERS.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
   try{
    var local=new TreeMap<ResourceLocation,ProcessingRecipe>();
    for(var r:List.copyOf(entry.getValue().get())){
     if(!r.id().getNamespace().equals(entry.getKey().getNamespace())||result.containsKey(r.id())||local.putIfAbsent(r.id(),r)!=null)throw new IllegalArgumentException("duplicate/foreign processing recipe "+r.id());
    }
    result.putAll(local);
   }catch(RuntimeException|LinkageError ex){org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("Processing provider {} unavailable",entry.getKey(),ex);}
  });
  return List.copyOf(result.values());
 }
 public static void verify(){
  var original=new ItemStack(net.minecraft.world.item.Items.STICK,2);
  var slot=new Slot(Role.INPUT,List.of(original));original.setCount(1);slot.alternatives().getFirst().setCount(9);
  if(slot.alternatives().getFirst().getCount()!=2)throw new IllegalStateException("recipe input copy");
  var recipe=new ProcessingRecipe(ResourceLocation.fromNamespaceAndPath("worldlogic","qa_fixture"),"worldlogic.processing.fixture",List.of(slot,new Slot(Role.OUTPUT,List.of(new ItemStack(net.minecraft.world.item.Items.BOWL)))),List.of("worldlogic.processing.fixture"));
  boolean reject=false;try{new ProcessingRecipe(recipe.id(),recipe.titleKey(),List.of(slot),recipe.stepKeys());}catch(IllegalArgumentException ex){reject=true;}
  if(!reject)throw new IllegalStateException("missing output accepted");
 }
}
