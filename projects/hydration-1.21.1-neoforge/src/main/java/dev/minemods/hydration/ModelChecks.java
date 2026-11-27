package dev.minemods.hydration;
import static dev.minemods.hydration.WaterBalance.*;
public final class ModelChecks {
 private static void check(boolean yes,String message){if(!yes)throw new IllegalStateException("Hydration regression: "+message);}
 public static void run(){
  var s=new State(40,0);
  var d=drink(s,Quality.DRINKABLE,250,0);
  check(d.water()==40&&d.pending()==20,"fresh water pending not instant");
  var a=tick(d,0);check(a.water()>40&&a.pending()<20,"absorption");
  for(int i=0;i<40;i++)a=tick(a,0);
  check(Math.abs(a.water()-60)<.001&&a.pending()<.001,"total absorption conserved");
  check(drink(s,Quality.DESALINATE,250,.3).water()==35,"salt water loses water");
  check(drink(s,Quality.UNKNOWN,250,-1).exposure()>0,"unknown not safe");
  check(drink(s,Quality.TREAT_RECOMMENDED,250,.7).exposure()>drink(s,Quality.TREAT_RECOMMENDED,250,.2).exposure(),"risk");
  check(drink(s,Quality.NOT_WATER,250,0).equals(s),"nonwater");
  check(drink(s,Quality.DRINKABLE,Double.NaN,0).equals(s),"invalid dose");
  var full=drink(new State(100,0),Quality.DRINKABLE,1000,0);
  check(full.pending()==25&&!canDrink(full),"overfill rejected");
  for(int i=0;i<60;i++)full=tick(full,0);
  check(Math.abs(full.reserve()-25)<.001&&full.water()==100,"reserve");
  check(tick(full,1).water()==100&&tick(full,1).reserve()<25,"reserve first");
  check(tick(new State(0,0),1).water()==0,"min");
  check(loss(40,20,true,true,false,false,1)>loss(20,50,false,false,false,true,0),"heat and effort");
  check(loss(40,20,false,false,false,true,0)>loss(40,90,false,false,false,true,0),"humidity");
  check(loss(20,50,true,false,false,true,0)>loss(20,50,false,false,false,true,0),"walking");
  check(loss(40,50,false,false,false,false,1,37,0)==loss(20,50,false,false,false,true,0,37,0),"thermal replaces ambient heat");
  check(loss(20,50,false,false,false,true,0,39,0)>loss(20,50,false,false,false,true,0,37,0),"body temperature");
  check(loss(20,50,false,false,false,true,0,37,4)>loss(20,50,false,false,false,true,0,37,0),"bleeding");
  check(halfUnits(0)==0&&halfUnits(1)==1&&halfUnits(50)==10&&halfUnits(100)==20,"HUD boundaries");
  var sea=new WaterBatch(1000,Quality.DESALINATE,35,.3,java.util.Set.of("SEA"));
  var fresh=new WaterBatch(500,Quality.DRINKABLE,.2,.1,java.util.Set.of("RIVER"));
  var t=WaterBatch.transfer(sea,fresh,750,1000);
  check(t.moved()==500&&t.source().ml()==500&&t.target().ml()==1000,"volume conservation/capacity");
  check(Math.abs(t.target().salinity()*1000+t.source().salinity()*500-(35*1000+.2*500))<.001,"salt mass conservation");
  check(Math.abs(t.target().risk()*1000+t.source().risk()*500-(.3*1000+.1*500))<.001,"contamination conservation");
  check(t.target().quality()==Quality.DESALINATE,"salt cannot be laundered");
  check(t.target().origins().equals(java.util.Set.of("SEA","RIVER"))&&t.source().origins().equals(java.util.Set.of("SEA")),"provenance survives mixing and split");
  check(WaterBatch.transfer(sea,t.target(),100,1000).moved()==0,"full target");
  var unknown=new WaterBatch(100,Quality.UNKNOWN,0,1);
  check(WaterBatch.transfer(unknown,fresh,100,1000).target().quality()==Quality.UNKNOWN,"unknown cannot be laundered");
 }
 public static void main(String[] args){run();System.out.println("HYDRATION_MODEL_CHECKS_PASSED");}
}
