package dev.minemods.hydration;
import java.util.Set;
import java.util.HashSet;
/** Conserves integer gameplay volume and modeled contamination; Core mass-weighted salinity for supported inputs. */
public record WaterBatch(int ml, WaterBalance.Quality quality, double salinity, double risk, Set<String> origins,long updatedTick) {
 public WaterBatch {
  if(origins==null||origins.size()>16||origins.stream().anyMatch(x->x==null||!x.matches("[A-Z_]{1,32}")))throw new IllegalArgumentException("Invalid origins");
  origins=Set.copyOf(origins);
  if(updatedTick < -1||ml<0||ml>1_000_000||quality==null||!Double.isFinite(salinity)||salinity<0||salinity>1000||!Double.isFinite(risk)||risk<0||risk>1)throw new IllegalArgumentException("Invalid water batch");
  if(ml>0&&quality==WaterBalance.Quality.NOT_WATER)throw new IllegalArgumentException("Not water");
  if(ml==0){quality=WaterBalance.Quality.NOT_WATER;salinity=0;risk=0;origins=Set.of();updatedTick=-1;}
  else if(origins.isEmpty())origins=Set.of("UNKNOWN");
 }
 public WaterBatch(int ml,WaterBalance.Quality quality,double salinity,double risk,Set<String> origins){this(ml,quality,salinity,risk,origins,-1);}
 public WaterBatch(int ml,WaterBalance.Quality quality,double salinity,double risk){this(ml,quality,salinity,risk,ml==0?Set.of():Set.of("UNKNOWN"));}
 public int mB(){return ml;}
 public static WaterBatch empty(){return new WaterBatch(0,WaterBalance.Quality.NOT_WATER,0,0);}
 public record Transfer(WaterBatch source,WaterBatch target,int moved){}
 public WaterBatch withVolume(int amount){return amount==0?empty():new WaterBatch(amount,quality,salinity,risk,origins,updatedTick);}
 /** Game-time storage spoilage proxy. Legacy batches start aging at first observation. */
 public WaterBatch aged(long now,boolean exposed){
  if(ml==0||now<0)return this;
  if(updatedTick<0||now<updatedTick)return new WaterBatch(ml,quality,salinity,risk,origins,now);
  if(now==updatedTick)return this;
  double days=(now-updatedTick)/24000.0;
  // Affine hazard independent of update interval; storage conditions change only the rate.
  double rate=(exposed?.22:.035);
  double newRisk=WaterBalance.clamp(risk+(1-risk)*(-Math.expm1(-days*rate)),0,1);
  var q=quality;
  if(q==WaterBalance.Quality.DRINKABLE&&newRisk>=.30)q=WaterBalance.Quality.TREAT_RECOMMENDED;
  return new WaterBatch(ml,q,salinity,newRisk,origins,now);
 }
 public boolean needsRefresh(long now){return ml>0&&(updatedTick<0||now<updatedTick||now-updatedTick>=1200);}
 public static Transfer transfer(WaterBatch source,WaterBatch target,int requested,int capacity){
  if(requested<0||capacity<0||capacity>1_000_000||target.ml>capacity)throw new IllegalArgumentException("Invalid transfer");
  int moved=Math.min(requested,Math.min(source.ml,capacity-target.ml));
  if(moved==0)return new Transfer(source,target,0);
  int total=target.ml+moved;
  double salt=target.ml==0?source.salinity:WaterPhysics.mixed(source,target,moved).orElseGet(()->(source.salinity*moved+target.salinity*target.ml)/total);
  double dirt=(source.risk*moved+target.risk*target.ml)/total;
  var q=target.ml==0?source.quality:conservative(source.quality,target.quality);
  if(salt>=1)q=WaterBalance.Quality.DESALINATE;
  var origins=new HashSet<String>(target.origins);origins.addAll(source.origins);
  return new Transfer(source.withVolume(source.ml-moved),new WaterBatch(total,q,salt,dirt,origins,Math.max(source.updatedTick,target.updatedTick)),moved);
 }
 private static WaterBalance.Quality conservative(WaterBalance.Quality a,WaterBalance.Quality b){
  if(a==WaterBalance.Quality.DESALINATE||b==WaterBalance.Quality.DESALINATE)return WaterBalance.Quality.DESALINATE;
  if(a==WaterBalance.Quality.UNKNOWN||b==WaterBalance.Quality.UNKNOWN)return WaterBalance.Quality.UNKNOWN;
  if(a==WaterBalance.Quality.TREAT_RECOMMENDED||b==WaterBalance.Quality.TREAT_RECOMMENDED)return WaterBalance.Quality.TREAT_RECOMMENDED;
  return WaterBalance.Quality.DRINKABLE;
 }
}
