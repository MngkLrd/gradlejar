package dev.minemods.worldlogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.server.ServerStartedEvent;
public final class ReleaseChecks {
 private static void check(boolean ok,String detail){if(!ok)throw new IllegalStateException("World Logic test failed: "+detail);}
 public static void run(){
  check(Rules.keys().size()>=81,"rule count");
  check(Balance.threshold(true,80,2)<Balance.threshold(false,0,0),"injury lowers balance threshold");
  check(Balance.slipRisk(true,80,2,.1)>Balance.slipRisk(false,0,0,0),"injured slip risk");
  check(Balance.duration(1,true)>Balance.duration(.5,false),"fall recovery time");
  check(Condition.weakness(.2,5)>Condition.weakness(1,20),"condition curve");
  check(TravelPhysics.boatFactor(7)<TravelPhysics.boatFactor(1),"boat overload curve");
  check(TravelPhysics.crashDamage(.2)==0&&TravelPhysics.crashDamage(.8)>0,"wall impact threshold");
  check(TorchLifetime.lifetime(2)<TorchLifetime.lifetime(1),"torch lifetime scaling");
  check(Rules.preset("realistic").get("torch_lifetime").equals("false"),"torch opt-in");
  check(Expedition.armorLoad(new ItemStack(Items.IRON_CHESTPLATE))>Expedition.armorLoad(new ItemStack(Items.LEATHER_CHESTPLATE)),"armor diving tradeoff");
  check(CombatConsequences.healDelay(10,1)>CombatConsequences.healDelay(2,1),"damage recovery curve");
  check(CombatConsequences.cargoLoss(4,1)==0&&CombatConsequences.cargoLoss(20,2)==3,"bounded cargo breakage");
  check(CombatConsequences.armorWork(4)>CombatConsequences.armorWork(0),"armor exertion curve");
  check(Rules.preset("realistic").get("fragile_cargo").equals("false"),"cargo opt-in");
  check(Expedition.shieldFraction(100)<Expedition.shieldFraction(0),"shield fatigue");
  check(Expedition.restRecovery(true)>Expedition.restRecovery(false),"shelter rest");
  check(Rules.preset("light").get("tree_weight").equals("false"),"light safety");
  check(Rules.preset("hardcore").get("tree_weight").equals("true"),"hardcore coverage");
  var i=new Impulse(new Vec3(1,0,2),2);check(Math.abs(i.energy()-5)<.0001,"impact energy");
  var r=i.reflect(new Vec3(1,0,0),.5);check(Math.abs(r.x+.5)<.0001&&Math.abs(r.z-1)<.0001,"reflection");
  check(Material.weight(new ItemStack(Items.IRON_BLOCK))>Material.weight(new ItemStack(Items.FEATHER)),"item weights");
  check(Content.TORCH.get().defaultBlockState().getLightEmission()==0,"extinguished torch");
  check(new FurnaceBlockEntity(BlockPos.ZERO,Blocks.FURNACE.defaultBlockState()) instanceof FurnaceHeatAccess,"furnace mixin loaded");
 }
 public static void started(ServerStartedEvent e){
  if(!Boolean.getBoolean("worldlogic.ci"))return;
  run();var l=e.getServer().overworld();
  check(Material.block(Blocks.GLASS.defaultBlockState(),"glass"),"glass tag");
  check(Material.block(Blocks.IRON_BLOCK.defaultBlockState(),"ricochet_metal"),"metal tag");
  check(Material.item(new ItemStack(Items.GLASS_BOTTLE),"fragile_cargo"),"fragile cargo tag");
  check(TorchLifeData.get(l)!=null,"torch persistence");
  check(Material.item(new ItemStack(Items.IRON_AXE),"heavy_weapons"),"heavy weapon tag");
  check(WorldState.get(l)!=null&&PocketData.get(l)!=null,"saved data");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_CORE_TESTS_PASSED");
 }
}
