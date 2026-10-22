package dev.minemods.sleepdebt.integration;
import dev.minemods.sleepdebt.registry.ModItems;
import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.server.level.ServerPlayer;
/** Only resolved when Accessories is installed. Never accessed from common init. */
public final class SleepAccessoriesBridge {
 public static boolean hasMask(ServerPlayer player){
  var cap=AccessoriesCapability.get(player);
  return cap!=null && cap.isEquipped(ModItems.MASK.get());
 }
 private SleepAccessoriesBridge(){}
}
