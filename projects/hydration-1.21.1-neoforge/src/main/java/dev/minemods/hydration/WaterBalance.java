package dev.minemods.hydration;

/** Game-calibrated water points. No medical units or real-world claims. */
public final class WaterBalance {
 public enum Quality { DRINKABLE, TREAT_RECOMMENDED, DESALINATE, UNKNOWN, NOT_WATER }
 public static final double MAX_RESERVE=25,MAX_PENDING=40;
 public record State(double water,double exposure,double reserve,double pending) {
  public State {water=clamp(water,0,100);exposure=clamp(exposure,0,100);reserve=clamp(reserve,0,MAX_RESERVE);pending=clamp(pending,0,MAX_PENDING);}
  public State(double water,double exposure){this(water,exposure,0,0);}
 }
 private WaterBalance(){}
 public static double clamp(double x,double min,double max){return Double.isFinite(x)?Math.max(min,Math.min(max,x)):min;}
 public static double loss(double airC,double relativeHumidity,boolean moving,boolean sprinting,boolean swimming,boolean sheltered,double solar){
  return loss(airC,relativeHumidity,moving,sprinting,swimming,sheltered,solar,Double.NaN,0);
 }
 /** When body temperature is available, replace (do not add to) the environmental heat proxy. */
 public static double loss(double airC,double relativeHumidity,boolean moving,boolean sprinting,boolean swimming,boolean sheltered,double solar,double bodyC,int bleeding){
  double t=Double.isFinite(airC)?airC:20;
  double humidity=Double.isFinite(relativeHumidity)?clamp(relativeHumidity,0,100):50;
  boolean thermal=Double.isFinite(bodyC)&&bodyC>=20&&bodyC<=50;
  double heat=thermal?clamp((bodyC-37)/2,0,1.8):clamp((t-24)/20,0,1.8);
  double activity=swimming?.13:sprinting?.16:moving?.045:0;
  double sun=sheltered||thermal?0:clamp(solar,0,1)*.025;
  return .045+activity+heat*(.055+.045*(1-humidity/100))+sun+.012*clamp(bleeding,0,14);
 }
 public static State tick(State s,double cost){
  double c=clamp(cost,0,2);
  double reserve=Math.max(0,s.reserve-c);
  double water=Math.max(0,s.water-Math.max(0,c-s.reserve));
  double absorbed=Math.min(.65,Math.min(s.pending,100-water+MAX_RESERVE-reserve));
  double main=Math.min(absorbed,100-water);
  return new State(water+main,s.exposure-.035,reserve+absorbed-main,s.pending-absorbed);
 }
 public static boolean canDrink(State s){return s.pending<MAX_PENDING-.01 && s.water+s.reserve+s.pending<100+MAX_RESERVE-.01;}
 public static State drink(State s,Quality quality,double ml,double risk){
  if(quality==null||quality==Quality.NOT_WATER||!Double.isFinite(ml)||ml<=0)return s;
  double dose=Math.min(1000,ml)/250.0;
  if(quality==Quality.DESALINATE){
   double cost=5*dose;return new State(s.water-Math.max(0,cost-s.reserve),s.exposure+8*dose,s.reserve-cost,s.pending);
  }
  double gain=switch(quality){case DRINKABLE->20;case TREAT_RECOMMENDED->17;case UNKNOWN->12;default->0;};
  double contamination=switch(quality){case TREAT_RECOMMENDED->Math.max(3,14*clamp(risk,0,1));case UNKNOWN->8;default->0;};
  double accepted=Math.min(gain*dose,Math.min(MAX_PENDING-s.pending,Math.max(0,100+MAX_RESERVE-s.water-s.reserve-s.pending)));
  return new State(s.water,s.exposure+contamination*dose,s.reserve,s.pending+accepted);
 }
 public static int halfUnits(double water){return (int)Math.ceil(clamp(water,0,100)/5);}
}
