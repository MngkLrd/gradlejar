package dev.minemods.metabolism;

import java.util.Arrays;

/** Game-calibrated nutrient requirements per 24,000 ticks, not clinical measurements. */
public final class MetabolicModel {
 public static final String[] KEYS={"protein","fat","carb","fiber","vitamin_a","vitamin_c","vitamin_b12","iron","calcium","magnesium"};
 public static final double[] DAILY={65,70,250,25,900,90,2.4,12,900,320};
 public static final double FAST_MAX=1800,FAT_MAX=30000,STOMACH_MAX=1800;
 public static final int N=KEYS.length;
 public record Meal(double kcal,double[] nutrients,double toxin,double absorption) {
  public Meal { nutrients=nutrients.clone(); if(nutrients.length!=N)throw new IllegalArgumentException("nutrients"); }
  @Override public double[] nutrients(){return nutrients.clone();}
 }
 public static final class State {
  public double fast=1350,reserve=21000,stomach=0,queuedEnergy=0,toxin=0,absorption=1,burn=90,activity=0,unknown=0;
  public final double[] stores=new double[N],pending=new double[N];
  public State(){Arrays.fill(stores,1);}
  public double adequacy(int i){return clamp(stores[i],0,1.5);}
  public double quality(){double sum=0,min=1;for(int i=0;i<N;i++){double v=Math.min(1,adequacy(i));sum+=v;min=Math.min(min,v);}return .65*sum/N+.35*min;}
  public double energy(){return clamp(.75*fast/FAST_MAX+.25*reserve/FAT_MAX,0,1);}
  public double recovery(){return clamp((.35+.65*quality())*(.45+.55*energy())*(1-.65*toxin),.1,1);}
  public double resistance(){return clamp((.5+.5*quality())*(1-.5*toxin),.1,1);}
  public double absorptionEfficiency(){return clamp(absorption*(1-.4*toxin),.15,1);}
 }
 public static double clamp(double x,double lo,double hi){return Double.isFinite(x)?Math.max(lo,Math.min(hi,x)):lo;}
 public static void ingest(State s,Meal m,boolean known){
  double kcal=clamp(m.kcal,0,10000),room=Math.max(0,STOMACH_MAX-s.stomach),accepted=Math.min(kcal,room),ratio=kcal>0?accepted/kcal:0;
  double old=s.stomach;s.stomach+=accepted;s.queuedEnergy+=accepted;
  if(s.stomach>0)s.absorption=clamp((s.absorption*old+clamp(m.absorption,.15,1)*accepted)/s.stomach,.15,1);
  for(int i=0;i<N;i++)s.pending[i]=clamp(s.pending[i]+clamp(m.nutrients[i],0,DAILY[i]*4)/DAILY[i]*ratio,0,8);
  s.toxin=clamp(s.toxin+clamp(m.toxin,0,1),0,1);
  if(!known)s.unknown=Math.min(999,s.unknown+1);
 }
 /** Exactly one real second at 20 TPS = 0.02 in-game hours. Offline time does not advance. */
 public static void tick(State s,double intensity){
  s.activity=clamp(intensity,0,1);s.burn=90+230*s.activity;
  double digested=Math.min(s.stomach,10*s.absorptionEfficiency()),fraction=s.stomach>0?digested/s.stomach:0;
  double kcal=s.queuedEnergy*fraction*s.absorptionEfficiency();s.queuedEnergy*=1-fraction;s.stomach-=digested;
  for(int i=0;i<N;i++){
   double transfer=s.pending[i]*fraction;s.pending[i]-=transfer;
   s.stores[i]=clamp(s.stores[i]+transfer*s.absorptionEfficiency()*.45-1.0/3600,0,1.5);
  }
  double use=s.burn/50.0;double fast=s.fast+kcal-use;
  if(fast>FAST_MAX){s.reserve=clamp(s.reserve+(fast-FAST_MAX)*.8,0,FAT_MAX);fast=FAST_MAX;}
  if(fast<0){double fromFat=Math.min(s.reserve,-fast/0.85);s.reserve-=fromFat;fast+=fromFat*.85;}
  s.fast=clamp(fast,0,FAST_MAX);s.toxin=clamp(s.toxin-.00035,0,1);
 }
 private MetabolicModel(){}
}
