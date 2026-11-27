package dev.minemods.hydration;

/** Deterministic gameplay flow. Each source sends at most one bounded batch per second. */
public final class GravityFlow {
 public static final int RATE=FluidUnits.SIP_MB, FILTER_CAPACITY=12*FluidUnits.BUCKET_MB, MAX_PIPE_LENGTH=24;
 public record Result(WaterBatch source,WaterBatch target,int moved,int remainingFilter){}
 private GravityFlow(){}
 public static Result move(WaterBatch source,WaterBatch target,int targetCapacity,int sourceY,int targetY,int charge,boolean filter){
  if(targetY>=sourceY)return new Result(source,target,0,charge);
  return transfer(source,target,targetCapacity,RATE,charge,filter);
 }
 public static Result transfer(WaterBatch source,WaterBatch target,int targetCapacity,int requested,int charge,boolean filter){
  int limit=Math.min(Math.max(0,requested),filter?Math.max(0,charge):Integer.MAX_VALUE);
  int moved=Math.min(limit,Math.min(source.ml(),Math.max(0,targetCapacity-target.ml())));
  if(moved==0)return new Result(source,target,0,charge);
  var split=WaterBatch.transfer(source,WaterBatch.empty(),moved,moved);
  var liquid=filter?WaterTreatment.filter(split.target()):split.target();
  var add=WaterBatch.transfer(liquid,target,moved,targetCapacity);
  return new Result(split.source(),add.target(),moved,filter?charge-moved:charge);
 }
 public static int rainMl(boolean exposed,boolean liquidRain,double intensity){
  return exposed&&liquidRain?(int)Math.round(60*WaterBalance.clamp(intensity,0,1)):0;
 }
}
