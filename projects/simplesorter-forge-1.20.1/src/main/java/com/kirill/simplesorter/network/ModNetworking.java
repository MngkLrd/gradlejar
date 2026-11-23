package com.kirill.simplesorter.network;
import com.kirill.simplesorter.SimpleSorterMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public final class ModNetworking {
 private static final String VERSION="2";
 public static final SimpleChannel CHANNEL=NetworkRegistry.ChannelBuilder
  .named(new ResourceLocation(SimpleSorterMod.MOD_ID,"network"))
  .networkProtocolVersion(()->VERSION)
  .clientAcceptedVersions(VERSION::equals)
  .serverAcceptedVersions(VERSION::equals)
  .simpleChannel();
 private ModNetworking(){}
 public static void register(){
  CHANNEL.registerMessage(0,WandSettingsPayload.class,WandSettingsPayload::write,WandSettingsPayload::read,(p,ctx)->handleSettings(p,ctx));
  CHANNEL.registerMessage(1,WandActionPayload.class,WandActionPayload::write,WandActionPayload::read,(p,ctx)->handleAction(p,ctx));
  CHANNEL.registerMessage(2,FlyingItemsPayload.class,FlyingItemsPayload::write,FlyingItemsPayload::read,FlyingItemsPayload::handle);
 }
 private static void handleSettings(WandSettingsPayload payload,Supplier<NetworkEvent.Context> c){
  var ctx=c.get();ctx.enqueueWork(()->{var player=ctx.getSender();if(player!=null)payload.handle(player);});ctx.setPacketHandled(true);
 }
 private static void handleAction(WandActionPayload payload,Supplier<NetworkEvent.Context> c){
  var ctx=c.get();ctx.enqueueWork(()->{var player=ctx.getSender();if(player!=null)payload.handle(player);});ctx.setPacketHandled(true);
 }
}
