package dev.minemods.thermal.server;
import dev.minemods.thermal.ClimateModel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
final class PhysicalChecks {
 private static void require(boolean ok,String name){if(!ok)throw new IllegalStateException("Physical integration QA: "+name);}
 static void verify(ServerLevel l,ServerPlayer p){
  var fallback=new PhysicalBridge.Wall(.85,.75);
  var a=PhysicalBridge.wallFactors(new PhysicalBridge.ThermalProperties(1,2_000_000,2.5),fallback);
  var b=PhysicalBridge.wallFactors(new PhysicalBridge.ThermalProperties(.1,100_000,2.5),fallback);
  require(a.massFactor()>b.massFactor()&&a.conductance()<b.conductance(),"SI heat capacity and nominal thickness affect room independently");
  require(PhysicalBridge.wallFactors(new PhysicalBridge.ThermalProperties(1,Double.NaN,Double.NaN),fallback).equals(fallback),"unknown values do not become zero");
  require(ClimateModel.naturalWater(35,15,"POND",0,2)<ClimateModel.naturalWater(35,15,"POND",0,1),"greater volumetric heat capacity dampens weather response");
  if(!PhysicalBridge.advertised()){
   require(!PhysicalBridge.available()&&PhysicalBridge.item(new ItemStack(Items.IRON_CHESTPLATE)).isEmpty(),"old Core uses explicit fallback");
   require(PhysicalBridge.waterInertia(Fluids.WATER)==1,"old Core fluid neutral multiplier");
   return;
  }
  require(PhysicalBridge.available(),"advertised RC53 API must be usable");
  var leather=new ItemStack(Items.LEATHER_CHESTPLATE);var iron=new ItemStack(Items.IRON_CHESTPLATE);
  require(PhysicalBridge.insulation(leather).orElseThrow()>PhysicalBridge.insulation(iron).orElseThrow(),"actual Core materials control armor thermal response");
  require(EnvironmentSampler.materialInsulation(leather)>EnvironmentSampler.materialInsulation(iron),"production gear uses registry");
  require(PhysicalBridge.waterInertia(Fluids.WATER)==1,"freshwater reference from Core");
  require(PhysicalBridge.item(ItemStack.EMPTY).isEmpty(),"empty item has no physical insulation");
  var pos=p.blockPosition().east(3);var old=l.getBlockState(pos);
  try{
   l.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
   var stone=PhysicalBridge.placed(l,pos).orElseThrow();var sw=PhysicalBridge.wall(l,pos,fallback);
   require(stone.heatCapacityJK()>0&&stone.conductivityWmK()>0&&sw.equals(PhysicalBridge.wallFactors(stone,fallback)),"real placed Core result drives wall");
   l.setBlock(pos,Blocks.OAK_PLANKS.defaultBlockState(),2);
   var wood=PhysicalBridge.placed(l,pos).orElseThrow();
   require(stone.conductivityWmK()>wood.conductivityWmK()&&PhysicalBridge.wall(l,pos,fallback).conductance()<sw.conductance(),"real material changes room conduction");
  }finally{l.setBlock(pos,old,2);}
 }
}
