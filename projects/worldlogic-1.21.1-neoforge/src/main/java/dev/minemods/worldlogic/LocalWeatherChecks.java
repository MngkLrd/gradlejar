package dev.minemods.worldlogic;
import java.util.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
final class LocalWeatherChecks {
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("local weather: "+s);}
 static void verify(ServerLevel l){
  require(Rules.on("regional_weather"),"new default enabled");
  require(!LocalWeatherRuntime.active(l)&&WorldLogicApi.localWeather(l,BlockPos.ZERO).isEmpty(),"flat world isolation");
  var climates=List.of(new LocalWeatherModel.Climate(34,.12,22,1,0,true),
   new LocalWeatherModel.Climate(15,.66,48,1,0,false),
   new LocalWeatherModel.Climate(-14,.72,68,1,0,false),
   new LocalWeatherModel.Climate(29,.88,8,1,0,false),
   new LocalWeatherModel.Climate(9,.95,54,0,0,false));
  var seen=EnumSet.noneOf(WeatherKind.class);var counts=new EnumMap<WeatherKind,Integer>(WeatherKind.class);
  boolean spatial=false,temporal=false;
  int samples=0;
  for(var c:climates)for(long t=0;t<24000L*8;t+=400)for(int x=0;x<=1800;x+=300){
   var a=LocalWeatherModel.sample(42,x,300,t,20,c,true);
   var other=LocalWeatherModel.sample(42,x+1800,300,t,20,c,true);
   var later=LocalWeatherModel.sample(42,x,300,t+4000,20,c,true);
   var covered=LocalWeatherModel.sample(42,x,300,t,20,c,false);
   seen.add(a.kind());counts.merge(a.kind(),1,Integer::sum);samples++;
   spatial|=a.kind()!=other.kind();temporal|=a.kind()!=later.kind();
   require(a.equals(LocalWeatherModel.sample(42,x,300,t,20,c,true)),"deterministic");
   require(covered.precipitation()==Precipitation.NONE&&covered.precipitationIntensity()==0,"roof protection");
   require(covered.temperatureDeltaC()==a.temperatureDeltaC()&&covered.kind()==a.kind(),"same regional air mass under shelter");
   for(double d:new double[]{a.intensity(),a.cloudFraction(),a.fogDensity(),a.solarTransmission(),a.precipitationIntensity()})require(d>=0&&d<=1&&Double.isFinite(d),"normalized field");
   require(Double.isFinite(a.temperatureDeltaC())&&a.relativeHumidityPercent()>=0&&a.relativeHumidityPercent()<=100&&a.pressureHpa()>0,"air data finite");
   if(a.kind()==WeatherKind.DUST_STORM)require(c.dryGround()&&a.precipitation()==Precipitation.NONE&&a.fogDensity()>0,"dry storm");
   if(a.kind()==WeatherKind.MONSOON)require(c.latitude()<35&&a.precipitation()==Precipitation.RAIN,"tropical wet season");
   if(a.kind()==WeatherKind.BLIZZARD||a.kind()==WeatherKind.SNOW)require(a.precipitation()==Precipitation.SNOW,"snow phase");
   var packet=LocalWeatherSync.snapshot("minecraft:overworld",Optional.of(a));
   require(packet.active()&&packet.weatherKind()==a.kind()&&Math.abs(packet.fog()-a.fogDensity())<1e-6&&Math.abs(packet.intensity()-a.intensity())<1e-6,"client state");
  }
  require(seen.size()==WeatherKind.values().length,"all nine weather kinds reached: "+seen);
  require(spatial&&temporal,"weather varies with position and time");
  require(!LocalWeatherSync.snapshot("minecraft:the_nether",Optional.empty()).active(),"inactive client snapshot");
  var c=climates.get(1);
  require(LocalWeatherModel.sample(42,0,0,0,200,c,true).pressureHpa()<LocalWeatherModel.sample(42,0,0,0,0,c,true).pressureHpa(),"pressure decreases with elevation");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_LOCAL_WEATHER samples={} kinds={} spatial={} temporal={}",samples,counts,spatial,temporal);
 }
}
