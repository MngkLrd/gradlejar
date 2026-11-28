package dev.minemods.hydration;
/** Game-calibrated water points. No medical units or real-world claims. */
public final class WaterBalance {
 public enum Quality { DRINKABLE, TREAT_RECOMMENDED, DESALINATE, UNKNOWN, NOT_WATER }
 public enum Dehydration { NORMAL, MILD, MODERATE, SEVERE, CRITICAL }
 public static final double MAX_RESERVE=25,MAX_PENDING=40;
 public record State(double water,double exposure,double reserve,double pending,double electrolytes) {
  public State {water=clamp(water,0,100);exposure=clamp(exposure,0,100);reserve=clamp(reserve,0,MAX_RESERVE);pending=clamp(pending,0,MAX_PENDING);electrolytes=Double.isFinite(electrolytes)?clamp(electrolytes,0,100):50;}
  public State(double water,double exposure,double reserve,double pending){this(water,exposure,reserve,pending,50);}
  public State(double water,double exposure){this(water,exposure,0,0,50);}
 }
 private WaterBalance(){}
 public static double clamp(double x,double min,double max){return Double.isFinite(x)?Math.max(min,Math.min(max,x)):min;}
 public static Dehydration stage(double water){return water>=70?Dehydration.NORMAL:water>=50?Dehydration.MILD:water>=25?Dehydration.MODERATE:water>=10?Dehydration.SEVERE:Dehydration.CRITICAL;}
 public static double electrolyteStress(double electrolytes){return clamp(Math.abs(electrolytes-50)/50,0,1);}
 public static double illnessLoss(double exposure){return clamp((exposure-12)/88,0,1)*.65;}
 public static double loss(double airC,double relativeHumidity,boolean moving,boolean sprinting,boolean swimming,boolean sheltered,double solar){
  return loss(airC,relativeHumidity,moving,sprinting,swimming,sheltered,solar,Double.NaN,0);
 }
 public static double loss(double airC,double relativeHumidity,boolean moving,boolean sprinting,boolean swimming,boolean sheltered,double solar,double bodyC,int bleeding){
  double t=Double.isFinite(airC)?airC:20;
  double humidity=Double.isFinite(relativeHumidity)?clamp(relativeHumidity,0,100):50;
  boolean thermal=Double.isFinite(bodyC)&&bodyC>=20&&bodyC<=50;
  double heat=thermal?clamp((bodyC-37)/2,0,1.8):clamp((t-24)/20,0,1.8);
  double activity=swimming?.13:sprinting?.16:moving?.045:0;
  double sun=sheltered||thermal?0:clamp(solar,0,1)*.025;
  return .045+activity+heat*(.055+.045*(1-humidity/100))+sun+.012*clamp(bleeding,0,14);
 }
 public static State restored(){return new State(100,0,0,0,50);}
 private static double renal(double f){return Double.isFinite(f)&&f>=0&&f<=1?f:1;}
 /** Replace only the existing .045 basal fluid term; never scale sweat, bleeding or illness. */
 public static double regulateBasalLoss(State s,double ordinaryLoss,double function){
  double need=clamp((50-s.water)/50,-1,1);
  return ordinaryLoss+.045*.25*(1-renal(function))*need;
 }
 public static State tick(State s,double cost){return tick(s,cost,1);}
 public static State tick(State s,double cost,double renalFunction){
  double c=clamp(cost,0,2);
  double reserve=Math.max(0,s.reserve-c);
  double water=Math.max(0,s.water-Math.max(0,c-s.reserve));
  double absorbed=Math.min(.65*(1-.35*electrolyteStress(s.electrolytes))*(1-.35*clamp(s.exposure/100,0,1)),Math.min(s.pending,100-water+MAX_RESERVE-reserve));
  double main=Math.min(absorbed,100-water);
  return new State(water+main,s.exposure-.035,reserve+absorbed-main,s.pending-absorbed,s.electrolytes-.10*c*(s.electrolytes>50?renal(renalFunction):1+.5*(1-renal(renalFunction))));
 }
 public static boolean canDrink(State s){return s.pending<MAX_PENDING-.01 && s.water+s.reserve+s.pending<100+MAX_RESERVE-.01;}
 public static State drink(State s,Quality quality,double ml,double risk){
  return drink(s,quality,ml,risk,quality==Quality.DESALINATE?35:0);
 }
 public static State drink(State s,Quality quality,double ml,double risk,double salinity){
  if(quality==null||quality==Quality.NOT_WATER||!Double.isFinite(ml)||ml<=0)return s;
  double dose=Math.min(1000,ml)/250.0;
  double salt=Double.isFinite(salinity)&&salinity>=0?clamp(salinity,0,1000):0;
  double dirt=clamp(risk,0,1);
  if(!Double.isFinite(risk)||risk<0||quality==Quality.UNKNOWN)dirt=Math.max(.65,dirt);
  if(quality==Quality.TREAT_RECOMMENDED)dirt=Math.max(.12,dirt);
  double exposure=Math.max(0,dirt-.10)*17*dose;
  double electrolytes=s.electrolytes+clamp(salt*.18,0,8)*dose;
  if(salt>=10){
   double cost=5*dose*clamp(salt/35,.3,3);
   return new State(s.water-Math.max(0,cost-s.reserve),s.exposure+exposure,s.reserve-cost,s.pending,electrolytes);
  }
  double gain=20*(1-.55*dirt)*clamp(1-salt*.07,.30,1);
  double accepted=Math.min(gain*dose,Math.min(MAX_PENDING-s.pending,Math.max(0,100+MAX_RESERVE-s.water-s.reserve-s.pending)));
  return new State(s.water,s.exposure+exposure,s.reserve,s.pending+accepted,electrolytes);
 }
 public static State eat(State s,int nutrition){
  double correction=Math.min(Math.abs(50-s.electrolytes),Math.max(0,nutrition)*.5);
  return new State(s.water,s.exposure,s.reserve,s.pending,s.electrolytes+Math.copySign(correction,50-s.electrolytes));
 }
 public static int halfUnits(double water){return (int)Math.ceil(clamp(water,0,100)/5);}
}
