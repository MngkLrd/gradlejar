package dev.minemods.worldlogic;
import java.util.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.neoforged.neoforge.network.PacketDistributor;
public final class AnalyticsSync{
 private AnalyticsSync(){}
 public record Cell(int dx,int dz,float air,float soil){}
 public record Point(long tick,float air,float soil){}
 public record State(String dimension,int x,int y,int z,long tick,List<List<String>> tabs,List<Cell> nearby,List<Point> history) implements CustomPacketPayload{
  public static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("worldlogic","analytics"));
  public Type<State> type(){return TYPE;}
 }
 public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,s)->{
  b.writeUtf(s.dimension,256);b.writeInt(s.x);b.writeInt(s.y);b.writeInt(s.z);b.writeLong(s.tick);
  b.writeVarInt(s.tabs.size());for(var tab:s.tabs){b.writeVarInt(tab.size());for(String row:tab)b.writeUtf(row,256);}
  b.writeVarInt(s.nearby.size());for(var c:s.nearby){b.writeInt(c.dx);b.writeInt(c.dz);b.writeFloat(c.air);b.writeFloat(c.soil);}
  b.writeVarInt(s.history.size());for(var p:s.history){b.writeLong(p.tick);b.writeFloat(p.air);b.writeFloat(p.soil);}
 },b->{
  String d=b.readUtf(256);int x=b.readInt(),y=b.readInt(),z=b.readInt();long t=b.readLong();
  int n=b.readVarInt();if(n<0||n>6)throw new IllegalArgumentException("analytics tabs");
  var tabs=new ArrayList<List<String>>();for(int i=0;i<n;i++){int count=b.readVarInt();if(count<0||count>32)throw new IllegalArgumentException("analytics rows");var rows=new ArrayList<String>();for(int k=0;k<count;k++)rows.add(b.readUtf(256));tabs.add(List.copyOf(rows));}
  n=b.readVarInt();if(n<0||n>9)throw new IllegalArgumentException("analytics cells");var cells=new ArrayList<Cell>();for(int i=0;i<n;i++)cells.add(new Cell(b.readInt(),b.readInt(),b.readFloat(),b.readFloat()));
  n=b.readVarInt();if(n<0||n>48)throw new IllegalArgumentException("analytics history");var points=new ArrayList<Point>();for(int i=0;i<n;i++)points.add(new Point(b.readLong(),b.readFloat(),b.readFloat()));
  return new State(d,x,y,z,t,List.copyOf(tabs),List.copyOf(cells),List.copyOf(points));
 });
 private static volatile State client;
 static void receive(State s){client=s;}public static State client(){return client;}public static void clear(){client=null;}
 private static String fmt(String key,String format,Object... values){return key+"|"+String.format(Locale.ROOT,format,values);}
 static void send(ServerPlayer p,AnalyticsState data){
  var l=p.serverLevel();var pos=p.blockPosition();var c=data.cells.get(EnvironmentAnalytics.key(pos));var air=new ArrayList<String>();var water=new ArrayList<String>();var soil=new ArrayList<String>();var impact=new ArrayList<String>();var ecology=new ArrayList<String>();var seasons=new ArrayList<String>();
  air.add(fmt("biome_id","%s",l.getBiome(pos).unwrapKey().map(k->k.location().toString()).orElse("unknown")));
  WorldLogicApi.atmosphere(l,pos).ifPresent(a->{air.add(fmt("temperature","%.1f C",a.environment().ambientCelsiusEstimate()));air.add(fmt("humidity","%.0f %%",a.relativeHumidityPercent()));air.add(fmt("wind","%.1f m/s",a.outdoorWindSpeedMps()));air.add(fmt("solar","%.0f %%",a.environment().solarExposure()*100));air.add(fmt("precipitation","%s / %.0f %%",a.precipitation(),a.precipitationIntensity()*100));});
  WorldLogicApi.season(l,pos).ifPresent(s->{seasons.add("season_"+s.kind().name().toLowerCase(Locale.ROOT)+"|");seasons.add(fmt("season_day","%d / %d",s.dayOfSeason(),s.daysPerSeason()));seasons.add(fmt("season_year","%.1f %%",s.yearProgress()*100));seasons.add(fmt("season_progress","%.1f %%",s.seasonProgress()*100));seasons.add(fmt("season_temp","%+.1f C",s.temperatureOffsetC()));seasons.add(fmt("season_solar","%.0f %%",s.solarFactor()*100));seasons.add(fmt("season_wet","%.0f %%",s.wetSeasonStrength()*100));});
  WorldLogicApi.localWeather(l,pos).ifPresent(w->{air.add(fmt("weather","%s",w.kind()));air.add(fmt("pressure","%.1f hPa",w.pressureHpa()));air.add(fmt("gust","%.1f m/s",w.gustMps()));});
  if(c!=null&&c.tick>=0){air.add(fmt("air_load","%.0f %%",c.air()*100));air.add(fmt("dust","%.0f %%",c.dust*100));air.add(fmt("combustion","%.0f %%",c.combustion*100));air.add(fmt("sources","%d",c.sources));soil.add(fmt("soil_load","%.0f %%",c.soil*100));}
  var hit=l.clip(new ClipContext(p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(6)),ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,p));
  var q=hit.getType()==HitResult.Type.BLOCK?hit.getBlockPos():pos;
  var w=WorldLogicApi.waterProfile(l,q);
  if(w.kind()!=WorldLogicApi.WaterKind.NONE&&w.kind()!=WorldLogicApi.WaterKind.UNKNOWN){
   water.add(fmt("water_pos","%d %d %d",q.getX(),q.getY(),q.getZ()));water.add(fmt("water_kind","%s",w.kind()));water.add(fmt("water_quality","%s",w.quality()));water.add(fmt("salinity","%.1f g/kg",w.salinityGkg()));water.add(fmt("water_risk","%.0f %%",w.contaminationRisk()*100));
  }else water.add("look_water|");
  WorldLogicApi.hydrology(l,pos).ifPresent(h->{water.add(fmt("groundwater","%.1f mm",h.groundwaterMm()));water.add(fmt("snow","%.1f mm",h.snowWaterMm()));water.add(fmt("runoff","%.2f mm",h.lastRunoffMm()));water.add(fmt("evaporation","%.2f mm",h.lastEvaporationMm()));soil.add(fmt("soil_type","%s",h.soil()));soil.add(fmt("saturation","%.0f %%",h.saturation()*100));soil.add(fmt("fertility","%.0f %%",h.fertility()*100));soil.add(fmt("permeability","%.0f %%",h.permeability()*100));soil.add(fmt("soil_water","%.1f / %.1f mm",h.soilWaterMm(),h.capacityMm()));soil.add(fmt("simulated","%s",h.simulated()));});
  if(c!=null){impact.add(fmt("logs","%d",c.logs));impact.add(fmt("mined","%d",c.mined));impact.add(fmt("disturbed","%d",c.disturbed));impact.add(fmt("placed","%d",c.placed));if(c.sources>0)impact.add("advice_fire|");if(c.disturbed>0)impact.add("advice_soil|");}
  impact.add("attribution|");
  WorldLogicApi.forestObservation(l,pos).ifPresent(v->{
   ecology.add(fmt("canopy","%.1f %%",v.canopyCover()*100));ecology.add(fmt("canopy_change","%+.1f pp",(v.canopyCover()-v.baselineCover())*100));ecology.add(fmt("canopy_recovery","%.1f pp",v.cumulativeRecovery()*100));ecology.add(fmt("observed_tick","%d",v.observedTick()));
  });
  WorldLogicApi.soilEcology(l,pos).ifPresent(v->{
   soil.add(fmt("salt_mass","%.2f g/m2",v.saltGm2()));soil.add(fmt("soil_salinity","%.2f g/L",v.salinityGPerLiter()));soil.add(fmt("salt_export","%.2f g/m2",v.lastSaltExportGm2()));
   if(v.lastVegetationTick()>=0){
    ecology.add(fmt("plant_cover","%.1f %%",v.vegetationCover()*100));ecology.add(fmt("root_retention","%.1f %%",v.rootRetention()*100));ecology.add(fmt("transpiration","%.2f mm",v.lastTranspirationMm()));ecology.add(fmt("total_transpiration","%.2f mm",v.totalTranspirationMm()));ecology.add(fmt("plant_recovery","%.1f pp",v.recoveredCover()*100));ecology.add(fmt("reported_tick","%d",v.lastVegetationTick()));
   }else ecology.add("no_plant_report|");
   ecology.add(fmt("habitat_fertility","%.1f %%",v.effectiveFertility()*100));
  });
  var nearby=new ArrayList<Cell>();var ch=p.chunkPosition();
  for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){var n=data.cells.get(ChunkPos.asLong(ch.x+dx,ch.z+dz));if(n!=null&&n.tick>=0)nearby.add(new Cell(dx,dz,(float)n.air(),(float)n.soil));}
  var history=new ArrayList<Point>();if(c!=null)for(var h:c.history)history.add(new Point(h.tick(),(float)h.air(),(float)h.soil()));
  PacketDistributor.sendToPlayer(p,new State(l.dimension().location().toString(),pos.getX(),pos.getY(),pos.getZ(),l.getGameTime(),List.of(List.copyOf(air),List.copyOf(water),List.copyOf(soil),List.copyOf(impact),List.copyOf(ecology),List.copyOf(seasons)),List.copyOf(nearby),List.copyOf(history)));
 }
}
