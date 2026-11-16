package dev.minemods.phantomlure.gameplay;
import dev.minemods.phantomlure.blockentity.PhantomLureBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Phantom;
public final class PhantomCleanup {
 private static final String MARKER="_phantomlure_summoned_";
 public static void mark(Phantom phantom) {
    // Vanilla persisted hidden name in 26.x; scoreboard tags no longer exist.
    phantom.setCustomName(Component.literal(MARKER));
    phantom.setCustomNameVisible(false);
 }
 public static void onWorldTick(ServerLevel level) {
    if(level.getGameTime()%20L!=0L)return;
    boolean night=PhantomLureBlockEntity.isLureNight(level);
    if(night&&level.players().stream().noneMatch(ServerPlayer::isSleeping))return;
    for(Entity e:level.getAllEntities()) {
      if(!(e instanceof Phantom phantom)||phantom.getCustomName()==null||
         !MARKER.equals(phantom.getCustomName().getString()))continue;
      if(!night||level.players().stream().anyMatch(player->player.isSleeping()&&player.distanceToSqr(phantom)<96.0*96.0))
        phantom.discard();
    }
 }
 private PhantomCleanup(){}
}
