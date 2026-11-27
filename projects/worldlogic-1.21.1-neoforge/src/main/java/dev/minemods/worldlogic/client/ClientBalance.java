package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.RuleSync;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid="worldlogic",value=Dist.CLIENT)
public final class ClientBalance {
 private static Player owner;
 private static float roll;
 @SubscribeEvent public static void tick(PlayerTickEvent.Post e){
  Player p=e.getEntity();if(!p.level().isClientSide||p!=Minecraft.getInstance().player)return;
  if(RuleSync.clientDown()&&p.isAlive()&&!p.isPassenger()&&!p.isInWater()&&!p.isSpectator()&&!p.isCreative()){
   if(owner!=null&&owner!=p)clearPose();if(owner!=p&&p.getForcedPose()==null){owner=p;p.setForcedPose(Pose.SWIMMING);p.setPose(Pose.SWIMMING);}
   p.setSprinting(false);
  }else clearPose();
  float target=RuleSync.clientDown()?RuleSync.clientRoll()*(RuleSync.clientCrawl()?.45f:1):0;roll+=(target-roll)*.27f;
 }
 private static boolean local(Player p){return p==Minecraft.getInstance().player&&RuleSync.clientDown()&&p.isAlive()&&!p.isCreative()&&!p.isSpectator();}
 @SubscribeEvent public static void attack(net.neoforged.neoforge.event.entity.player.AttackEntityEvent e){if(local(e.getEntity()))e.setCanceled(true);}
 private static void interact(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent e){if(e instanceof net.neoforged.bus.api.ICancellableEvent c&&local(e.getEntity()))c.setCanceled(true);}
 @SubscribeEvent public static void interactRightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e){interact(e);}
 @SubscribeEvent public static void interactRightClickItem(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem e){interact(e);}
 @SubscribeEvent public static void interactEntityInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract e){interact(e);}
 @SubscribeEvent public static void interactEntityInteractSpecific(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific e){interact(e);}
 @SubscribeEvent public static void interactLeftClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e){interact(e);}
 @SubscribeEvent public static void use(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start e){if(e.getEntity() instanceof Player p&&local(p))e.setCanceled(true);}
 @SubscribeEvent public static void breakSpeed(net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed e){if(local(e.getEntity()))e.setNewSpeed(0);}
 private static void clearPose(){if(owner!=null&&owner.getForcedPose()==Pose.SWIMMING)owner.setForcedPose(null);owner=null;}
 @SubscribeEvent public static void input(MovementInputUpdateEvent e){if(e.getEntity()==Minecraft.getInstance().player&&(RuleSync.clientDown()||RuleSync.clientHanging())){var i=e.getInput();if(!RuleSync.clientCrawl()){i.forwardImpulse=0;i.leftImpulse=0;}else{i.forwardImpulse*=.5f;i.leftImpulse*=.5f;}i.jumping=false;}}
 @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles e){if(Minecraft.getInstance().options.getCameraType().isFirstPerson())e.setRoll(e.getRoll()+roll);}
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clearPose();roll=0;}
}
