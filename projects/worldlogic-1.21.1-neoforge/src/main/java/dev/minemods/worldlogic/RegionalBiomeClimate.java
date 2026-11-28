package dev.minemods.worldlogic;
import java.util.function.BiFunction;
/** A globally aligned, climate-only regional interpolation. It never writes
 * elevation, weather or the public environmental climate. */
final class RegionalBiomeClimate{
 static final int GRID=128;
 record Sample(double temperature,double humidity){}
 private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
 private static double smooth(double u){return u*u*(3-2*u);}
 private static double mix(double a,double b,double u){return a+(b-a)*u;}
 static Sample anchorBlend(int x,int z,BiFunction<Integer,Integer,Sample> sample){
  int gx=Math.floorDiv(x,GRID)*GRID,gz=Math.floorDiv(z,GRID)*GRID;
  double u=smooth(Math.floorMod(x,GRID)/(double)GRID),v=smooth(Math.floorMod(z,GRID)/(double)GRID);
  var a=sample.apply(gx,gz);var b=sample.apply(gx+GRID,gz);var c=sample.apply(gx,gz+GRID);var d=sample.apply(gx+GRID,gz+GRID);
  return new Sample(mix(mix(a.temperature,b.temperature,u),mix(c.temperature,d.temperature,u),v),
   mix(mix(a.humidity,b.humidity,u),mix(c.humidity,d.humidity,u),v));
 }
 static Sample blend(int x,int z,Sample local,BiFunction<Integer,Integer,Sample> sample){
  var region=anchorBlend(x,z,sample);
  // Preserve the original climate belts and rare extremes. Regional averaging
  // is a limited correction, not a rescaling of the original map.
  return new Sample(clamp(local.temperature+clamp((region.temperature-local.temperature)*.55,-.075,.075),-1,1),
   clamp(local.humidity+clamp((region.humidity-local.humidity)*.70,-.12,.12),-1,1));
 }
 static void verify(){
  BiFunction<Integer,Integer,Sample> field=(x,z)->new Sample(Math.sin(x*.001),Math.cos(z*.001));
  for(int x=-256;x<=256;x+=4)for(int z=-256;z<=256;z+=4){
   var original=field.apply(x,z);var regional=blend(x,z,original,field);
   if(Math.abs(regional.temperature-original.temperature)>.07500001||Math.abs(regional.humidity-original.humidity)>.12000001)
    throw new IllegalStateException("regional climate correction exceeded");
   if(!regional.equals(blend(x,z,original,field)))throw new IllegalStateException("regional climate determinism");
  }
  for(int x:new int[]{-128,0,128})for(int z:new int[]{-128,0,128}){
   var a=anchorBlend(x,z,field);if(!a.equals(field.apply(x,z)))throw new IllegalStateException("global anchor seam");
   var left=anchorBlend(x-1,z,field);var right=anchorBlend(x+1,z,field);
   if(Math.abs(left.temperature-right.temperature)>.001)throw new IllegalStateException("regional anchor discontinuity");
  }
  var uniform=blend(-5,-17,new Sample(.4,-.7),(x,z)->new Sample(.4,-.7));
  if(!uniform.equals(new Sample(.4,-.7)))throw new IllegalStateException("uniform climate changed");
 }
}
