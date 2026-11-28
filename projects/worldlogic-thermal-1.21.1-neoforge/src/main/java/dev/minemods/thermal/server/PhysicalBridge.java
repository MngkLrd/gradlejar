package dev.minemods.thermal.server;
import dev.minemods.thermal.ClimateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Optional;
import java.util.OptionalDouble;
import org.slf4j.LoggerFactory;

/** Public Core API only. Reflection permits the verified pre-RC53 build to remain usable
 * until an eligible RC53 dependency is available. No registry values are copied. */
public final class PhysicalBridge {
 private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("WorldLogic Thermal materials");
 private static final String TYPE="dev.minemods.worldlogic.api.PhysicalMaterials";
 private static final Access API=load();
 private static boolean warned;
 private static final Layers LAYERS=layers();
 private static final Water WATER=water();
 private record Layers(java.lang.reflect.Constructor<?> state,java.lang.reflect.Constructor<?> layer,Method resistance,Method r,Method u,Method materialId){}
 private record Water(Method freeze,Method solution,Method density,Method specificHeat){}
 private record Access(Method placed,Method itemResult,Method fluid,Method evaluate,Method reference,
                       Method object,Method state,Method volume,Method heat,Method components,Method evidence,
                       Method constituent,Method componentVolume,Method conductivity){}
 record ThermalProperties(double volumeM3,double heatCapacityJK,double conductivityWmK){}
 record Wall(double massFactor,double conductance){}
 private PhysicalBridge(){}
 static boolean advertised(){
  try{return Class.forName("dev.minemods.worldlogic.api.WorldLogicApi").getField("API_REVISION").getInt(null)>=14;}
  catch(ReflectiveOperationException|LinkageError ex){return false;}
 }
 private static Access load(){
  if(!advertised())return null;
  try{
   Class<?> a=Class.forName(TYPE),p=Class.forName(TYPE+"$ObjectProfile"),s=Class.forName(TYPE+"$State"),
    r=Class.forName(TYPE+"$Result"),component=Class.forName(TYPE+"$Component"),cr=Class.forName(TYPE+"$ComponentResult");
   if(a.getField("REVISION").getInt(null)<1)throw new IllegalStateException("Unsupported PhysicalMaterials revision");
   return new Access(a.getMethod("placed",ServerLevel.class,BlockPos.class),a.getMethod("itemResult",ItemStack.class),
    a.getMethod("fluid",Fluid.class),a.getMethod("evaluate",p,s),s.getMethod("reference"),
    r.getMethod("object"),r.getMethod("state"),r.getMethod("volumeM3"),r.getMethod("heatCapacityJK"),
    p.getMethod("components"),p.getMethod("evidence"),a.getMethod("componentProperties",component,s),
    component.getMethod("volumeM3"),cr.getMethod("conductivityWmK"));
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){LOG.error("Core advertises materials but its public API is unavailable",ex);return null;}
 }
 static boolean available(){return API!=null;}
 static boolean revision2(){return LAYERS!=null&&WATER!=null;}
 private static Layers layers(){
  if(API==null)return null;
  try{
   Class<?> a=Class.forName(TYPE),state=Class.forName(TYPE+"$State"),layer=Class.forName(TYPE+"$ThermalLayer"),r=Class.forName(TYPE+"$ThermalResistance"),component=Class.forName(TYPE+"$Component");
   if(a.getField("REVISION").getInt(null)<2)return null;
   return new Layers(state.getConstructor(double.class,double.class),layer.getConstructor(ResourceLocation.class,double.class,state),a.getMethod("thermalResistance",List.class),r.getMethod("areaResistanceM2KW"),r.getMethod("conductanceWm2K"),component.getMethod("materialId"));
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn(ex);return null;}
 }
 private static Water water(){
  if(API==null)return null;
  try{
   Class<?> w=Class.forName("dev.minemods.worldlogic.api.PhysicalWater"),s=Class.forName(w.getName()+"$Solution");
   return new Water(w.getMethod("freezingPointC",Fluid.class),w.getMethod("solution",Fluid.class,double.class),s.getMethod("densityKgM3"),s.getMethod("specificHeatJkgK"));
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){return null;}
 }
 record Layer(String material,double thicknessM){}
 // Geometry/design assumptions of the actual garment, not material constants.
 private static List<Layer> garmentLayers(ItemStack stack){
  if(!(stack.getItem() instanceof dev.minemods.thermal.content.ClimateGarment))return List.of();
  String n=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
  return switch(n){
   case "fur_hat","fur_boots" -> List.of(new Layer("fur",.018),new Layer("leather",.002));
   case "fur_coat" -> List.of(new Layer("fur",.025),new Layer("leather",.002));
   case "grass_hood","grass_coat","grass_leggings","grass_boots" -> List.of(new Layer("straw",.020));
   case "wool_underwear" -> List.of(new Layer("wool",.012));
   case "waxed_cloak" -> List.of(new Layer("flax",.007),new Layer("wax",.001));
   case "leather_face_mask" -> List.of(new Layer("leather",.004));
   case "linen_clothes","light_tunic","desert_headwear" -> List.of(new Layer("flax",.002));
   case "sun_hat","straw_cape" -> List.of(new Layer("straw",.005));
   default -> List.of();
  };
 }
 private static OptionalDouble resistance(List<Layer> specs,double wet){
  if(LAYERS==null||specs.isEmpty())return OptionalDouble.empty();
  try{
   Object state=LAYERS.state().newInstance(20d,wet);
   List<Object> parts=new ArrayList<>();
   for(Layer p:specs)parts.add(LAYERS.layer().newInstance(ResourceLocation.fromNamespaceAndPath("worldlogic",p.material()),p.thicknessM(),state));
   Object result=((Optional<?>)LAYERS.resistance().invoke(null,parts)).orElse(null);
   return result==null?OptionalDouble.empty():OptionalDouble.of(number(LAYERS.r(),result));
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn(ex);return OptionalDouble.empty();}
 }
 static OptionalDouble garmentResistance(ItemStack stack,double moistureKgKg){return resistance(garmentLayers(stack),moistureKgKg);}
 public static double garmentInsulation(ItemStack stack,double fallback){
  var r=garmentResistance(stack,Double.NaN);
  if(r.isEmpty()||!Double.isFinite(r.getAsDouble()))return fallback;
  // Convert physical R to existing dimensionless gameplay scale; body wetness remains Thermal-owned.
  return ClimateModel.clamp(r.getAsDouble()*.72,0,.50);
 }
 static OptionalDouble freezing(Fluid fluid){
  if(WATER==null)return OptionalDouble.empty();
  try{return (OptionalDouble)WATER.freeze().invoke(null,fluid);}
  catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn(ex);return OptionalDouble.empty();}
 }
 private static OptionalDouble volumetricHeat(Fluid fluid,double temp){
  if(WATER==null)return OptionalDouble.empty();
  try{
   Object r=((Optional<?>)WATER.solution().invoke(null,fluid,temp)).orElse(null);
   if(r==null)return OptionalDouble.empty();
   double h=number(WATER.density(),r)*number(WATER.specificHeat(),r);
   return Double.isFinite(h)&&h>0?OptionalDouble.of(h):OptionalDouble.empty();
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn(ex);return OptionalDouble.empty();}
 }
 static double waterInertia(Fluid fluid,double temp){
  var a=volumetricHeat(fluid,temp);var b=volumetricHeat(Fluids.WATER,temp);
  if(a.isPresent()&&b.isPresent())return ClimateModel.clamp(a.getAsDouble()/b.getAsDouble(),.5,2);
  return waterInertia(fluid);
 }
 private static OptionalDouble singleWallU(ServerLevel l,BlockPos pos){
  if(LAYERS==null)return OptionalDouble.empty();
  try{
   Object result=((Optional<?>)API.placed().invoke(null,l,pos)).orElse(null);
   if(result==null)return OptionalDouble.empty();
   Object p=API.object().invoke(result),state=API.state().invoke(result);
   List<?> components=(List<?>)API.components().invoke(p);
   // Composition alone does not establish ordered layers. Only a single constituent is safe here.
   if(components.size()!=1)return OptionalDouble.empty();
   Object part=components.get(0);
   Object layer=LAYERS.layer().newInstance(LAYERS.materialId().invoke(part),Math.max(.05,number(API.componentVolume(),part)),state);
   Object r=((Optional<?>)LAYERS.resistance().invoke(null,List.of(layer))).orElse(null);
   return r==null?OptionalDouble.empty():OptionalDouble.of(number(LAYERS.u(),r));
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn(ex);return OptionalDouble.empty();}
 }

 private static void warn(Throwable ex){if(!warned){LOG.warn("Core material reading failed; using explicit gameplay fallback",ex);warned=true;}}
 private static double number(Method m,Object o)throws ReflectiveOperationException{return ((Number)m.invoke(o)).doubleValue();}
 private static Optional<ThermalProperties> read(Object result)throws ReflectiveOperationException{
  if(result==null)return Optional.empty();
  Object object=API.object().invoke(result),state=API.state().invoke(result);
  if(API.evidence().invoke(object).toString().equals("UNKNOWN"))return Optional.empty();
  double volume=number(API.volume(),result),heat=number(API.heat(),result);
  if(!Double.isFinite(volume)||volume<=0)return Optional.empty();
  double weighted=0,total=0;
  for(Object c:(List<?>)API.components().invoke(object)){
   Object part=((Optional<?>)API.constituent().invoke(null,c,state)).orElse(null);
   if(part==null)return Optional.empty();
   double v=number(API.componentVolume(),c),k=number(API.conductivity(),part);
   if(!Double.isFinite(v)||v<=0||!Double.isFinite(k)||k<=0){weighted=Double.NaN;break;}
   weighted+=v*k;total+=v;
  }
  double k=total>0&&Double.isFinite(weighted)?weighted/total:Double.NaN;
  return Optional.of(new ThermalProperties(volume,heat,k));
 }
 static Optional<ThermalProperties> placed(ServerLevel l,BlockPos p){
  if(API==null)return Optional.empty();
  try{return read(((Optional<?>)API.placed().invoke(null,l,p)).orElse(null));}
  catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn(ex);return Optional.empty();}
 }
 static Optional<ThermalProperties> item(ItemStack stack){
  if(API==null||stack.isEmpty())return Optional.empty();
  try{return read(API.itemResult().invoke(null,stack));}
  catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn(ex);return Optional.empty();}
 }
 private static Optional<ThermalProperties> fluid(Fluid fluid){
  if(API==null)return Optional.empty();
  try{return read(API.evaluate().invoke(null,API.fluid().invoke(null,fluid),API.reference().invoke(null)));}
  catch(ReflectiveOperationException|LinkageError|RuntimeException ex){warn(ex);return Optional.empty();}
 }
 static Wall wallFactors(ThermalProperties t,Wall fallback){
  // Normalize actual J/K and nominal occupied m3 for the bounded room solver.
  // Effective thickness = volume / nominal 1 m2 face, not collision/assembly geometry.
  double mass=Double.isFinite(t.heatCapacityJK())&&t.heatCapacityJK()>0
      ?ClimateModel.clamp(t.heatCapacityJK()/1_000_000,.3,4):fallback.massFactor();
  double conduct=Double.isFinite(t.conductivityWmK())&&t.conductivityWmK()>0
      ?ClimateModel.clamp(.85*Math.sqrt(t.conductivityWmK()/Math.max(.05,t.volumeM3())/2.5),.2,3):fallback.conductance();
  return new Wall(mass,conduct);
 }
 static Wall wall(ServerLevel l,BlockPos p,Wall fallback){
  Wall w=placed(l,p).map(t->wallFactors(t,fallback)).orElse(fallback);
  var u=singleWallU(l,p);
  return u.isPresent()&&Double.isFinite(u.getAsDouble())&&u.getAsDouble()>0?new Wall(w.massFactor(),ClimateModel.clamp(.85*Math.sqrt(u.getAsDouble()/2.5),.2,3)):w;
 }
 static OptionalDouble insulation(ItemStack stack){
  var t=item(stack).orElse(null);
  if(t==null||!Double.isFinite(t.conductivityWmK())||t.conductivityWmK()<=0)return OptionalDouble.empty();
  // Gameplay calibration only; Core does not provide textile thickness or coverage.
  return OptionalDouble.of(ClimateModel.clamp(.24*Math.sqrt(.15/t.conductivityWmK()),.045,.30));
 }
 static double waterInertia(Fluid fluid){
  var a=fluid(fluid).orElse(null);var b=fluid(Fluids.WATER).orElse(null);
  if(a==null||b==null||!Double.isFinite(a.heatCapacityJK())||!Double.isFinite(b.heatCapacityJK())||
      a.heatCapacityJK()<=0||b.heatCapacityJK()<=0)return 1;
  double ratio=(a.heatCapacityJK()/a.volumeM3())/(b.heatCapacityJK()/b.volumeM3());
  return Double.isFinite(ratio)?ClimateModel.clamp(ratio,.5,2):1;
 }
}
