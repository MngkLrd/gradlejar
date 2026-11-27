package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.LocalWeatherSync;
import dev.minemods.worldlogic.api.WorldLogicApi.WeatherKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Reuses the vanilla precipitation renderer; additional particles and terrain fog are client-local. */
@EventBusSubscriber(modid="worldlogic",value=Dist.CLIENT)
public final class LocalWeatherRenderer {
 private LocalWeatherRenderer(){}
 private static final BlockParticleOption SAND=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.SAND.defaultBlockState());
 private static ClientLevel lastLevel;
 private static float rain,thunder,fog;
 private static boolean owned;
 private static LocalWeatherSync.State current(){
  var mc=Minecraft.getInstance();var s=LocalWeatherSync.client();
  return mc.level!=null&&s!=null&&s.active()&&s.dimension().equals(mc.level.dimension().location().toString())?s:null;
 }
 private static float lerp(float a,float b,float t){return a+(b-a)*t;}
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){
  var mc=Minecraft.getInstance();var l=mc.level;var p=mc.player;
  if(l!=lastLevel){lastLevel=l;rain=0;thunder=0;fog=0;owned=false;}
  if(l==null||p==null)return;
  var s=current();
  if(s==null){
   if(owned){l.setRainLevel(0);l.setThunderLevel(0);owned=false;}
   rain=0;thunder=0;fog=0;return;
  }
  owned=true;var kind=s.weatherKind();
  rain=lerp(rain,s.rain(),.12f);
  thunder=lerp(thunder,kind==WeatherKind.THUNDERSTORM?s.intensity():0,.10f);
  fog=lerp(fog,s.exposed()?s.fog():s.fog()*.08f,.10f);
  l.setRainLevel(rain);l.setThunderLevel(thunder);
  if(!s.exposed()||p.tickCount%2!=0||s.intensity()<.08)return;
  boolean dust=kind==WeatherKind.DUST_STORM,blizzard=kind==WeatherKind.BLIZZARD;
  boolean snow=kind==WeatherKind.SNOW||blizzard;
  boolean wet=kind==WeatherKind.RAIN||kind==WeatherKind.DOWNPOUR||kind==WeatherKind.MONSOON||kind==WeatherKind.THUNDERSTORM;
  // Additional particles cover local phase mismatches with static vanilla biome temperatures.
  if(!dust&&!snow&&!wet)return;
  int count=(int)((dust||blizzard?26:wet?10:7)*s.intensity());
  for(int i=0;i<count;i++){
   double x=p.getX()+(l.random.nextDouble()-.5)*18,z=p.getZ()+(l.random.nextDouble()-.5)*18;
   double y=p.getY()+.8+l.random.nextDouble()*7;
   var pos=BlockPos.containing(x,y,z);
   if(l.isOutsideBuildHeight(pos)||!l.hasChunkAt(pos)||!l.canSeeSky(pos)||!l.isEmptyBlock(pos))continue;
   if(dust)l.addParticle(SAND,x,y,z,s.windX()*.09,-.015,s.windZ()*.09);
   else if(snow)l.addParticle(ParticleTypes.SNOWFLAKE,x,y,z,s.windX()*.035,-.08,s.windZ()*.035);
   else l.addParticle(ParticleTypes.RAIN,x,y,z,s.windX()*.025,-.32,s.windZ()*.025);
  }
 }
 @SubscribeEvent public static void fogDistance(ViewportEvent.RenderFog e){
  var s=current();if(s==null||fog<.05||e.getType()!=FogType.NONE||e.getFarPlaneDistance()<8)return;
  float target=lerp(e.getFarPlaneDistance(),Math.min(e.getFarPlaneDistance(),12),fog*fog);
  e.setFarPlaneDistance(target);e.setNearPlaneDistance(Math.min(e.getNearPlaneDistance(),target*.14f));e.setCanceled(true);
 }
 @SubscribeEvent public static void fogColor(ViewportEvent.ComputeFogColor e){
  var s=current();if(s==null||fog<.05||e.getCamera().getFluidInCamera()!=FogType.NONE)return;
  boolean sand=s.weatherKind()==WeatherKind.DUST_STORM;
  float blend=Math.min(.83f,fog*.8f);
  e.setRed(lerp(e.getRed(),sand?.77f:.74f,blend));
  e.setGreen(lerp(e.getGreen(),sand?.57f:.78f,blend));
  e.setBlue(lerp(e.getBlue(),sand?.32f:.82f,blend));
 }
}
