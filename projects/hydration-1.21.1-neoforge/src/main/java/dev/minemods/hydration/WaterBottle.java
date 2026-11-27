package dev.minemods.hydration;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

/** Single vanilla bottle, fixed 250 mL gameplay volume. Server owns writes. */
public final class WaterBottle {
 private static final String PROFILE="hydration_profile";
 private WaterBottle(){}
 public static boolean isWater(ItemStack stack){
  var potion=stack.get(DataComponents.POTION_CONTENTS);
  return stack.is(Items.POTION)&&potion!=null&&potion.is(Potions.WATER);
 }
 private static WaterBalance.Quality quality(String q){
  try{var v=WaterBalance.Quality.valueOf(q);return v==WaterBalance.Quality.NOT_WATER?WaterBalance.Quality.UNKNOWN:v;}
  catch(IllegalArgumentException ex){return WaterBalance.Quality.UNKNOWN;}
 }
 private static WaterBatch unknown(){return new WaterBatch(250,WaterBalance.Quality.UNKNOWN,0,1,Set.of("UNKNOWN"));}
 public static Optional<WaterBatch> read(ItemStack stack){
  if(!isWater(stack))return Optional.empty();
  var custom=stack.get(DataComponents.CUSTOM_DATA);
  if(custom==null)return Optional.of(unknown());
  var data=custom.copyTag().getCompound(PROFILE);
  if(data.isEmpty())return Optional.of(unknown());
  var q=quality(data.getString("quality"));
  if(!data.contains("salinity")||!data.contains("risk"))return Optional.of(unknown());
  double salt=data.getDouble("salinity"),risk=data.getDouble("risk");
  if(!Double.isFinite(salt)||!Double.isFinite(risk)||salt<0||risk<0)return Optional.of(unknown());
  if(salt>=1)q=WaterBalance.Quality.DESALINATE;
  Set<String> origins=new HashSet<>();
  for(Tag n:data.getList("origins",Tag.TAG_STRING))origins.add(n.getAsString());
  if(origins.isEmpty()&&!data.getString("kind").isEmpty())origins.add(data.getString("kind"));
  try{return Optional.of(new WaterBatch(250,q,salt,risk,origins,data.contains("updated_tick")?Math.max(-1,data.getLong("updated_tick")):-1));}
  catch(IllegalArgumentException ex){return Optional.of(unknown());}
 }
 static WaterBatch fromSource(WorldLogicApi.WaterProfile p){
  if(p.quality()==WorldLogicApi.WaterQuality.NOT_WATER)throw new IllegalArgumentException("Not water");
  var q=quality(p.quality().name());
  double salt=p.salinityGkg(),risk=p.contaminationRisk();
  if(!Double.isFinite(salt)||!Double.isFinite(risk)||salt<0||risk<0)return new WaterBatch(250,WaterBalance.Quality.UNKNOWN,0,1,Set.of(p.kind().name()));
  if(salt>=1)q=WaterBalance.Quality.DESALINATE;
  return new WaterBatch(250,q,salt,risk,Set.of(p.kind().name()));
 }
 static ItemStack create(WorldLogicApi.WaterProfile p,long now){
  var batch=fromSource(p).aged(now,false);
  ItemStack bottle=PotionContents.createItemStack(Items.POTION,Potions.WATER);
  CustomData.update(DataComponents.CUSTOM_DATA,bottle,tag->{
   CompoundTag data=new CompoundTag();data.putInt("volume_ml",batch.ml());
   data.putString("kind",p.kind().name());data.putString("quality",batch.quality().name());
   data.putDouble("salinity",batch.salinity());data.putDouble("risk",batch.risk());
   data.putString("evidence",p.evidence().name());data.putLong("updated_tick",batch.updatedTick());
   ListTag origins=new ListTag();batch.origins().stream().sorted().forEach(x->origins.add(StringTag.valueOf(x)));data.put("origins",origins);
   tag.put(PROFILE,data);
  });
  return bottle;
 }
 static boolean refresh(ItemStack stack,long now){
  var before=read(stack);if(before.isEmpty()||!before.get().needsRefresh(now))return false;
  var batch=before.get().aged(now,false);
  CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->{
   CompoundTag data=tag.getCompound(PROFILE);data.putString("quality",batch.quality().name());data.putDouble("salinity",batch.salinity());data.putDouble("risk",batch.risk());data.putLong("updated_tick",batch.updatedTick());
   ListTag origins=new ListTag();batch.origins().stream().sorted().forEach(x->origins.add(StringTag.valueOf(x)));data.put("origins",origins);tag.put(PROFILE,data);
  });return true;
 }
}
