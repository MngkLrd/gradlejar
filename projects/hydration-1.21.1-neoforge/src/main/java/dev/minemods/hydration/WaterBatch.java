package dev.minemods.hydration;

/** Conserves volume and modeled salt/contamination load in a closed transfer. */
public record WaterBatch(int ml, WaterBalance.Quality quality, double salinity, double risk) {
 public WaterBatch {
  if(ml<0||ml>1_000_000||quality==null||!Double.isFinite(salinity)||salinity<0||salinity>1000||!Double.isFinite(risk)||risk<0||risk>1)throw new IllegalArgumentException("Invalid water batch");
  if(ml>0&&quality==WaterBalance.Quality.NOT_WATER)throw new IllegalArgumentException("Not water");
  if(ml==0){quality=WaterBalance.Quality.NOT_WATER;salinity=0;risk=0;}
 }
 public static WaterBatch empty(){return new WaterBatch(0,WaterBalance.Quality.NOT_WATER,0,0);}
 public record Transfer(WaterBatch source,WaterBatch target,int moved){}
 private WaterBatch withVolume(int amount){return amount==0?empty():new WaterBatch(amount,quality,salinity,risk);}
 public static Transfer transfer(WaterBatch source,WaterBatch target,int requested,int capacity){
  if(requested<0||capacity<0||capacity>1_000_000||target.ml>capacity)throw new IllegalArgumentException("Invalid transfer");
  int moved=Math.min(requested,Math.min(source.ml,capacity-target.ml));
  if(moved==0)return new Transfer(source,target,0);
  int total=target.ml+moved;
  double salt=(source.salinity*moved+target.salinity*target.ml)/total;
  double dirt=(source.risk*moved+target.risk*target.ml)/total;
  var q=target.ml==0?source.quality:conservative(source.quality,target.quality);
  if(salt>=1)q=WaterBalance.Quality.DESALINATE;
  return new Transfer(source.withVolume(source.ml-moved),new WaterBatch(total,q,salt,dirt),moved);
 }
 private static WaterBalance.Quality conservative(WaterBalance.Quality a,WaterBalance.Quality b){
  if(a==WaterBalance.Quality.DESALINATE||b==WaterBalance.Quality.DESALINATE)return WaterBalance.Quality.DESALINATE;
  if(a==WaterBalance.Quality.UNKNOWN||b==WaterBalance.Quality.UNKNOWN)return WaterBalance.Quality.UNKNOWN;
  if(a==WaterBalance.Quality.TREAT_RECOMMENDED||b==WaterBalance.Quality.TREAT_RECOMMENDED)return WaterBalance.Quality.TREAT_RECOMMENDED;
  return WaterBalance.Quality.DRINKABLE;
 }
}
