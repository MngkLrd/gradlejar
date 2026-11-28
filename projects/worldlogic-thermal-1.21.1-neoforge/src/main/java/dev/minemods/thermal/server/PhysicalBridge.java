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
import java.util.Optional;
import java.util.OptionalDouble;
import org.slf4j.LoggerFactory;

/** Public Core API only. Reflection permits the verified pre-RC53 build to remain usable
 * until an eligible RC53 dependency is available. No registry values are copied. */
final class PhysicalBridge {
 private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("WorldLogic Thermal materials");
 private static final String TYPE="dev.minemods.worldlogic.api.PhysicalMaterials";
 private static final Access API=load();
 private static boolean warned;
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
   if(a.getField("REVISION").getInt(null)!=1)throw new IllegalStateException("Unsupported PhysicalMaterials revision");
   return new Access(a.getMethod("placed",ServerLevel.class,BlockPos.class),a.getMethod("itemResult",ItemStack.class),
    a.getMethod("fluid",Fluid.class),a.getMethod("evaluate",p,s),s.getMethod("reference"),
    r.getMethod("object"),r.getMethod("state"),r.getMethod("volumeM3"),r.getMethod("heatCapacityJK"),
    p.getMethod("components"),p.getMethod("evidence"),a.getMethod("componentProperties",component,s),
    component.getMethod("volumeM3"),cr.getMethod("conductivityWmK"));
  }catch(ReflectiveOperationException|LinkageError|RuntimeException ex){LOG.error("Core advertises materials but its public API is unavailable",ex);return null;}
 }
 static boolean available(){return API!=null;}
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
  return placed(l,p).map(t->wallFactors(t,fallback)).orElse(fallback);
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
