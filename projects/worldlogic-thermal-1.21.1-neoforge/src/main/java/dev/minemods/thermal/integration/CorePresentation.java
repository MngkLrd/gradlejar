package dev.minemods.thermal.integration;
import dev.minemods.thermal.ThermalMod;
import dev.minemods.thermal.api.ThermalApi;
import dev.minemods.thermal.server.ThermalEvents;
import dev.minemods.thermal.content.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.function.*;
import java.lang.reflect.*;
/** Optional presentation-only bridge. The actual mechanics and server snapshots remain Thermal-owned. */
public final class CorePresentation {
 private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("WorldLogic Thermal");
 private static final String PKG="dev.minemods.worldlogic.api.";
 private static final ResourceLocation DASH=id("dashboard");
 private static boolean dashboards,recipes;
 private CorePresentation(){}
 private static ResourceLocation id(String key){return ResourceLocation.fromNamespaceAndPath(ThermalMod.ID,key);}
 private static String key(String s){return "worldlogic_thermal.processing."+s;}
 private static String label(String s){return "screen.worldlogic_thermal."+s;}
 public static void register(){
  try{
   Class<?> c=Class.forName(PKG+"WorldLogicDashboards");
   if(c.getField("API_VERSION").getInt(null)==1){
    c.getMethod("register",ResourceLocation.class,Function.class).invoke(null,DASH,(Function<ServerPlayer,Object>)CorePresentation::dashboard);
    dashboards=true;
   }
  }catch(ClassNotFoundException ignored){}catch(ReflectiveOperationException|LinkageError|RuntimeException ex){LOG.warn("Core dashboard integration unavailable",ex);}
  try{
   Class<?> c=Class.forName(PKG+"WorldLogicRecipes");
   if(c.getField("API_VERSION").getInt(null)==1){
    c.getMethod("registerProcessingProvider",ResourceLocation.class,Supplier.class).invoke(null,id("processing"),(Supplier<List<Object>>)CorePresentation::coreRecipes);
    recipes=true;
   }
  }catch(ClassNotFoundException ignored){}catch(ReflectiveOperationException|LinkageError|RuntimeException ex){LOG.warn("Core processing integration unavailable",ex);}
 }
 private static Object dashboard(ServerPlayer p){
  var status=ThermalApi.status(p).orElse(null);if(status==null)return null;
  try{
   var row=Class.forName(PKG+"WorldLogicDashboards$Row").getConstructor(String.class,String.class,double.class);
   var section=Class.forName(PKG+"WorldLogicDashboards$Section").getConstructor(String.class,List.class);
   var snapshot=Class.forName(PKG+"WorldLogicDashboards$Snapshot").getConstructor(ResourceLocation.class,int.class,String.class,ResourceLocation.class,List.class);
   var n=p.getPersistentData().getCompound(ThermalEvents.KEY);
   List<Object> rows=new ArrayList<>();
   rows.add(row.newInstance(label("body"),degrees(status.bodyC()),Double.NaN));
   rows.add(row.newInstance(label("air"),degrees(n.getDouble("air")),Double.NaN));
   rows.add(row.newInstance(label("felt"),degrees(n.getDouble("felt")),Double.NaN));
   rows.add(row.newInstance(label("wet"),percent(n.getDouble("wet")),Double.NaN));
   rows.add(row.newInstance(label("insulation"),percent(n.getDouble("insulation")),Double.NaN));
   rows.add(row.newInstance(label("trend"),String.format(Locale.ROOT,"%+.2f °C/min",n.getDouble("trend")),Double.NaN));
   return snapshot.newInstance(DASH,50,label("title"),ResourceLocation.fromNamespaceAndPath("minecraft","blaze_powder"),List.of(section.newInstance(label("body_tab"),rows)));
  }catch(ReflectiveOperationException ex){throw new IllegalStateException("Thermal dashboard contract",ex);}
 }
 private static String degrees(double v){return String.format(Locale.ROOT,"%.1f °C",v);}
 private static String percent(double v){return String.format(Locale.ROOT,"%.0f%%",100*v);}
 public record Slot(String role,List<ItemStack> alternatives){
  public Slot{alternatives=alternatives.stream().filter(s->!s.isEmpty()).map(ItemStack::copy).toList();}
  @Override public List<ItemStack> alternatives(){return alternatives.stream().map(ItemStack::copy).toList();}
 }
 public record Process(String name,List<Slot> slots,List<String> steps){
  public Process{slots=List.copyOf(slots);steps=List.copyOf(steps);}
 }
 private static Slot slot(String role,ItemStack... items){return new Slot(role,List.of(items));}
 private static Process process(String name,List<Slot> slots,String... steps){return new Process(name,slots,Arrays.stream(steps).map(CorePresentation::key).toList());}
 private static ItemStack stack(net.minecraft.world.level.ItemLike item){return new ItemStack(item);}
 public static List<Process> processes(){
  List<Process> out=new ArrayList<>();
  var supports=BuiltInRegistries.BLOCK.getTag(FiberWorkpieceBlock.SURFACES).map(t->t.stream().map(h->stack(h.value())).filter(s->!s.isEmpty()).toList()).orElse(List.of());
  if(!supports.isEmpty()){
   out.add(process("fiber",List.of(slot("INPUT",stack(ThermalContent.RAW_FIBER.get())),new Slot("SUPPORT",supports),slot("OUTPUT",FiberWorkpieceEntity.finishedOutput())),"fiber.step1","fiber.step2","fiber.step3"));
   out.add(process("fiber_scraper",List.of(slot("INPUT",stack(ThermalContent.RAW_FIBER.get())),new Slot("SUPPORT",supports),slot("TOOL",stack(ThermalContent.SCRAPER.get())),slot("OUTPUT",FiberWorkpieceEntity.finishedOutput())),"fiber.step1","fiber.step2","fiber.step3"));
  }
  var plants=BuiltInRegistries.BLOCK.getTag(FiberHarvest.PLANTS).map(t->t.stream().map(h->stack(h.value())).filter(s->!s.isEmpty()).distinct().toList()).orElse(List.of());
  if(!plants.isEmpty())out.add(process("harvest",List.of(new Slot("INPUT",plants),slot("OUTPUT",stack(ThermalContent.RAW_FIBER.get()))),"harvest.step"));
  var spindle=ThermalContent.SPINDLE.get();
  out.add(process("spindle",List.of(slot("INPUT",new ItemStack(ThermalContent.FIBER.get(),spindle.processingCount())),slot("TOOL",stack(spindle)),slot("OUTPUT",spindle.processingOutput())),"tool.step"));
  var comb=ThermalContent.WOOL_COMB.get();
  var wool=BuiltInRegistries.ITEM.getTag(ItemTags.WOOL).map(t->t.stream().map(h->new ItemStack(h.value(),comb.processingCount())).toList()).orElse(List.of());
  if(!wool.isEmpty())out.add(process("comb",List.of(new Slot("INPUT",wool),slot("TOOL",stack(comb)),slot("OUTPUT",comb.processingOutput())),"tool.step"));
  for(var station:List.of(ThermalContent.LOOM.get(),ThermalContent.RACK.get(),ThermalContent.HIDE_FRAME.get(),ThermalContent.FELT_BOARD.get())){
   String name=BuiltInRegistries.BLOCK.getKey(station).getPath();
   out.add(process(name,List.of(slot("INPUT",station.input()),slot("SUPPORT",stack(station)),slot("OUTPUT",station.output())),name.equals("drying_rack")?"rack.step":"station.step","station.collect"));
  }
  for(var item:ThermalContent.creativeItems())if(item instanceof ArmorItem){
   ItemStack damaged=stack(item);damaged.setDamageValue(Math.max(1,damaged.getMaxDamage()/2));
   ItemStack restored=damaged.copy();restored.setDamageValue(TextileToolItem.repairedDamage(damaged));
   out.add(process("repair_"+BuiltInRegistries.ITEM.getKey(item).getPath(),List.of(slot("INPUT",damaged),slot("INPUT",stack(ThermalContent.THREAD.get())),slot("TOOL",stack(ThermalContent.BONE_NEEDLE.get())),slot("OUTPUT",restored)),"repair.step"));
  }
  return List.copyOf(out);
 }
 @SuppressWarnings({"rawtypes","unchecked"})
 private static List<Object> coreRecipes(){
  try{
   Class<?> role=Class.forName(PKG+"WorldLogicRecipes$Role"),s=Class.forName(PKG+"WorldLogicRecipes$Slot"),r=Class.forName(PKG+"WorldLogicRecipes$ProcessingRecipe");
   var slot=s.getConstructor(role,List.class);var recipe=r.getConstructor(ResourceLocation.class,String.class,List.class,List.class);
   List<Object> result=new ArrayList<>();
   for(Process p:processes()){
    List<Object> slots=new ArrayList<>();
    for(Slot v:p.slots())slots.add(slot.newInstance(Enum.valueOf((Class)role,v.role()),v.alternatives()));
    result.add(recipe.newInstance(id("process/"+p.name()),key(p.name().startsWith("repair_")?"repair":p.name().equals("fiber_scraper")?"fiber":p.name())+".title",slots,p.steps()));
   }
   return List.copyOf(result);
  }catch(ReflectiveOperationException ex){throw new IllegalStateException("Thermal processing contract",ex);}
 }
 public static void verify(ServerPlayer p){
  var all=processes();var ids=new HashSet<String>();
  for(var process:all){
   if(!ids.add(process.name())||process.steps().isEmpty()||process.slots().stream().anyMatch(s->s.alternatives().isEmpty()))throw new IllegalStateException("invalid processing descriptor");
  }
  for(String name:List.of("fiber","harvest","spindle","comb","textile_loom","drying_rack","hide_frame","felting_board"))if(!ids.contains(name))throw new IllegalStateException("missing "+name);
  if(all.stream().filter(v->v.name().startsWith("repair_")).count()!=ThermalContent.creativeItems().stream().filter(i->i instanceof ArmorItem).count())throw new IllegalStateException("repair coverage");
  try{
   Class<?> api=Class.forName(PKG+"WorldLogicRecipes");
   if(!recipes)throw new IllegalStateException("available Core recipes not registered");
   var entries=(List<?>)api.getMethod("processing").invoke(null);
   long count=0;for(Object entry:entries)if(((ResourceLocation)entry.getClass().getMethod("id").invoke(entry)).getNamespace().equals(ThermalMod.ID))count++;
   if(count!=all.size())throw new IllegalStateException("Core recipe provider count");
  }catch(ClassNotFoundException ignored){}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
  try{
   Class<?> api=Class.forName(PKG+"WorldLogicDashboards");
   if(!dashboards||!Boolean.TRUE.equals(api.getMethod("registered",ResourceLocation.class).invoke(null,DASH)))throw new IllegalStateException("Core dashboard not registered");
   if(ThermalApi.status(p).isPresent()&&dashboard(p)==null)throw new IllegalStateException("missing authoritative dashboard");
  }catch(ClassNotFoundException ignored){}catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
 }
}
