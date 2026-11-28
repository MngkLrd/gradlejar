package dev.minemods.worldlogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
public final class ReleaseChecks {
 private static void check(boolean ok,String detail){if(!ok)throw new IllegalStateException("World Logic test failed: "+detail);}
 public static void run(){
  PhysicalMaterialChecks.verify();
  check(Rules.keys().size()>=115,"rule count");
  check(EnvironmentPhysics.crampedFactor(2,true)<EnvironmentPhysics.crampedFactor(2,false),"cramped mining geometry");
  check(EnvironmentPhysics.swingFactor(2)<EnvironmentPhysics.swingFactor(0),"weapon clearance");
  check(EnvironmentPhysics.currentStrength(4,1)<EnvironmentPhysics.currentStrength(1,1),"current mass");
  check(StructuralHazards.minBurn(2)<StructuralHazards.minBurn(1),"burn exposure");
  check(StructuralHazards.quenched(Blocks.STONE_BRICKS.defaultBlockState()).is(Blocks.CRACKED_STONE_BRICKS),"quench reaction");
  check(Trauma.guardRisk(10,3,90)>Trauma.guardRisk(4,1,0),"guard overload curve");
  check(Trauma.aggravation(100)>Trauma.aggravation(0),"injury aggravation curve");
  check(Trauma.armFactor(true)<Trauma.armFactor(false),"arm injury mining penalty");
  check(ImpactDynamics.hitRisk(10,3,80,true)>ImpactDynamics.hitRisk(2,1,0,false),"knockdown risk");
  check(ImpactDynamics.collisionRisk(.4,3)>ImpactDynamics.collisionRisk(.1,1),"collision momentum");
  check(ImpactDynamics.shockRadius(10,40)<=5,"shockwave bound");
  check(GroundHazards.recoveryExtra(100,4,8)>GroundHazards.recoveryExtra(0,0,0),"ground recovery curve");
  check(GroundHazards.landingRisk(6,4,true,80)>GroundHazards.landingRisk(3,0,false,0),"heavy landing curve");
  check(Balance.threshold(true,80,2)<Balance.threshold(false,0,0),"injury lowers balance threshold");
  check(Balance.slipRisk(true,80,2,.1)>Balance.slipRisk(false,0,0,0),"injured slip risk");
  check(Balance.duration(1,true)>Balance.duration(.5,false),"fall recovery time");
  check(Condition.weakness(.2,5)>Condition.weakness(1,20),"condition curve");
  check(TravelPhysics.boatFactor(7)<TravelPhysics.boatFactor(1),"boat overload curve");
  check(TravelPhysics.crashDamage(.2)==0&&TravelPhysics.crashDamage(.8)>0,"wall impact threshold");
  check(TorchLifetime.lifetime(2)<TorchLifetime.lifetime(1),"torch lifetime scaling");
  check(Expedition.armorLoad(new ItemStack(Items.IRON_CHESTPLATE))>Expedition.armorLoad(new ItemStack(Items.LEATHER_CHESTPLATE)),"armor diving tradeoff");
  check(CombatConsequences.healDelay(10,1)>CombatConsequences.healDelay(2,1),"damage recovery curve");
  check(CombatConsequences.cargoLoss(4,1)==0&&CombatConsequences.cargoLoss(20,2)==3,"bounded cargo breakage");
  check(CombatConsequences.armorWork(4)>CombatConsequences.armorWork(0),"armor exertion curve");
  check(Expedition.shieldFraction(100)<Expedition.shieldFraction(0),"shield fatigue");
  check(Expedition.restRecovery(true)>Expedition.restRecovery(false),"shelter rest");
  check(Rules.addonSummary().contains("hydration=")&&Rules.addonSummary().contains("worldlogicterrain="),"all five addon ownership records");
  var baseline=Rules.preset("realistic");
  for(String key:Rules.keys())check(Boolean.parseBoolean(baseline.get(key))==!key.equals("modeled_weather"),"realism default "+key);
  for(String family:Rules.tuningKeys())check(Double.parseDouble(baseline.get("strength."+family))==1,"realism intensity "+family);
  for(String retired:new String[]{"light","hardcore","custom"}){
   boolean rejected=false;try{Rules.preset(retired);}catch(IllegalArgumentException ex){rejected=true;}
   check(rejected,"retired profile "+retired);
  }
  var i=new Impulse(new Vec3(1,0,2),2);check(Math.abs(i.energy()-5)<.0001,"impact energy");
  var r=i.reflect(new Vec3(1,0,0),.5);check(Math.abs(r.x+.5)<.0001&&Math.abs(r.z-1)<.0001,"reflection");
  check(Material.weight(new ItemStack(Items.IRON_BLOCK))>Material.weight(new ItemStack(Items.FEATHER)),"item weights");
  check(Content.TORCH.get().defaultBlockState().getLightEmission()==0,"extinguished torch");
  check(new FurnaceBlockEntity(BlockPos.ZERO,Blocks.FURNACE.defaultBlockState()) instanceof FurnaceHeatAccess,"furnace mixin loaded");
 }
 public static void started(ServerStartedEvent e){
  if(!Boolean.getBoolean("worldlogic.ci"))return;
  run();var l=e.getServer().overworld();
  TerrainChecks.verify(l);
  ApiChecks.verify(l);
  verifyWorld(l);
 }
 public static void verifyWorld(net.minecraft.server.level.ServerLevel l){
  PhysicalMaterialChecks.verifyLoaded(l);
  HostileSpawnPolicy.verify(l);
  ExertionModifiers.verify(net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(l));
  CoreCreativeTab.verify(l);
  check(Material.block(Blocks.GLASS.defaultBlockState(),"glass"),"glass tag");
  check(Material.block(Blocks.IRON_BLOCK.defaultBlockState(),"ricochet_metal"),"metal tag");
  check(Material.item(new ItemStack(Items.GLASS_BOTTLE),"fragile_cargo"),"fragile cargo tag");
  check(TorchLifeData.get(l)!=null,"torch persistence");
  check(Material.item(new ItemStack(Items.IRON_AXE),"heavy_weapons"),"heavy weapon tag");
  check(Material.block(Blocks.ROOTED_DIRT.defaultBlockState(),"rough_terrain"),"rough terrain tag");
  check(Material.item(new ItemStack(Items.IRON_SWORD),"disarmable"),"disarm tag");
  check(Material.block(Blocks.CLAY.defaultBlockState(),"structural_weak"),"weak structure tag");
  check(StructuralHazards.verifyWorld(l),"live world support and falling debris fixture");
  check(Material.block(Blocks.PACKED_ICE.defaultBlockState(),"ice_ceiling"),"ice tag");
  check(WorldState.get(l)!=null&&PocketData.get(l)!=null,"saved data");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_CORE_TESTS_PASSED");
 }
}
