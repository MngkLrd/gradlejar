package dev.minemods.aquaculture;

import dev.minemods.worldlogic.api.PhysicalMaterials;
import dev.minemods.worldlogic.api.PhysicalWater;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/** Core owns physical properties; Aqua supplies only persistent biological specimen mass. */
final class AquaPhysical {
 private static final ResourceLocation TISSUE=ResourceLocation.fromNamespaceAndPath("worldlogic","fish_tissue");
 private AquaPhysical(){}
 static void register(){
  for(Species s:Species.ALL)PhysicalMaterials.registerIndividualItemProvider(
   ResourceLocation.fromNamespaceAndPath(Aquaculture.ID,"raw_"+s.id()),AquaPhysical::individual);
 }
 private static Optional<PhysicalMaterials.IndividualItem> individual(ItemStack stack){
  var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
  if(!tag.contains("wl_fish_mass_kg",Tag.TAG_ANY_NUMERIC))return Optional.empty();
  double mass=tag.getDouble("wl_fish_mass_kg");
  if(!Double.isFinite(mass)||mass<=0)return Optional.empty();
  // One relative tissue volume, not one cubic metre of actual fish. Core scales it to specimen mass.
  return Optional.of(new PhysicalMaterials.IndividualItem(mass,List.of(new PhysicalMaterials.Component(TISSUE,1))));
 }
 static Optional<PhysicalWater.Solution> water(Aquaculture.Habitat h){
  return PhysicalWater.solution(h.salt(),h.temp());
 }
 static OptionalDouble freezing(Aquaculture.Habitat h){return PhysicalWater.freezingPointC(h.salt());}
 static String describe(Aquaculture.Habitat h){
  var solution=water(h);
  String freeze=freezing(h).isPresent()?Double.toString(freezing(h).getAsDouble()):"unknown";
  return solution.map(s->" densityKgM3="+s.densityKgM3()+" viscosityPaS="+s.viscosityPaS()
    +" specificHeatJkgK="+s.specificHeatJkgK()+" boilingCAt1Atm="+s.boilingCAt1Atm())
    .orElse(" densityKgM3=unknown viscosityPaS=unknown specificHeatJkgK=unknown boilingCAt1Atm="+PhysicalWater.boilingPointC(h.salt()).orElse(Double.NaN))
    +" freezingC="+freeze+" (Core estimate using Aqua habitat temperature proxy)";
 }
}
