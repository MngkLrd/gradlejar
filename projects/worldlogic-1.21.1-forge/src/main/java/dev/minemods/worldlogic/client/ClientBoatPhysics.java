package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.RuleSync;
import dev.minemods.worldlogic.TravelPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="worldlogic",value=Dist.CLIENT)
public final class ClientBoatPhysics {
 @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent.Post e){
  if(!e.player.level().isClientSide||e.player!=Minecraft.getInstance().player)return;
  if(e.player.getVehicle() instanceof Boat b&&b.getControllingPassenger()==e.player)TravelPhysics.capBoat(b,RuleSync.boatLimit());
 }
}
