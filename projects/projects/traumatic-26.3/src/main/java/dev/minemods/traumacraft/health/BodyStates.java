package dev.minemods.traumacraft.health;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
/** Per-player medical NBT, common across server saves. */
public final class BodyStates {
    private static final String KEY="traumacraft.body";
    private BodyStates(){}
    public static BodyState get(Player p){
        BodyState state=new BodyState();
        CompoundTag tag=p.getPersistentData();
        if(tag.contains(KEY))state.deserializeNBT(tag.getCompoundOrEmpty(KEY));
        return state;
    }
    public static void save(Player p,BodyState s){p.getPersistentData().put(KEY,s.serializeNBT());}
    public static void copy(Player from,Player to,boolean death){
        if(!death&&from.getPersistentData().contains(KEY))
            to.getPersistentData().put(KEY,from.getPersistentData().getCompoundOrEmpty(KEY).copy());
        else to.getPersistentData().remove(KEY);
    }
}
