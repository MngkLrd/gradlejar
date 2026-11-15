package dev.kirill.embershelter.camp;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
/** Only actual player-caused health damage provokes a mob, never nearby mobs. */
public final class RetaliationTracker {
    private record Anger(UUID player,long until) {}
    private static final WeakHashMap<Mob,Anger> ANGER=new WeakHashMap<>();
    private RetaliationTracker(){}
    public static void provoke(Mob mob,ServerPlayer player,ServerLevel level) {
        ANGER.put(mob,new Anger(player.getUUID(),level.getGameTime()+200));
        if(!(mob instanceof Animal) && mob.isAlive()) mob.setTarget(player);
    }
    public static boolean isProvoked(Mob mob,ServerPlayer player,ServerLevel level) {
        Anger anger=ANGER.get(mob);
        if(anger==null) return false;
        if(level.getGameTime()>anger.until){ ANGER.remove(mob); return false; }
        return anger.player.equals(player.getUUID());
    }
    public static ServerPlayer provokedTarget(Animal mob) {
        if(!(mob.level() instanceof ServerLevel level)) return null;
        Anger anger=ANGER.get(mob);
        if(anger==null) return null;
        if(level.getGameTime()>anger.until){ ANGER.remove(mob); return null; }
        ServerPlayer p=level.getServer().getPlayerList().getPlayer(anger.player);
        return p==null || p.level()!=level || !p.isAlive() || mob.distanceToSqr(p)>400 ? null : p;
    }
    public static void clear(){ANGER.clear();}
}
