package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.RuleSync;
import dev.minemods.worldlogic.TravelPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.vehicle.Boat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="worldlogic",value=Dist.CLIENT)
public final class ClientBoatPhysics {
 @SubscribeEvent public static void tick(PlayerTickEvent.Post e){
  if(!e.getEntity().level().isClientSide||e.getEntity()!=Minecraft.getInstance().player)return;
  if(e.getEntity().getVehicle() instanceof Boat b&&b.getControllingPassenger()==e.getEntity())TravelPhysics.capBoat(b,RuleSync.boatLimit());
 }
}
