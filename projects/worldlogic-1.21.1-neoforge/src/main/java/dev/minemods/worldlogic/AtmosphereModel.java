package dev.minemods.worldlogic;

/** Bounded, deterministic weather estimates. Does not mutate weather or generate terrain. */
public final class AtmosphereModel {
 private AtmosphereModel(){}
 public record Wind(double x,double z,double speed){}
 private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
 private static double saturation(double c){
  c=clamp(c,-80,80);
  return Math.exp(17.625*c/(243.04+c));
 }
 /** Approximate vapor pressure from regional moisture and reference air temperature. */
 public static double humidity(double air,double reference,double moisture,double wetWeather){
  double dew=reference-(2+27*(1-clamp(moisture,0,1)));
  double dry=clamp(saturation(dew)/saturation(air),0,1);
  return 100*clamp(dry+(1-dry)*.75*clamp(wetWeather,0,1),0,1);
 }
 public static Wind wind(long seed,int x,int z,long tick,double dx,double dz,double rain,double thunder){
  double phase=(seed & 0xffffL)*Math.PI/32768.0;
  double wave=phase+x/1800.0+z/2100.0+tick/4800.0;
  double angle=Math.hypot(dx,dz)<1e-8?phase+Math.sin(wave)*.8:Math.atan2(dz,dx)+Math.sin(wave)*.35;
  double speed=clamp(2.5+1.6*Math.sin(wave)+.8*Math.sin(wave*.63+phase)+3*clamp(rain,0,1)+6*clamp(thunder,0,1),.2,15);
  return new Wind(Math.cos(angle)*speed,Math.sin(angle)*speed,speed);
 }
}
