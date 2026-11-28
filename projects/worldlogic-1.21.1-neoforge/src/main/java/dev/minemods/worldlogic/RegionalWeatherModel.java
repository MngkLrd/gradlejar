package dev.minemods.worldlogic;

/** Experimental continuous, seed-driven synoptic fields. Not a fluid solver or live precipitation controller. */
public final class RegionalWeatherModel {
 private RegionalWeatherModel(){}
 public record Sample(double seaLevelPressureHpa,double localPressureHpa,double temperatureAnomalyC,
  double moistureFraction,double cloudFraction,double precipitationPotential,double windX,double windZ,double gustMps){}
 private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
 private static double phase(long seed){
  long v=seed;
  v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;
  v=(v^(v>>>27))*0x94d049bb133111ebL;
  v^=v>>>31;
  return (v>>>11)*0x1.0p-53*Math.PI*2;
 }
 public static Sample sample(long seed,double x,double z,long tick,double elevation,double moisture,double prevailingX,double prevailingZ){
  // Two overlapping advecting waves. Fixed seed-dependent orientation prevents cell seams.
  double angle=phase(seed),cx=Math.cos(angle),sz=Math.sin(angle);
  double time=tick/24000.0;
  double u=(x*cx+z*sz)/1800.0-time*.85+angle;
  double v=(-x*sz+z*cx)/3100.0-time*.42+phase(seed^0x9e3779b97f4a7c15L);
  double p=1013.25+15*Math.sin(u)+9*Math.sin(v);
  double gradU=15*Math.cos(u)/1800.0,gradV=9*Math.cos(v)/3100.0;
  double gradX=gradU*cx-gradV*sz,gradZ=gradU*sz+gradV*cx;
  // Pressure-gradient-driven component and prevailing climatological flow, in game-scale m/s.
  double wx=2.2*prevailingX-520*gradX,wz=2.2*prevailingZ-520*gradZ;
  double speed=Math.hypot(wx,wz);
  if(speed>18){wx*=18/speed;wz*=18/speed;speed=18;}
  double low=clamp((1017-p)/28,0,1);
  double anomaly=5.5*Math.sin(u-.8)+2.5*Math.sin(v+.7);
  double moist=clamp(clamp(moisture,0,1)+.20*Math.sin(u+1.6)+.14*Math.sin(v-.4)+.12*low,0,1);
  double cloud=clamp((moist-.28)*1.25+.40*low,0,1);
  double precip=clamp((cloud-.55)*1.7*low,0,1);
  double local=p*Math.exp(-clamp(elevation,-128,512)/8434.0);
  double gust=Math.min(24,speed+2*cloud+4*precip);
  return new Sample(p,local,anomaly,moist,cloud,precip,wx,wz,gust);
 }
}
