package dev.minemods.thermal;
/** Local heat exchange only, never changes core air or weather. */
public final class ClimateComfort {
 private ClimateComfort(){}
 public static double heat(double output,double distance){return output/(1+.24*distance*distance);}
 public static double cooling(double air,double humidity,double distance,boolean sheltered,boolean wet){
  if(!wet||air<=24)return 0;
  double dry=Math.max(0,Math.min(1,1-humidity/100));
  return Math.min(air-24,16*dry*(sheltered?1:.4)/(1+.18*distance*distance));
 }
 public static double capRadiant(double air,double heat){return Math.max(0,Math.min(55,Math.min(heat,Math.max(8,29-air))));}
}
