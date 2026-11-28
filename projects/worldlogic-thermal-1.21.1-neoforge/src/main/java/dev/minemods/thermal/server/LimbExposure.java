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
  if(s.getItem() instanceof ClimateGarment g)return Math.min(1,PhysicalBridge.garmentInsulation(s,g.profile().insulation())*2.2+g.profile().windShield()*.3);
  var physical=PhysicalBridge.insulation(s);
  if(physical.isPresent())return Math.min(1,physical.getAsDouble()*2.2);
  if(PhysicalBridge.advertised())return Math.min(1,EnvironmentSampler.materialInsulation(s)*2.2);
  // Preserve old-Core fallback behavior until the verified dependency can be advanced.
  String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
  return id.contains("grass")||id.startsWith("leather_")?.55:id.contains("iron")||id.contains("gold")||id.contains("diamond")||id.contains("netherite")?.12:.25;
 }
 static void update(ServerPlayer p,CompoundTag n,ThermalModel.Exposure e,ThermalModel.State state){
  double arms=protection(p.getItemBySlot(EquipmentSlot.CHEST));
  double legs=(protection(p.getItemBySlot(EquipmentSlot.LEGS))+protection(p.getItemBySlot(EquipmentSlot.FEET)))*.5;
  boolean migrate=n.getInt("frostModelRevision")<Adaptation.FROST_MODEL_REVISION;
  // No freezing injury in above-freezing air or liquid solely from a wind/evaporation
  // penalty. Nonfreezing cold injury and systemic hypothermia remain separate mechanics.
  double medium=e.submerged()?e.waterC():e.airC()+e.roomDelta();
  double local=medium>=0?Math.max(0,state.feelsLikeC()):state.feelsLikeC();
  for(int i=0;i<LIMBS.length;i++){
   String key="frost_"+LIMBS[i];
   double protection=i<2?arms:legs;
   double previous=n.getDouble(key);
   if(migrate)previous=Adaptation.migrateFrostDose(previous);
   double dose=Adaptation.frostDose(previous,local,protection,e.wetness());
   // Existing Trauma owns tissue damage. No repeat at half the original exposure.
   // If unavailable, keep bounded pending exposure and never report while warming.
   if(dose>=Adaptation.FROST_THRESHOLD&&Adaptation.frostRate(Adaptation.frostEquivalent(local,protection,e.wetness()))>0){
    if(AddonBridge.frostbite(p,LIMBS[i],1.5f))dose=0;
   }
   n.putDouble(key,dose);
  }
  n.putInt("frostModelRevision",Adaptation.FROST_MODEL_REVISION);
 }
}
