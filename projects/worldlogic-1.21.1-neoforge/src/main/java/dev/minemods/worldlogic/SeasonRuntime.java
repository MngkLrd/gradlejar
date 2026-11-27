package dev.minemods.worldlogic;
import java.util.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
public final class SeasonRuntime{
 private SeasonRuntime(){}
 private static final TagKey<Biome> OCEAN=TagKey.create(Registries.BIOME,ResourceLocation.withDefaultNamespace("is_ocean"));
 public static boolean active(ServerLevel l){return l==l.getServer().overworld()&&NaturalWaterGeneration.enabled(l.getChunkSource().getGenerator());}
 public static Optional<Season> at(ServerLevel l,BlockPos p,Optional<RegionalClimate> region){
  if(!active(l)||l.isOutsideBuildHeight(p)||!l.hasChunkAt(p)||region.isEmpty())return Optional.empty();
  var c=region.get();var state=SeasonState.get(l);
  return Optional.of(SeasonModel.sample(state.phase(l.getGameTime()),state.days(),c.absoluteLatitude(),c.prevailingWindZ(),l.getBiome(p).is(OCEAN)));
 }
 static String describe(ServerLevel l,BlockPos p){return WorldLogicApi.season(l,p).map(s->String.format(Locale.ROOT,"Season %s day %d/%d; local year %.1f%%; air offset %+.1f C; solar %.0f%%",s.kind(),s.dayOfSeason(),s.daysPerSeason(),s.yearProgress()*100,s.temperatureOffsetC(),s.solarFactor()*100)).orElse("Season unavailable: use a loaded Realistic overworld position.");}
 static String setLength(ServerLevel l,int days){if(!active(l))return "Season unavailable: Realistic overworld only.";SeasonState.get(l).length(l.getGameTime(),days);return "Season length: "+days+" elapsed game days; year: "+days*4+"; calendar phase retained.";}
 static String setSeason(ServerLevel l,BlockPos p,SeasonKind kind){if(!active(l)||!l.hasChunkAt(p))return "Season unavailable: use a loaded Realistic overworld position.";var c=GeoDensity.regional(l,p.getX(),p.getZ());if(c.isEmpty())return "No regional climate.";SeasonState.get(l).phase(l.getGameTime(),kind.ordinal()/4.0-SeasonModel.shift(c.get().absoluteLatitude(),c.get().prevailingWindZ())+1e-8);return describe(l,p);}
 /** Change only exposed snow layers; never remove full snow blocks or ice. */
 static boolean applySnow(ServerLevel l,BlockPos p,int action){
  if(action==0||l.isOutsideBuildHeight(p)||!l.hasChunkAt(p)||!l.canSeeSky(p))return false;
  var b=l.getBlockState(p);
  if(action<0){
   if(!b.is(Blocks.SNOW))return false;
   int n=b.getValue(SnowLayerBlock.LAYERS);
   return l.setBlock(p,n<=1?Blocks.AIR.defaultBlockState():b.setValue(SnowLayerBlock.LAYERS,n-1),3);
  }
  if(b.is(Blocks.SNOW)){
   int n=b.getValue(SnowLayerBlock.LAYERS);
   return n<4&&l.setBlock(p,b.setValue(SnowLayerBlock.LAYERS,n+1),3);
  }
  if(!b.isAir()||!Blocks.SNOW.defaultBlockState().canSurvive(l,p)||l.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,p)>=10)return false;
  return l.setBlock(p,Blocks.SNOW.defaultBlockState(),3);
 }
 public static void tick(LevelTickEvent.Post e){
  if(!(e.getLevel() instanceof ServerLevel l)||!active(l)||l.getGameTime()%100!=0||l.players().isEmpty()||!l.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE))return;
  var chunks=new LinkedHashSet<ChunkPos>();
  for(var player:l.players()){if(player.isSpectator())continue;var cp=player.chunkPosition();for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)chunks.add(new ChunkPos(cp.x+dx,cp.z+dz));}
  var list=new ArrayList<>(chunks);if(list.isEmpty())return;
  int start=(int)Math.floorMod(l.getGameTime()/100*16,list.size());
  for(int n=0;n<Math.min(16,list.size());n++){
   var cp=list.get((start+n)%list.size());if(l.getChunkSource().getChunkNow(cp.x,cp.z)==null)continue;
   for(int k=0;k<4;k++){
    int x=cp.getMinBlockX()+l.random.nextInt(16),z=cp.getMinBlockZ()+l.random.nextInt(16);
    int y=l.getHeight(Heightmap.Types.MOTION_BLOCKING,x,z);if(y<=l.getMinBuildHeight()||y>=l.getMaxBuildHeight())continue;
    var q=new BlockPos(x,y,z);if(l.getBlockState(q.below()).is(Blocks.SNOW))q=q.below();
    var b=l.getBlockState(q);if(!b.isAir()&&!b.is(Blocks.SNOW)||!l.canSeeSky(q)||!b.is(Blocks.SNOW)&&!Blocks.SNOW.defaultBlockState().canSurvive(l,q))continue;
    var atm=WorldLogicApi.atmosphere(l,q);if(atm.isEmpty())continue;var a=atm.get();
    applySnow(l,q,SeasonModel.snowAction(a.environment().ambientCelsiusEstimate(),a.precipitation(),a.precipitationIntensity(),l.random.nextDouble()));
   }
  }
 }
}
