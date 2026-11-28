package dev.minemods.hydration;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
public final class VesselChecks {
 private static void require(boolean c,String message){if(!c)throw new IllegalStateException(message);}
 public static void run(){
  VesselItem[] items={HydrationItems.CUP.get(),HydrationItems.SKIN.get(),HydrationItems.JUG.get(),HydrationItems.CANTEEN.get()};
  int[] caps={500,1500,3000,2000};
  var b=new WaterBatch(250,WaterBalance.Quality.DESALINATE,35,.3,Set.of("SEA"),12345);
  for(int i=0;i<items.length;i++){
   var v=items[i];var s=new ItemStack(v);require(v.capacity==caps[i]&&v.read(s).ml()==0,"capacity/empty "+i);
   v.write(s,b);require(v.read(s).equals(b)&&v.read(s.copy()).equals(b),"NBT/copy "+i);
   var physics=dev.minemods.hydration.api.HydrationApi.liquidPhysics(s).orElseThrow();
   require(physics.known()&&Math.abs(physics.volumeM3()-.00025)<1e-12&&physics.materialId().equals("worldlogic:sea_water"),"vessel water physics "+i);
   require(dev.minemods.hydration.api.HydrationApi.liquidPhysics(s.copy()).orElseThrow().equals(physics),"physical snapshot copy "+i);
   var distilled=WaterTreatment.distill(b);v.write(s,distilled);
   require(dev.minemods.hydration.api.HydrationApi.liquidPhysics(s).orElseThrow().materialId().equals("worldlogic:fresh_water"),"distilled not source salt "+i);
   v.write(s,new WaterBatch(250,WaterBalance.Quality.DESALINATE,17.5,.3,Set.of("SEA","RIVER")));
   var mix=dev.minemods.hydration.api.HydrationApi.liquidPhysics(s).orElseThrow();
   require(mix.known()&&Math.abs(mix.volumeM3()-.00025)<1e-12&&Math.abs(mix.densityKgM3()-dev.minemods.worldlogic.api.PhysicalWater.solution(17.5,20).orElseThrow().densityKgM3())<1e-10,"Core arbitrary mixture "+i);
   require(dev.minemods.hydration.api.HydrationApi.liquidSolution(s,40).orElseThrow().temperatureC()==40,"caller temperature "+i);
   v.write(s,new WaterBatch(250,WaterBalance.Quality.UNKNOWN,0,1));
   require(!dev.minemods.hydration.api.HydrationApi.liquidPhysics(s).orElseThrow().known(),"unknown remains unknown "+i);
   v.write(s,WaterBatch.empty());require(v.read(s).ml()==0,"empty write "+i);
   var empty=dev.minemods.hydration.api.HydrationApi.liquidPhysics(s).orElseThrow();require(empty.volumeM3()==0&&empty.massKg()==0&&!empty.known(),"empty physical volume "+i);
  }
  require(!items[0].heatproof&&!items[1].heatproof&&items[2].heatproof&&items[3].heatproof,"heatproof boundary");
  var a=new ItemStack(items[2]);var d=new ItemStack(items[0]);
  items[2].write(a,new WaterBatch(1200,b.quality(),b.salinity(),b.risk(),b.origins()));
  var t=WaterBatch.transfer(items[2].read(a),items[0].read(d),1200,items[0].capacity);
  items[2].write(a,t.source());items[0].write(d,t.target());
  require(items[2].read(a).ml()==700&&items[0].read(d).ml()==500,"stack transfer conservation");
 }
}
