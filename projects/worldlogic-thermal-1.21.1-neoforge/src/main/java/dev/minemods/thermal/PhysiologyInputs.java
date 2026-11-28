package dev.minemods.thermal;
/** Read-only inputs from the owners of metabolism, water balance and medical state.
 * Neutral values for absent optional providers; factors are gameplay estimates, not clinical measures. */
public record PhysiologyInputs(boolean metabolism,boolean hydration,boolean trauma,
 double energy,double dietQuality,double waterPercent,double electrolyteIndex,double bloodFraction,double perfusion) {
 public PhysiologyInputs(boolean metabolism,boolean hydration,boolean trauma,double energy,double dietQuality,double waterPercent,double electrolyteIndex,double bloodFraction){this(metabolism,hydration,trauma,energy,dietQuality,waterPercent,electrolyteIndex,bloodFraction,bloodFraction);}
 public PhysiologyInputs {
  energy=unit(energy,1);dietQuality=unit(dietQuality,1);
  waterPercent=100*unit(waterPercent/100,1);electrolyteIndex=100*unit(electrolyteIndex/100,.5);
  bloodFraction=unit(bloodFraction,1);perfusion=unit(perfusion,bloodFraction);
 }
 private static double unit(double v,double fallback){return Double.isFinite(v)?Math.max(0,Math.min(1,v)):fallback;}
 public static PhysiologyInputs absent(){return new PhysiologyInputs(false,false,false,1,1,100,50,1);}
 public double nutrition(double vanilla){
  return metabolism?unit(.8*energy+.2*dietQuality,1):unit(vanilla,1);
 }
 public double coolingCapacity(){
  if(!hydration)return 1;
  double saltStress=Math.abs(electrolyteIndex-50)/50;
  return unit(waterPercent/100*(1-.35*saltStress),1);
 }
 public double circulation(){return trauma?perfusion:1;}
}
