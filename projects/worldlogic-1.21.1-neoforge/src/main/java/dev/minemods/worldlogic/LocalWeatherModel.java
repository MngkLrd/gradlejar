package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
/** Gameplay-scale fronts on continuous seeded fields. No claim of full numerical weather prediction. */
final class LocalWeatherModel {
 private LocalWeatherModel(){}
 record Climate(double temperatureC,double moisture,double latitude,double prevailingX,double prevailingZ,boolean dryGround){}
 /** Explicit operator-only visual test; blends back into actual weather at the edge and expiry. */
 static LocalWeather demo(LocalWeather natural,WeatherKind kind,double weight){
  double i=kind==WeatherKind.CLEAR?0:.85*weight;
  Precipitation pr=switch(kind){case RAIN,DOWNPOUR,THUNDERSTORM,MONSOON->Precipitation.RAIN;case SNOW,BLIZZARD->Precipitation.SNOW;default->Precipitation.NONE;};
  double delta=switch(kind){case DUST_STORM->3;case BLIZZARD->-8;case SNOW->-5;case MONSOON,DOWNPOUR,THUNDERSTORM->-4;case RAIN->-2;case FOG->-1;default->0;};
  double fog=switch(kind){case DUST_STORM,BLIZZARD,FOG->.85;case MONSOON,DOWNPOUR,THUNDERSTORM->.30;default->.05;};
  double cloud=kind==WeatherKind.CLEAR?.05:kind==WeatherKind.DUST_STORM?.25:.85;
  double rh=kind==WeatherKind.DUST_STORM?22:kind==WeatherKind.CLEAR?45:92;
  double wx=kind==WeatherKind.DUST_STORM||kind==WeatherKind.BLIZZARD?14:4;
  if(weight<.15)return natural;
  return new LocalWeather(kind,i,blend(natural.temperatureDeltaC(),delta,weight),blend(natural.relativeHumidityPercent(),rh,weight),
   blend(natural.windXMps(),wx,weight),blend(natural.windZMps(),2,weight),blend(natural.gustMps(),wx+4,weight),
   blend(natural.cloudFraction(),cloud,weight),blend(natural.fogDensity(),fog,weight),blend(natural.solarTransmission(),1-.75*cloud,weight),
   natural.exposed()?pr:Precipitation.NONE,natural.exposed()&&pr!=Precipitation.NONE?i:0,natural.pressureHpa(),natural.exposed());
 }
 private static double blend(double a,double b,double t){return a+(b-a)*t;}
 static double unit(double x){return Math.max(0,Math.min(1,x));}
 static double smooth(double a,double b,double x){double t=unit((x-a)/(b-a));return t*t*(3-2*t);}
 static LocalWeather sample(long seed,int x,int z,long tick,int elevation,Climate c,boolean exposed){
  var w=RegionalWeatherModel.sample(seed,x,z,tick,0,c.moisture,c.prevailingX,c.prevailingZ);
  // A faster, spatially advecting mesoscale field overlays the slow pressure waves.
  double angle=GeologyModel.unit(GeologyModel.hash(seed,0,9,0))*Math.PI*2;
  double u=(x*Math.cos(angle)+z*Math.sin(angle))/420.0-tick/2300.0+angle;
  double v=(-x*Math.sin(angle)+z*Math.cos(angle))/670.0-tick/3900.0+angle*2;
  double front=.5+.33*Math.sin(u)+.17*Math.sin(v);
  double season=.5+.5*Math.sin(tick/(24000.0*8)*Math.PI*2+angle);
  double wet=unit(.64*c.moisture+.36*w.moistureFraction());
  double monsoon=smooth(.42,.80,wet)*smooth(35,12,c.latitude)*smooth(.40,.85,season);
  double wind=Math.hypot(w.windX(),w.windZ());
  double gust=Math.min(32,wind+12*smooth(.48,.92,front)+5*monsoon);
  double rain=smooth(.38,.79,front)*smooth(.28,.65,wet)*(1-.70*(c.dryGround?1:0));
  rain=unit(rain+.56*monsoon*smooth(.25,.68,front));
  double dust=(c.dryGround?1:0)*smooth(.68,.27,wet)*smooth(.55,.89,front)*smooth(6,15,gust);
  double snowCold=smooth(3,-3,c.temperatureC+w.temperatureAnomalyC()*.5);
  double fog=smooth(.56,.90,wet)*smooth(.40,.05,front)*smooth(8,2,wind);
  WeatherKind kind=WeatherKind.CLEAR;double intensity=0;
  if(dust>.13){kind=WeatherKind.DUST_STORM;intensity=dust;}
  else if(rain>.14){
   intensity=rain;
   if(snowCold>.55)kind=gust>12&&rain>.48?WeatherKind.BLIZZARD:WeatherKind.SNOW;
   else if(monsoon>.22&&rain>.52)kind=WeatherKind.MONSOON;
   else if(rain>.65&&gust>12&&c.temperatureC>8)kind=WeatherKind.THUNDERSTORM;
   else kind=rain>.52?WeatherKind.DOWNPOUR:WeatherKind.RAIN;
  }else if(fog>.17){kind=WeatherKind.FOG;intensity=fog;}
  // Smooth temperature response even at categorical boundaries. No body-temperature effect here.
  double cloud=unit(w.cloudFraction()*.55+rain*.75+fog*.35);
  double delta=w.temperatureAnomalyC()*.65-4.5*rain-4*snowCold*rain+2*dust-1.2*fog;
  double visibility=unit(Math.max(dust*.9,Math.max(fog*.88,rain*(snowCold>.55?.66:.24))));
  double rh=100*unit(.25+.58*wet+.25*rain+.14*fog-.22*dust);
  double boost=1+intensity*1.1;
  double wx=w.windX()*boost,wz=w.windZ()*boost;
  Precipitation pr=switch(kind){case RAIN,DOWNPOUR,THUNDERSTORM,MONSOON->Precipitation.RAIN;case SNOW,BLIZZARD->Precipitation.SNOW;default->Precipitation.NONE;};
  double pressure=w.seaLevelPressureHpa()*Math.exp(-Math.max(-1280,Math.min(4480,elevation*10.0))/8434.0);
  // Rain/snow and wind exposure under shelter are distinct from regional air temperature.
  return new LocalWeather(kind,intensity,delta,rh,wx,wz,gust,cloud,visibility,1-.75*cloud,
   exposed?pr:Precipitation.NONE,exposed&&pr!=Precipitation.NONE?rain:0,pressure,exposed);
 }
}
