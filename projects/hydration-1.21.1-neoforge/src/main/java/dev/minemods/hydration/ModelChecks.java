package dev.minemods.hydration;
import static dev.minemods.hydration.WaterBalance.*;
public final class ModelChecks {
 private static void check(boolean yes,String message){if(!yes)throw new IllegalStateException("Hydration regression: "+message);}
 public static void run(){
  var s=new State(40,0);
  check(drink(s,Quality.DRINKABLE,250,0).water()==60,"fresh water");
  check(drink(s,Quality.DESALINATE,250,.3).water()==35,"salt water loses water");
  check(drink(s,Quality.UNKNOWN,250,-1).exposure()>0,"unknown not safe");
  check(drink(s,Quality.TREAT_RECOMMENDED,250,.7).exposure()>drink(s,Quality.TREAT_RECOMMENDED,250,.2).exposure(),"risk");
  check(drink(s,Quality.NOT_WATER,250,0).equals(s),"nonwater");
  check(drink(s,Quality.DRINKABLE,Double.NaN,0).equals(s),"invalid dose");
  check(drink(new State(95,0),Quality.DRINKABLE,250,0).water()==100,"max");
  check(tick(new State(0,0),1).water()==0,"min");
  check(loss(40,20,true,true,false,false,1)>loss(20,50,false,false,false,true,0),"heat and effort");
  check(loss(40,20,false,false,false,true,0)>loss(40,90,false,false,false,true,0),"humidity");
  check(loss(20,50,true,false,false,true,0)>loss(20,50,false,false,false,true,0),"walking");
  check(halfUnits(0)==0&&halfUnits(1)==1&&halfUnits(50)==10&&halfUnits(100)==20,"HUD boundaries");
 }
 public static void main(String[] args){run();System.out.println("HYDRATION_MODEL_CHECKS_PASSED");}
}
