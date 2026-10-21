package dev.minemods.traumacraft.health;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class BodyStates {
    private static final String KEY = "traumacraft.body";
    private BodyStates() {}
    public static BodyState get(Player player) {
        BodyState state = new BodyState();
        CompoundTag tag = player.getPersistentData();
        if (tag.contains(KEY, 10)) state.deserializeNBT(tag.getCompound(KEY));
        return state;
    }
    public static void save(Player player, BodyState state) {
        player.getPersistentData().put(KEY, state.serializeNBT());
    }
    public static void copy(Player from, Player to, boolean death) {
        if (!death && from.getPersistentData().contains(KEY, 10))
            to.getPersistentData().put(KEY, from.getPersistentData().getCompound(KEY).copy());
        else to.getPersistentData().remove(KEY);
    }
}
