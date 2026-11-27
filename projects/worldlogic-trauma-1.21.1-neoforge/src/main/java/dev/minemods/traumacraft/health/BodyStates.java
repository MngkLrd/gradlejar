package dev.minemods.traumacraft.health;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
/** Server-thread only. NeoForge persistent NBT owns the durable state; cache is never shared with clients. */
public final class BodyStates {
    public static final String KEY = "traumacraft_body";
    private static final Map<Player,BodyState> CACHE = new WeakHashMap<>();
    private BodyStates() {}
    public static BodyState get(Player p) {
        if (!(p instanceof ServerPlayer)) throw new IllegalStateException("Server-only body state");
        return CACHE.computeIfAbsent(p, key -> {
            BodyState state = new BodyState();
            if (p.getPersistentData().contains(KEY,10))
                state.deserializeNBT(p.registryAccess(), p.getPersistentData().getCompound(KEY));
            return state;
        });
    }
    public static boolean flush(Player p) {
        BodyState state=get(p);
        if (!state.takeDirty()) return false;
        p.getPersistentData().put(KEY,state.serializeNBT(p.registryAccess()));
        return true;
    }
    public static void forget(Player p) { CACHE.remove(p); }
    /** Replace both cached and serialized state, even if a clone listener copied old NBT. */
    public static void resetForNewLife(Player p) {
        CACHE.put(p,new BodyState());
        flush(p);
    }
    public static void copy(Player from, Player to, boolean death) {
        CACHE.remove(to);
        if (death) { resetForNewLife(to); return; }
        CompoundTag tag=get(from).serializeNBT(from.registryAccess());
        to.getPersistentData().put(KEY,tag.copy());
    }
}
