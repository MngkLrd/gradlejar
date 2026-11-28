package dev.minemods.aquaculture;

import dev.minemods.worldlogic.api.PhysicalMaterials;
import dev.minemods.worldlogic.api.PhysicalWater;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Real registered items and Core callbacks, including old catch NBT and missing/invalid data. */
final class AquaPhysicalChecks {
 private static void require(boolean value,String what){if(!value)throw new IllegalStateException("Aqua physical: "+what);}
 private static boolean near(double a,double b){return Double.isFinite(a)&&Math.abs(a-b)<1e-8*Math.max(1,Math.abs(b));}
 static void run(ServerLevel level){
  require(WorldLogicApi.API_REVISION>=15&&PhysicalMaterials.REVISION>=2,"Core revisions");
  for(Species s:Species.ALL){
   var e=Aquaculture.TYPES.get(s.id()).get().create(level);
   require(e instanceof Fish,"factory "+s.id());
   Fish fish=(Fish)e;
   ItemStack catchItem=Aquaculture.catchStack(fish,s);
   var data=catchItem.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
   double mass=data.getDouble("wl_fish_mass_kg");
   var r=PhysicalMaterials.itemResult(catchItem);
   require(near(r.massKg(),mass)&&r.volumeM3()>0&&r.heatCapacityJK()>0,"catch profile "+s.id());
   require(near(PhysicalMaterials.stackResult(catchItem.copy()).massKg(),mass),"copy "+s.id());
   require(near(PhysicalMaterials.droppedItemResult(new ItemEntity(level,0,0,0,catchItem.copy())).massKg(),mass),"drop "+s.id());
   require(data.equals(catchItem.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag()),"provider changed NBT "+s.id());
   ItemStack other=new ItemStack(Aquaculture.RAW.get(s.id()).get());
   var old=new CompoundTag();old.putDouble("wl_fish_mass_kg",s.legacyMaxKg()*.8);
   other.set(DataComponents.CUSTOM_DATA,CustomData.of(old));
   double migrated=s.maxKg()*.8;
   require(near(PhysicalMaterials.itemResult(other).massKg(),migrated),"legacy gameplay conversion "+s.id());
   require(near(PhysicalMaterials.itemResult(other).volumeM3(),r.volumeM3()*migrated/mass),"Core scales volume "+s.id());
   require(!other.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().contains(FishMass.REVISION),"pure legacy provider "+s.id());
   FishMass.migrate(old,s);FishMass.migrate(old,s);
   require(near(old.getDouble(FishMass.KG),migrated)&&old.getInt(FishMass.REVISION)==1,"one-time migration "+s.id());
   other.set(DataComponents.CUSTOM_DATA,CustomData.of(old));
   require(near(PhysicalMaterials.itemResult(other).massKg(),migrated),"new NBT individual "+s.id());
   ItemStack blank=new ItemStack(Aquaculture.RAW.get(s.id()).get());
   require(Double.isNaN(PhysicalMaterials.itemResult(blank).massKg()),"no invented mass "+s.id());
   old.putDouble("wl_fish_mass_kg",Double.NaN);other.set(DataComponents.CUSTOM_DATA,CustomData.of(old));
   require(Double.isNaN(PhysicalMaterials.itemResult(other).massKg()),"invalid mass "+s.id());
   fish.discard();
  }
  var fresh=new Aquaculture.Habitat(WorldLogicApi.WaterKind.LAKE,20,0,.1,8);
  var sea=new Aquaculture.Habitat(WorldLogicApi.WaterKind.SEA,20,35,.1,8);
  var f=AquaPhysical.water(fresh).orElseThrow();var s=AquaPhysical.water(sea).orElseThrow();
  require(s.equals(PhysicalWater.solution(35,20).orElseThrow())&&s.densityKgM3()>f.densityKgM3()&&s.viscosityPaS()>f.viscosityPaS(),"Core water");
  var cold=new Aquaculture.Habitat(WorldLogicApi.WaterKind.SEA,-1,35,.1,8);
  require(AquaPhysical.water(cold).isEmpty()&&AquaPhysical.freezing(cold).orElseThrow()<0,"subzero unknown liquid model");
  var unknown=new Aquaculture.Habitat(WorldLogicApi.WaterKind.UNKNOWN,20,-1,.1,8);
  require(AquaPhysical.water(unknown).isEmpty()&&AquaPhysical.freezing(unknown).isEmpty(),"unknown salinity");
 }
}
