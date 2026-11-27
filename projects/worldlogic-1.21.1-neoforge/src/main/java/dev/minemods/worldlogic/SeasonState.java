package dev.minemods.worldlogic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
/** Per-world phase anchor. Changing duration does not jump the calendar. */
final class SeasonState extends SavedData{
 static final int DEFAULT_DAYS=24;
 private int days=DEFAULT_DAYS;private long anchorTick;private double anchorPhase;
 static SeasonState get(ServerLevel l){return l.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(SeasonState::new,SeasonState::load,null),"worldlogic_seasons");}
 int days(){return days;}
 double phase(long tick){return SeasonModel.wrap(anchorPhase+(Math.max(0L,tick-anchorTick)/(24000.0*4*days)));}
 void length(long tick,int value){if(value<1||value>365)throw new IllegalArgumentException("season length");anchorPhase=phase(tick);anchorTick=tick;days=value;setDirty();}
 void phase(long tick,double value){if(!Double.isFinite(value))throw new IllegalArgumentException("phase");anchorPhase=SeasonModel.wrap(value);anchorTick=tick;setDirty();}
 static SeasonState load(CompoundTag n,HolderLookup.Provider lookup){var s=new SeasonState();int d=n.getInt("days");s.days=d>=1&&d<=365?d:DEFAULT_DAYS;s.anchorTick=Math.max(0,n.getLong("anchorTick"));double p=n.getDouble("anchorPhase");s.anchorPhase=Double.isFinite(p)?SeasonModel.wrap(p):0;return s;}
 @Override public CompoundTag save(CompoundTag n,HolderLookup.Provider lookup){n.putInt("days",days);n.putLong("anchorTick",anchorTick);n.putDouble("anchorPhase",anchorPhase);return n;}
}
