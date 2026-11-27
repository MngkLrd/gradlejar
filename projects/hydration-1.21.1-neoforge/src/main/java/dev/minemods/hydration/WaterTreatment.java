package dev.minemods.hydration;
/** Abstract gameplay treatment. Salt remains after boiling/filtering. Distillation exports residue and loses volume. */
public final class WaterTreatment {
 private WaterTreatment(){}
 private static WaterBatch treated(WaterBatch b,int ml,double salt,double risk,boolean distilled){
  if(ml<=0)return WaterBatch.empty();
  var q=b.quality();
  if(distilled)q=WaterBalance.Quality.DRINKABLE;
  else if(q==WaterBalance.Quality.DESALINATE||salt>=1)q=WaterBalance.Quality.DESALINATE;
  else if(q!=WaterBalance.Quality.UNKNOWN)q=risk<=.10?WaterBalance.Quality.DRINKABLE:WaterBalance.Quality.TREAT_RECOMMENDED;
  return new WaterBatch(ml,q,salt,risk,b.origins());
 }
 public static WaterBatch filter(WaterBatch b){return treated(b,b.ml(),b.salinity(),b.risk()*.55,false);}
 public static WaterBatch boil(WaterBatch b){
  int ml=b.ml()-Math.max(1,b.ml()/20);
  return treated(b,ml,b.ml()==0?0:Math.min(1000,b.salinity()*b.ml()/Math.max(1,ml)),b.risk()*.12,false);
 }
 public static WaterBatch distill(WaterBatch b){return treated(b,b.ml()*7/10,0,.01,true);}
}
