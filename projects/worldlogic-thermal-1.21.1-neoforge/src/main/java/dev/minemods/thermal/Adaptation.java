package dev.minemods.thermal;
/** Game-time acclimation; no new visible physiological actions. */
public final class Adaptation {
 public record State(double cold,double heat){
  public State{cold=bound(cold);heat=bound(heat);}
 }
 private Adaptation(){}
 private static double bound(double x){return Double.isFinite(x)?Math.max(0,Math.min(1,x)):0;}
 public static State step(State s,double felt,double body){
  // Roughly four active game days for substantial adaptation. Severe exposure is not training.
  double c= felt<12 && felt>=-10 && body>=35.5?1:0;
  double h= felt>28 && felt<=42 && body<=38.5?1:0;
  return new State(s.cold()+(c-s.cold())/(c>0?4800:14400),s.heat()+(h-s.heat())/(h>0?4800:14400));
 }
 public static double nutrition(int food,float saturation){return Math.max(0,Math.min(1,(food+Math.min(5,Math.max(0,saturation)))/20.0));}
 /** One active second (20 ticks). This is a relative gameplay exposure budget,
  * not measured skin temperature or a clinical freezing prediction.
  * At 20 TPS, a 1200-second Minecraft day corresponds to 24 hours.
  * Clinical wind-chill risk is compressed much less aggressively than the calendar:
  * a moderate frost permits shelter construction and dangerous frost remains urgent. */
 public static final double FROST_THRESHOLD=120;
 public static final int FROST_MODEL_REVISION=2;
 private static final double[] COLD={3,5,10,15,20,28,40,55};
 private static final double[] SECONDS={Double.POSITIVE_INFINITY,1200,600,300,180,120,75,45};
 public static double frostEquivalent(double local,double insulation,double wet){
  return local+bound(insulation)*18*(1-.75*bound(wet));
 }
 public static double frostRate(double equivalent){
  if(!Double.isFinite(equivalent)||equivalent>=-3)return 0;
  double severity=-equivalent;
  if(severity>=COLD[COLD.length-1])return FROST_THRESHOLD/SECONDS[SECONDS.length-1];
  for(int i=1;i<COLD.length;i++)if(severity<=COLD[i]){
   double t=(severity-COLD[i-1])/(COLD[i]-COLD[i-1]);
   double a=FROST_THRESHOLD/SECONDS[i-1],b=FROST_THRESHOLD/SECONDS[i];
   return a+(b-a)*t;
  }
  return 0;
 }
 public static double frostDose(double previous,double local,double insulation,double wet){
  double equivalent=frostEquivalent(local,insulation,wet);
  double rate=frostRate(equivalent);
  double old=Double.isFinite(previous)?Math.max(0,Math.min(FROST_THRESHOLD,previous)):0;
  // Slow partial recovery at merely tolerable cold; real warming removes pre-injury exposure.
  double next=Math.max(0,Math.min(FROST_THRESHOLD,old+(rate>0?rate:equivalent>=0?-2:-.25)));
  return next>=FROST_THRESHOLD-1e-8?FROST_THRESHOLD:next;
 }
 public static double migrateFrostDose(double previous){
  // Legacy model accumulated too quickly and retained a half-dose after each report.
  return Math.max(0,Math.min(FROST_THRESHOLD*.25,Double.isFinite(previous)?previous*.25:0));
 }
 public static double approach(double old,double target,double seconds,double tau){return old+(target-old)*-Math.expm1(-Math.max(0,seconds)/Math.max(1,tau));}
}
