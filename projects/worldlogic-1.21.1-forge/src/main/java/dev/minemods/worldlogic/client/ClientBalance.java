package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.RuleSync;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="worldlogic",value=Dist.CLIENT)
public final class ClientBalance {
 private static Player owner;
 private static float roll;
 @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent.Post e){
  Player p=e.player;if(!p.level().isClientSide||p!=Minecraft.getInstance().player)return;
  if(RuleSync.clientDown()&&p.isAlive()&&!p.isPassenger()&&!p.isInWater()&&!p.isSpectator()&&!p.isCreative()){
   if(owner!=p&&p.getForcedPose()==null){owner=p;p.setForcedPose(Pose.SWIMMING);p.setPose(Pose.SWIMMING);}
   p.setSprinting(false);
  }else clearPose();
  float target=RuleSync.clientDown()?RuleSync.clientRoll():0;roll+=(target-roll)*.27f;
 }
 private static void clearPose(){if(owner!=null&&owner.getForcedPose()==Pose.SWIMMING)owner.setForcedPose(null);owner=null;}
 @SubscribeEvent public static void input(MovementInputUpdateEvent e){if(e.getEntity()==Minecraft.getInstance().player&&RuleSync.clientDown()){var i=e.getInput();i.forwardImpulse=0;i.leftImpulse=0;i.jumping=false;}}
 @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles e){if(Minecraft.getInstance().options.getCameraType().isFirstPerson())e.setRoll(e.getRoll()+roll);}
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clearPose();roll=0;}
}
