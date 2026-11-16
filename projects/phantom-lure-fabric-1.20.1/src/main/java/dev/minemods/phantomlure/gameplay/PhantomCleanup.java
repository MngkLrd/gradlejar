package dev.minemods.phantomlure.gameplay;
import dev.minemods.phantomlure.blockentity.PhantomLureBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Phantom;
public final class PhantomCleanup {
 public static final String LURE_TAG="phantomlure.summoned";
 public static void onWorldTick(ServerLevel level) {
    if(level.getGameTime()%20L!=0L)return;
    boolean night=PhantomLureBlockEntity.isLureNight(level);
    if(night&&level.players().stream().noneMatch(ServerPlayer::isSleeping))return;
    for(Entity e:level.getAllEntities()) {
      if(!(e instanceof Phantom phantom)||!phantom.getTags().contains(LURE_TAG))continue;
      if(!night||level.players().stream().anyMatch(player->player.isSleeping()&&player.distanceToSqr(phantom)<96.0*96.0))
        phantom.discard();
    }
 }
 private PhantomCleanup(){}
}
