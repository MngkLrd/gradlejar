package dev.minemods.hydration;

/** Pure gameplay model, not clinical or real-world fluid advice. Units: hydration points, real game seconds. */
public final class WaterBalance {
 public enum Quality { DRINKABLE, TREAT_RECOMMENDED, DESALINATE, UNKNOWN, NOT_WATER }
 public record State(double water,double exposure) {
  public State { water=clamp(water,0,100); exposure=clamp(exposure,0,100); }
 }
 private WaterBalance(){}
 public static double clamp(double x,double min,double max){return Double.isFinite(x)?Math.max(min,Math.min(max,x)):min;}
 public static double loss(double airC,double relativeHumidity,boolean moving,boolean sprinting,boolean swimming,boolean sheltered,double solar){
  double t=Double.isFinite(airC)?airC:20;
  double humidity=Double.isFinite(relativeHumidity)?clamp(relativeHumidity,0,100):50;
  double heat=clamp((t-24)/20,0,1.8);
  double activity=swimming?.13:sprinting?.16:moving?.045:0;
  double sun=sheltered?0:clamp(solar,0,1)*.025;
  return .045+activity+heat*(.055+.045*(1-humidity/100))+sun;
 }
 public static State tick(State s,double cost){
  return new State(s.water-clamp(cost,0,2),s.exposure-.035);
 }
 public static State drink(State s,Quality quality,double ml,double risk){
  if(quality==null||quality==Quality.NOT_WATER||!Double.isFinite(ml)||ml<=0)return s;
  double dose=Math.min(1000,ml)/250.0;
  return switch(quality){
   case DRINKABLE -> new State(s.water+20*dose,s.exposure);
   case TREAT_RECOMMENDED -> new State(s.water+17*dose,s.exposure+Math.max(3,14*clamp(risk,0,1))*dose);
   case DESALINATE -> new State(s.water-5*dose,s.exposure+8*dose);
   case UNKNOWN -> new State(s.water+12*dose,s.exposure+8*dose);
   case NOT_WATER -> s;
  };
 }
 public static int halfUnits(double water){return (int)Math.ceil(clamp(water,0,100)/5);}
}
