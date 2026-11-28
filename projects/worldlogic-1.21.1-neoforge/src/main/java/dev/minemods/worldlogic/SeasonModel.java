package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
/** Continuous gameplay climatology over the existing repeating latitude bands. */
final class SeasonModel{
 private SeasonModel(){}
 static double wrap(double phase){return phase-Math.floor(phase);}
 static double shift(double lat,double windZ){
  // Existing geography has warm/cold repeating belts, not a globe.
  // Fade phase differences at the belt boundaries to avoid a climate seam.
  double blend=LocalWeatherModel.smooth(0,12,lat)*(1-LocalWeatherModel.smooth(78,90,lat));
  return .25+(windZ<0?.25:-.25)*blend;
 }
 static Season sample(double phase,int days,double lat,double windZ,boolean marine){
  lat=Math.max(0,Math.min(90,lat));double local=wrap(phase+shift(lat,windZ));
  double q=local*4;int index=Math.min(3,(int)q);double progress=q-index;
  double heat=Math.cos(2*Math.PI*(local-.375));
  double latFactor=Math.pow(Math.sin(Math.toRadians(lat)),1.3);
  double amplitude=(1.5+18.5*latFactor)*(marine?.55:1);
  double solar=1+.36*latFactor*Math.cos(2*Math.PI*(local-.3125));
  double wet=.5+.5*Math.cos(2*Math.PI*(local-.44));
  double moisture=(wet-.5)*(.08+.18*(1-LocalWeatherModel.smooth(15,35,lat)));
  return new Season(SeasonKind.values()[index],Math.min(days,1+(int)(progress*days)),days,local,progress,amplitude*heat,solar,wet,moisture);
 }
 // Hysteresis avoids repeatedly placing/removing snow near freezing.
 static int snowAction(double air,Precipitation precipitation,double intensity,double random){
  if(air>2)return random<Math.min(.9,.22+(air-2)*.055)?-1:0;
  if(air<=0&&precipitation==Precipitation.SNOW)return random<Math.max(0,Math.min(1,intensity))?1:0;
  return 0;
 }
}
