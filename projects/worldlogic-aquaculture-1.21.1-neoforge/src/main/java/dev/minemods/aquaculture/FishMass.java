package dev.minemods.aquaculture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import java.util.OptionalDouble;

/** Single gameplay mass authority. Keep old specimen rank/length and save IDs; never rescale twice. */
final class FishMass {
 static final String REVISION="wl_fish_mass_revision";
 static final String KG="wl_fish_mass_kg";
 private FishMass(){}
 static OptionalDouble read(CompoundTag tag,Species s){
  if(!tag.contains(KG,Tag.TAG_ANY_NUMERIC))return OptionalDouble.empty();
  double kg=tag.getDouble(KG);if(!Double.isFinite(kg)||kg<=0)return OptionalDouble.empty();
  if(!tag.contains(REVISION)){
   // 0.4.6 and earlier stored the previous real-world-scaled masses.
   if(kg>s.legacyMaxKg()*1.01)return OptionalDouble.empty();
   return OptionalDouble.of(Math.max(.001,Math.min(s.maxKg(),kg*s.maxKg()/s.legacyMaxKg())));
  }
  if(tag.getInt(REVISION)!=1||kg>s.maxKg()+1e-8)return OptionalDouble.empty();
  return OptionalDouble.of(kg);
 }
 static void mark(CompoundTag tag){tag.putInt(REVISION,1);}
 static void migrate(CompoundTag tag,Species s){
  if(tag.contains(REVISION))return;
  var mass=read(tag,s);
  if(mass.isPresent()){tag.putDouble(KG,mass.getAsDouble());mark(tag);}
 }
}
