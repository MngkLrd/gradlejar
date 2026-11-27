package dev.minemods.metabolism;

import java.util.Arrays;

/** Game-calibrated nutrient requirements per 24,000 ticks, not clinical measurements. */
public final class MetabolicModel {
 public static final String[] KEYS={"protein","fat","carb","fiber","vitamin_a","vitamin_c","vitamin_b12","iron","calcium","magnesium"};
 public static final double[] DAILY={65,70,250,25,900,90,2.4,12,900,320};
 public static final double FAST_MAX=1800,FAT_MAX=30000,STOMACH_MAX=1800;
 public static final int N=KEYS.length;
 public static final double GAME_DAY_SECONDS=1200, BASE_DIGEST_KCAL_PER_SECOND=2.5;
 private static final double[] BUFFER_DAYS={1.5,2,1.25,1.5,12,4,30,12,14,5};
 public record Meal(double kcal,double[] nutrients,double toxin,double absorption) {
  public Meal { nutrients=nutrients.clone(); if(nutrients.length!=N)throw new IllegalArgumentException("nutrients"); }
  @Override public double[] nutrients(){return nutrients.clone();}
 }
 public static final class State {
  public double fast=1350,reserve=21000,stomach=0,queuedEnergy=0,toxin=0,absorption=1,burn=90,activity=0,unknown=0,observedSeconds=0;
  public final double[] stores=new double[N],pending=new double[N],baseline=new double[N];
  public State(){Arrays.fill(stores,1);Arrays.fill(baseline,1);}
  public static final int HISTORY=7;
  public final double[] historyIntake=new double[HISTORY],historyBurn=new double[HISTORY],historyQuality=new double[HISTORY];
  public int historyCount,daySeconds;
  public double dayIntake,dayBurn,dayQuality,water=-1,salts=-1,bodyC=-1;
  public double tissueFactor(){return clamp(recovery()*(.35+.65*Math.min(1,adequacy(0)))*(.4+.6*Math.min(1,adequacy(5))),.1,1);}
  public double boneFactor(){return clamp(recovery()*(.3+.7*Math.min(1,adequacy(8))),.1,1);}
  public double bloodFactor(){return clamp(recovery()*(.3+.7*Math.min(1,Math.min(adequacy(7),adequacy(6)))),.1,1);}
  public double fractureThresholdFactor(){return .65+.35*Math.min(1,adequacy(8));}
  public double physicalFactor(){return .5+.5*Math.min(1,adequacy(7));}
  public double hydrationFactor(){return water<0?1:clamp((.55+.45*water/100)*(salts<0?1:1-.35*Math.abs(salts-50)/50),.3,1);}
  public double thermalCost(){return bodyC<0?1:1+clamp(Math.abs(bodyC-37)-.5,0,5)*.09;}
  public double trend(int i){return observedSeconds<60?0:adequacy(i)-baseline[i];}
  public int deficient(){int count=0;for(int i=0;i<N;i++)if(adequacy(i)<.6)count++;return count;}
  public int critical(){int count=0;for(int i=0;i<N;i++)if(adequacy(i)<.25)count++;return count;}
  public double fastHours(){return fast/Math.max(1,burn);}
  public double totalHours(){return (fast+.85*reserve)/Math.max(1,burn);}
  public double digestiveHours(){return stomach/Math.max(.01,digestionRate())/50;}
  public double digestionRate(){return BASE_DIGEST_KCAL_PER_SECOND*hydrationFactor()*(1-.5*toxin)*(.7+.3*Math.min(1,adequacy(3)));}
  public double adequacy(int i){return clamp(stores[i],0,1.5);}
  public double quality(){double sum=0,min=1;for(int i=0;i<N;i++){double v=Math.min(1,adequacy(i));sum+=v;min=Math.min(min,v);}return .65*sum/N+.35*min;}
  public double energy(){return clamp(.75*fast/FAST_MAX+.25*reserve/FAT_MAX,0,1);}
  public double recovery(){return clamp((.35+.65*quality())*(.45+.55*energy())*(1-.65*toxin),.1,1);}
  public double resistance(){return clamp((.5+.5*quality())*(1-.5*toxin),.1,1);}
  public double absorptionEfficiency(){return clamp(absorption*(1-.4*toxin)*hydrationFactor(),.15,1);}
 }
 public static double clamp(double x,double lo,double hi){return Double.isFinite(x)?Math.max(lo,Math.min(hi,x)):lo;}
 public static void ingest(State s,Meal m,boolean known){
  double kcal=clamp(m.kcal,0,10000),room=Math.max(0,STOMACH_MAX-s.stomach),accepted=Math.min(kcal,room),ratio=kcal>0?accepted/kcal:0;
  double old=s.stomach;s.stomach+=accepted;s.queuedEnergy+=accepted;s.dayIntake+=accepted;
  if(s.stomach>0)s.absorption=clamp((s.absorption*old+clamp(m.absorption,.15,1)*accepted)/s.stomach,.15,1);
  for(int i=0;i<N;i++)s.pending[i]=clamp(s.pending[i]+clamp(m.nutrients[i],0,DAILY[i]*4)/DAILY[i]*ratio,0,8);
  s.toxin=clamp(s.toxin+clamp(m.toxin,0,1),0,1);
  if(!known)s.unknown=Math.min(999,s.unknown+1);
 }
 /** Exactly one real second at 20 TPS = 0.02 in-game hours. Offline time does not advance. */
 public static void tick(State s,double intensity){
  s.activity=clamp(intensity,0,1);s.burn=(90+230*s.activity)*s.thermalCost();
  double digested=Math.min(s.stomach,s.digestionRate()),fraction=s.stomach>0?digested/s.stomach:0;
  double kcal=s.queuedEnergy*fraction*s.absorptionEfficiency();s.queuedEnergy*=1-fraction;s.stomach-=digested;
  for(int i=0;i<N;i++){
   double transfer=s.pending[i]*fraction;s.pending[i]-=transfer;
   s.baseline[i]+=(s.stores[i]-s.baseline[i])/GAME_DAY_SECONDS;
   s.stores[i]=clamp(s.stores[i]+transfer*s.absorptionEfficiency()/BUFFER_DAYS[i]-1.0/(GAME_DAY_SECONDS*BUFFER_DAYS[i]),0,1.5);
  }
  s.observedSeconds=Math.min(GAME_DAY_SECONDS,s.observedSeconds+1);
  if(s.stomach<=1e-8){s.stomach=0;s.queuedEnergy=0;s.absorption=1;Arrays.fill(s.pending,0);}
  double use=s.burn/50.0;s.dayBurn+=use;s.dayQuality+=s.quality();
  double fatFuel=Math.min(s.reserve*.85,use*(.55-.4*s.activity));s.reserve-=fatFuel/.85;
  double fast=s.fast+kcal-(use-fatFuel);
  if(fast>FAST_MAX){s.reserve=clamp(s.reserve+(fast-FAST_MAX)*.8,0,FAT_MAX);fast=FAST_MAX;}
  if(fast<0){double fromFat=Math.min(s.reserve,-fast/0.85);s.reserve-=fromFat;fast+=fromFat*.85;}
  s.fast=clamp(fast,0,FAST_MAX);s.toxin=clamp(s.toxin-.00035,0,1);
  if(++s.daySeconds>=1200){for(int i=State.HISTORY-1;i>0;i--){s.historyIntake[i]=s.historyIntake[i-1];s.historyBurn[i]=s.historyBurn[i-1];s.historyQuality[i]=s.historyQuality[i-1];}s.historyIntake[0]=s.dayIntake;s.historyBurn[0]=s.dayBurn;s.historyQuality[0]=s.dayQuality/s.daySeconds;s.historyCount=Math.min(State.HISTORY,s.historyCount+1);s.daySeconds=0;s.dayIntake=0;s.dayBurn=0;s.dayQuality=0;}
 }
 private MetabolicModel(){}
}
