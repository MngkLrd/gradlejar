package dev.minemods.thermal.server;
import dev.minemods.thermal.Adaptation;
import dev.minemods.thermal.ThermalModel;
import dev.minemods.thermal.content.ClimateGarment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
final class LimbExposure {
 static final String[] LIMBS={"LEFT_ARM","RIGHT_ARM","LEFT_LEG","RIGHT_LEG"};
 private LimbExposure(){}
 static double protection(ItemStack s){
  if(s.isEmpty())return 0;
  if(s.getItem() instanceof ClimateGarment g)return Math.min(1,g.profile().insulation()*2.2+g.profile().windShield()*.3);
  String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
  return id.contains("grass")||id.startsWith("leather_")?.55:id.contains("iron")||id.contains("gold")||id.contains("diamond")||id.contains("netherite")?.12:.25;
 }
 static void update(ServerPlayer p,CompoundTag n,ThermalModel.Exposure e,ThermalModel.State state){
  double arms=protection(p.getItemBySlot(EquipmentSlot.CHEST));
  double legs=(protection(p.getItemBySlot(EquipmentSlot.LEGS))+protection(p.getItemBySlot(EquipmentSlot.FEET)))*.5;
  for(int i=0;i<LIMBS.length;i++){
   String key="frost_"+LIMBS[i];
   double dose=Adaptation.frostDose(n.getDouble(key),state.feelsLikeC(),i<2?arms:legs,e.wetness());
   // Distinct peripheral tissue damage, not a second application of the systemic damage event.
   if(dose>=120){AddonBridge.frostbite(p,LIMBS[i],1.5f);dose=60;}
   n.putDouble(key,dose);
  }
 }
}
