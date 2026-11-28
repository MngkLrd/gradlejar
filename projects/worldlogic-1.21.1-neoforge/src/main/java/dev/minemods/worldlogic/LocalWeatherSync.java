package dev.minemods.worldlogic;
import java.util.Optional;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
public final class LocalWeatherSync {
 private LocalWeatherSync(){}
 public record State(boolean active,String dimension,int kind,float intensity,float rain,float cloud,float fog,float windX,float windZ,boolean exposed) implements CustomPacketPayload {
  public static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("worldlogic","local_weather"));
  @Override public Type<State> type(){return TYPE;}
  public WeatherKind weatherKind(){return WeatherKind.values()[Math.max(0,Math.min(WeatherKind.values().length-1,kind))];}
 }
 public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,s)->{
  b.writeBoolean(s.active);b.writeUtf(s.dimension,256);b.writeVarInt(s.kind);b.writeFloat(s.intensity);b.writeFloat(s.rain);
  b.writeFloat(s.cloud);b.writeFloat(s.fog);b.writeFloat(s.windX);b.writeFloat(s.windZ);b.writeBoolean(s.exposed);
 },b->new State(b.readBoolean(),b.readUtf(256),b.readVarInt(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readBoolean()));
 private static volatile State client;
 private static volatile long received;
 static void receive(State s){client=s;received=System.nanoTime();}
 public static State client(){return System.nanoTime()-received<5_000_000_000L?client:null;}
 public static void clear(){client=null;received=0;}
 static State snapshot(String dimension,Optional<LocalWeather> w){
  if(w.isEmpty())return new State(false,dimension,0,0,0,0,0,0,0,false);
  var v=w.orElseThrow();
  // Client sky can show regional weather from shelter; individual particles are sky-checked client-side.
  float rain=switch(v.kind()){case RAIN,DOWNPOUR,THUNDERSTORM,MONSOON,SNOW,BLIZZARD->(float)v.intensity();default->0;};
  return new State(true,dimension,v.kind().ordinal(),(float)v.intensity(),rain,(float)v.cloudFraction(),(float)v.fogDensity(),(float)v.windXMps(),(float)v.windZMps(),v.exposed());
 }
 static void send(ServerPlayer p,Optional<LocalWeather> w){PacketDistributor.sendToPlayer(p,snapshot(p.level().dimension().location().toString(),w));}
}
