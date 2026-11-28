package dev.minemods.worldlogic.api;

import java.util.*;
import dev.minemods.worldlogic.NaturalFluids;
import dev.minemods.worldlogic.api.PhysicalMaterials.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

public final class PhysicalAddonChecks {
 private static ResourceLocation id(String key){return ResourceLocation.fromNamespaceAndPath("worldlogic",key);}
 private static void require(boolean value,String label){if(!value)throw new IllegalStateException("Physical addon API: "+label);}
 private static void equal(double actual,double expected,double tolerance,String label){require(Math.abs(actual-expected)<=tolerance,label+" actual="+actual+" expected="+expected);}
 private static final ResourceLocation TEST_WOOD=ResourceLocation.fromNamespaceAndPath("worldlogic_qa","test_wood");
 private static final IndividualItemProvider PROVIDER=stack->{
   require(stack.getCount()==1,"provider receives one item");
   var custom=stack.get(DataComponents.CUSTOM_DATA);
   if(custom==null)return Optional.empty();
   var tag=custom.copyTag();
   if(!tag.contains("worldlogic_qa_mass"))return Optional.empty();
   return Optional.of(new IndividualItem(tag.getDouble("worldlogic_qa_mass"),tag.getBoolean("worldlogic_qa_unknown")?List.of():List.of(new Component(id("fish_tissue"),1))));
 };
 private static ResourceLocation testItemId(){return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Items.PAPER);}
 private static void registerFixtures(){
  var n=Double.NaN;
  PhysicalMaterials.registerMaterial(new MaterialProfile(TEST_WOOD,Phase.SOLID,500,n,30,50,8,n,.12,.15,1500,n,300,.7,.5,n,n));
  try{
   PhysicalMaterials.registerWoodProperties(TEST_WOOD,new WoodProperties(3,70,.8));
   PhysicalMaterials.registerIndividualItemProvider(testItemId(),PROVIDER);
  }catch(RuntimeException ex){PhysicalMaterials.removeQaFixture(TEST_WOOD,testItemId(),PROVIDER);throw ex;}
 }
 private static ItemStack specimen(double mass,int count,boolean unknown){
  var s=new ItemStack(Items.PAPER,count);var data=new CompoundTag();data.putDouble("worldlogic_qa_mass",mass);data.putBoolean("worldlogic_qa_unknown",unknown);s.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return s;
 }
 public static void verify(){
  registerFixtures();
  try{
  for(String name:List.of("flax","fur","straw","felt","fish_tissue","fish_scale","aquatic_shell","crustacean_shell")){
   var m=PhysicalMaterials.material(id(name)).orElseThrow();
   require(m.dryDensityKgM3()>0&&m.heatCapacityJkgK()>0&&m.conductivityWmK()>0,name+" valid profile");
  }
  var dry=new State(20,0);
  var flax=new ThermalLayer(id("flax"),.02,dry);
  var felt=new ThermalLayer(id("felt"),.04,dry);
  double r1=PhysicalMaterials.thermalResistance(List.of(flax)).orElseThrow().areaResistanceM2KW();
  double r2=PhysicalMaterials.thermalResistance(List.of(felt)).orElseThrow().areaResistanceM2KW();
  var series=PhysicalMaterials.thermalResistance(List.of(flax,felt)).orElseThrow();
  equal(series.areaResistanceM2KW(),r1+r2,1e-10,"series resistance");
  equal(series.conductanceWm2K(),1/(r1+r2),1e-10,"conductance");
  require(PhysicalMaterials.thermalResistance(List.of(new ThermalLayer(id("flax"),.02,new State(20,.3)))).orElseThrow().areaResistanceM2KW()<r1,"wet textile");
  require(PhysicalMaterials.thermalResistance(List.of(new ThermalLayer(id("missing"),.02,dry))).isEmpty(),"unknown layer");
  for(String wood:List.of("oak","dark_oak","spruce","birch","jungle","acacia","mangrove","cherry","bamboo","crimson","warped")){
   var p=PhysicalMaterials.woodProperties(id(wood)).orElseThrow();
   var a=PhysicalMaterials.woodAtMoisture(id(wood),.12).orElseThrow();var b=PhysicalMaterials.woodAtMoisture(id(wood),p.greenMoistureKgKg()).orElseThrow();
   require(b.densityKgM3()>a.densityKgM3()&&b.bendingStrengthMPa()<a.bendingStrengthMPa()&&b.flammability()<=a.flammability(),wood+" moisture response");
   double density=PhysicalMaterials.evaluate(new ObjectProfile(List.of(new Component(id(wood),1)),Evidence.EXPLICIT),new State(20,.12)).massKg();
   equal(a.densityKgM3(),density,1e-8,wood+" canonical density");
  }
  equal(PhysicalMaterials.woodAtMoisture(id("oak"),.12).orElseThrow().densityKgM3(),710,1e-8,"Terrarian oak reference");
  equal(PhysicalMaterials.woodHardnessKN(id("oak")).orElseThrow(),5.9,1e-8,"oak Janka kN");
  equal(PhysicalMaterials.woodHardnessKN(TEST_WOOD).orElseThrow(),3,1e-8,"addon Janka");
  equal(PhysicalMaterials.woodProperties(TEST_WOOD).orElseThrow().bendingStrengthMPa(),70,1e-8,"addon bending");
  require(PhysicalMaterials.woodHardnessKN(id("warped")).isEmpty(),"fungal Janka unknown");
  require(PhysicalMaterials.woodAtMoisture(id("oak"),Double.NaN).isEmpty(),"invalid moisture");
  var fresh=PhysicalWater.solution(0,20).orElseThrow();
  var sea=PhysicalWater.solution(35,20).orElseThrow();
  var mixed=PhysicalWater.solution(17.5,20).orElseThrow();
  equal(fresh.densityKgM3(),998.2,1,"fresh density");
  equal(sea.densityKgM3(),1024.8,1,"sea density");
  equal(sea.specificHeatJkgK(),3990,80,"sea heat capacity");
  equal(sea.viscosityPaS(),.001077,.00005,"sea viscosity");
  require(fresh.densityKgM3()<mixed.densityKgM3()&&mixed.densityKgM3()<sea.densityKgM3(),"intermediate salinity");
  require(sea.specificHeatJkgK()<fresh.specificHeatJkgK()&&sea.viscosityPaS()>fresh.viscosityPaS(),"saline properties");
  require(PhysicalWater.solution(35,80).orElseThrow().viscosityPaS()<sea.viscosityPaS(),"hot liquid viscosity");
  equal(PhysicalWater.freezingPointC(35).orElseThrow(),-1.92,.1,"sea freezing");
  equal(PhysicalWater.freezingPointC(Fluids.WATER).orElseThrow(),0,1e-8,"pure water freezing");
  for(var f:NaturalFluids.families()){
   var expected=PhysicalWater.freezingPointC(f.source.get()).orElseThrow();
   equal(PhysicalWater.freezingPointC(f.flowing.get()).orElseThrow(),expected,1e-8,f.id+" freezing threshold");
  }
  require(PhysicalWater.boilingPointC(35).orElseThrow()>100,"salt boiling elevation");
  equal(PhysicalWater.mixedSalinityGkg(1,0,3,35).orElseThrow(),26.25,1e-8,"salt mass balance");
  require(PhysicalWater.solution(121,20).isEmpty()&&PhysicalWater.solution(35,-2).isEmpty()&&PhysicalWater.solution(Double.NaN,20).isEmpty()&&PhysicalWater.freezingPointC(60).isEmpty(),"out of range water");
  var a=specimen(.25,1,false);var b=specimen(.75,2,false);
  equal(PhysicalMaterials.itemResult(a).massKg(),.25,1e-8,"individual A");
  equal(PhysicalMaterials.itemResult(b).massKg(),.75,1e-8,"individual B");
  equal(PhysicalMaterials.stackResult(b).massKg(),1.5,1e-8,"individual stack");
  equal(PhysicalMaterials.itemResult(b.copy()).volumeM3(),.75/1060,1e-10,"volume derived from actual mass");
  var unknown=PhysicalMaterials.itemResult(specimen(.5,1,true));
  equal(unknown.massKg(),.5,1e-8,"known mass without composition");
  require(Double.isNaN(unknown.volumeM3())&&Double.isNaN(unknown.specificHeatJkgK()),"no invented individual composition");
  require(PhysicalMaterials.itemResult(new ItemStack(Items.PAPER)).object().evidence()!=Evidence.UNKNOWN,"static fallback");
  equal(PhysicalMaterials.impactEnergyJ(70,4).orElseThrow(),560,1e-8,"explicit kinetic energy");
  require(PhysicalMaterials.impactEnergyJ(-1,4).isEmpty()&&PhysicalMaterials.impactEnergyJ(70,Double.NaN).isEmpty(),"invalid impact");
  var axe=new ItemStack(Items.IRON_AXE);
  var head=PhysicalMaterials.itemContact(axe,0,.001,1,4).orElseThrow();
  var handle=PhysicalMaterials.itemContact(axe,1,.002,1,4).orElseThrow();
  require(!head.contactingComponent().material().id().equals(handle.contactingComponent().material().id()),"actual component selected");
  require(PhysicalMaterials.itemContact(axe,2,.001,1,4).isEmpty()&&PhysicalMaterials.itemContact(axe,0,0,1,4).isEmpty(),"invalid contact");
  }finally{PhysicalMaterials.removeQaFixture(TEST_WOOD,testItemId(),PROVIDER);}
 }
 public static void verifyLoaded(ServerLevel l){
  var spawn=l.getSharedSpawnPos();var p=new BlockPos(spawn.getX(),Math.min(l.getMaxBuildHeight()-2,Math.max(l.getMinBuildHeight()+2,spawn.getY()+4)),spawn.getZ());
  require(l.hasChunkAt(p),"contact fixture loaded");
  var old=l.getBlockState(p);var oldBelow=l.getBlockState(p.below());
  try{
   l.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());
   l.setBlockAndUpdate(p,Blocks.STONE.defaultBlockState());
   double hard=PhysicalMaterials.surfaceContact(l,p).orElseThrow().stoppingDistanceM();
   l.setBlockAndUpdate(p,Blocks.WHITE_WOOL.defaultBlockState());
   require(PhysicalMaterials.surfaceContact(l,p).orElseThrow().stoppingDistanceM()>hard,"soft vs stone");
   l.setBlockAndUpdate(p,Blocks.OAK_PLANKS.defaultBlockState());
   require(PhysicalMaterials.surfaceContact(l,p).orElseThrow().stoppingDistanceM()>hard,"wood vs stone");
   l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
   require(PhysicalMaterials.surfaceContact(l,p).isEmpty(),"air not surface");
   require(PhysicalMaterials.surfaceContact(l,new BlockPos(0,l.getMaxBuildHeight(),0)).isEmpty(),"out of world");
  }finally{l.setBlockAndUpdate(p,old);l.setBlockAndUpdate(p.below(),oldBelow);}
 }
}
