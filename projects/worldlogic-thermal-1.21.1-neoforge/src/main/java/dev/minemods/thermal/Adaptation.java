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
 public static double frostDose(double previous,double local,double insulation,double wet){
  double skin=local+Math.max(0,Math.min(1,insulation))*18*(1-.75*Math.max(0,Math.min(1,wet)));
  return Math.max(0,Math.min(180,(Double.isFinite(previous)?previous:0)+(skin<-3?Math.min(3,(-3-skin)/6): -2)));
 }
 public static double approach(double old,double target,double seconds,double tau){return old+(target-old)*-Math.expm1(-Math.max(0,seconds)/Math.max(1,tau));}
}
