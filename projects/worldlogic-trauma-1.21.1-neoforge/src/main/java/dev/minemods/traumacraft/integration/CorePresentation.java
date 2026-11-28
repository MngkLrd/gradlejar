package dev.minemods.traumacraft.integration;
import dev.minemods.traumacraft.api.TraumaApi;
import dev.minemods.traumacraft.content.*;
import dev.minemods.traumacraft.registry.ModItems;
import dev.minemods.worldlogic.api.WorldLogicRecipes;
import dev.minemods.worldlogic.api.WorldLogicDashboards;
import dev.minemods.worldlogic.api.WorldLogicCapabilities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import java.util.*;
import static dev.minemods.worldlogic.api.WorldLogicRecipes.Role.*;
/** Presentation only: derives catalogue variants from the same tags and item identity used by field processing. */
public final class CorePresentation {
 private CorePresentation(){}
 private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("worldlogic_trauma",path);}
 public interface Clinical {
  Optional<TraumaApi.Status> status(ServerPlayer player);
  Optional<TraumaApi.Physiology> physiology(ServerPlayer player);
  Optional<TraumaApi.IllnessStatus> illness(ServerPlayer player);
  boolean adminRestore(ServerPlayer player);
 }
 public static final WorldLogicCapabilities.Key<Clinical> CLINICAL=new WorldLogicCapabilities.Key<>(id("trauma_status"),1,Clinical.class);
 public static void register(){
  WorldLogicCapabilities.register(CLINICAL,TraumaApi.API_REVISION,"worldlogic_trauma",new Clinical(){
   public Optional<TraumaApi.Status> status(ServerPlayer p){return TraumaApi.status(p);}
   public Optional<TraumaApi.Physiology> physiology(ServerPlayer p){return TraumaApi.physiology(p);}
   public Optional<TraumaApi.IllnessStatus> illness(ServerPlayer p){return TraumaApi.illnessStatus(p);}
   public boolean adminRestore(ServerPlayer p){return TraumaApi.adminRestore(p);}
  });
  WorldLogicRecipes.registerProcessingProvider(id("herbs"),CorePresentation::recipes);
  WorldLogicDashboards.register(id("dashboard"),CorePresentation::dashboard);
 }
 private static List<ItemStack> supports(TagKey<Block> tag){return BuiltInRegistries.BLOCK.getTag(tag).stream().flatMap(s->s.stream()).map(h->new ItemStack(h.value())).filter(s->!s.isEmpty()).toList();}
 private static WorldLogicRecipes.Slot slot(WorldLogicRecipes.Role r,List<ItemStack> a){return new WorldLogicRecipes.Slot(r,a);}
 private static List<ItemStack> inputs(HerbSpecies s,int dry){
  var bundle=new ItemStack(ModItems.RAW_HERBS.get());HerbSpecies.write(bundle,s,dry);
  var list=new ArrayList<ItemStack>();list.add(bundle);
  if(s.botanical()&&dry==0)BuiltInRegistries.ITEM.getTag(s.tag()).ifPresent(tag->tag.stream().map(h->new ItemStack(h.value())).filter(x->HerbSpecies.plant(x)==s).forEach(list::add));
  return list;
 }
 public static List<WorldLogicRecipes.ProcessingRecipe> recipes(){
  var stone=supports(HerbWorkpieceBlock.SURFACES);var logs=supports(HerbWorkpieceBlock.DRYING_LOGS);
  var tools=List.of(new ItemStack(Items.FLINT),new ItemStack(Items.COBBLESTONE));
  var result=new ArrayList<WorldLogicRecipes.ProcessingRecipe>();
  if(!stone.isEmpty())for(var s:HerbSpecies.values())if(s!=HerbSpecies.UNKNOWN){
   for(int dry:s.botanical()?new int[]{0,HerbSpecies.DRY_SECONDS}:new int[]{0}){
    var powder=s.botanical()?HerbSpecies.powder(s,2,dry):new ItemStack(ModItems.CRUSHED_HERBS.get(),2);
    result.add(new WorldLogicRecipes.ProcessingRecipe(id("crush_"+s.id()+"_"+dry),"traumacraft.processing.crush",List.of(slot(INPUT,inputs(s,dry)),slot(SUPPORT,stone),slot(TOOL,tools),slot(OUTPUT,List.of(powder))),List.of("traumacraft.processing.crush1","traumacraft.processing.crush2","traumacraft.processing.crush3")));
   }
  }
  if(!logs.isEmpty())for(var s:HerbSpecies.values())if(s.botanical()){
   var dry=inputs(s,HerbSpecies.DRY_SECONDS).getFirst();
   result.add(new WorldLogicRecipes.ProcessingRecipe(id("dry_"+s.id()),"traumacraft.processing.dry",List.of(slot(INPUT,inputs(s,0)),slot(INPUT,List.of(PotionContents.createItemStack(Items.POTION,Potions.WATER))),slot(SUPPORT,logs),slot(OUTPUT,List.of(dry))),List.of("traumacraft.processing.dry1","traumacraft.processing.dry2","traumacraft.processing.dry3")));
  }
  return List.copyOf(result);
 }
 private static WorldLogicDashboards.Snapshot dashboard(ServerPlayer p){
  var a=TraumaApi.status(p);var b=TraumaApi.physiology(p);if(a.isEmpty()||b.isEmpty())return null;
  var s=a.get();var f=b.get();var rows=new ArrayList<WorldLogicDashboards.Row>();
  rows.add(new WorldLogicDashboards.Row("Blood / mL",Math.round(s.bloodMl())+"/"+Math.round(s.maximumBloodMl()),Math.max(0,Math.min(1,s.bloodMl()/s.maximumBloodMl()))));
  rows.add(new WorldLogicDashboards.Row("Oxygen",Math.round(f.oxygenFraction()*100)+"%",Math.max(0,Math.min(1,f.oxygenFraction()))));
  rows.add(new WorldLogicDashboards.Row("Perfusion",Math.round(f.perfusion()*100)+"%",Math.max(0,Math.min(1,f.perfusion()))));
  var anatomy=s.regions().stream().map(z->new WorldLogicDashboards.Row(z.zone().name(),Math.round(z.health())+"/"+Math.round(z.maximum()),Math.max(0,Math.min(1,z.health()/z.maximum())))).toList();
  return new WorldLogicDashboards.Snapshot(id("dashboard"),40,"Trauma",ResourceLocation.fromNamespaceAndPath("traumacraft","bandage"),List.of(new WorldLogicDashboards.Section("Physiology",rows),new WorldLogicDashboards.Section("Anatomy",anatomy)));
 }
 public static void verify(){
  var r=recipes();
  if(r.isEmpty()||r.stream().noneMatch(x->x.id().getPath().equals("crush_legacy_0"))||r.stream().noneMatch(x->x.id().getPath().equals("dry_yarrow")))throw new IllegalStateException("Herb catalogue missing actual operations");
  if(r.stream().map(WorldLogicRecipes.ProcessingRecipe::id).distinct().count()!=r.size())throw new IllegalStateException("Duplicate herb descriptors");
  if(WorldLogicCapabilities.find(CLINICAL,TraumaApi.API_REVISION).isEmpty())throw new IllegalStateException("Clinical capability not registered");
 }
}
