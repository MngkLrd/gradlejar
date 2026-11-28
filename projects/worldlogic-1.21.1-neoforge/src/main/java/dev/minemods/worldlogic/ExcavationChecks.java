package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicExcavation;
import dev.minemods.worldlogic.api.WorldLogicCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
final class ExcavationChecks{
 private static net.minecraft.server.level.ServerPlayer testPlayer;
 private static BlockPos testPos;
 private static boolean cancel,observed;
 private static net.neoforged.neoforge.event.level.BlockEvent.BreakEvent observedEvent;
 static void probeObserver(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent e){
  if(testPlayer==null||e.getPlayer()!=testPlayer||!e.getPos().equals(testPos))return;
  observed=dev.minemods.worldlogic.api.WorldLogicBreaks.isProtectionProbe(e);observedEvent=e;
  if(cancel)e.setCanceled(true);
 }
 private static void verifyProtection(ServerLevel l,BlockPos p){
  var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(l);
  var old=l.getBlockState(p);var fatigue=Survival.fatigue(player);
  var state=Blocks.GLASS.defaultBlockState();
  try{
   l.setBlock(p,state,2|16);testPlayer=player;testPos=p.immutable();cancel=false;observed=false;observedEvent=null;
   require(dev.minemods.worldlogic.api.WorldLogicBreaks.checkProtection(l,player,p,state),"accepted protection probe");
   require(observed&&observedEvent!=null&&!dev.minemods.worldlogic.api.WorldLogicBreaks.isProtectionProbe(observedEvent),"probe visible only during event scope");
   require(Survival.fatigue(player)==fatigue&&l.getBlockState(p).equals(state),"probe does not consume stamina or change terrain");
   cancel=true;
   require(!dev.minemods.worldlogic.api.WorldLogicBreaks.checkProtection(l,player,p,state),"late protection cancellation");
   require(Survival.fatigue(player)==fatigue&&l.getBlockState(p).equals(state),"denied probe has no debit");
   require(!player.gameMode.destroyBlock(p)&&Survival.fatigue(player)==fatigue,"denied actual break has no debit");
   require(!dev.minemods.worldlogic.api.WorldLogicBreaks.checkProtection(l,player,p,Blocks.DIRT.defaultBlockState()),"stale state rejected");
   cancel=false;Survival.rest(player,100);
   require(player.gameMode.destroyBlock(p)&&!l.getBlockState(p).equals(state),"actual break succeeds");
   double expected=Math.min(5,.6+state.getDestroySpeed(l,p)*.5)*Rules.strength("movement");
   require(Math.abs(Survival.fatigue(player)-expected)<1e-6,"actual break debits exactly once");
  }finally{testPlayer=null;testPos=null;observedEvent=null;cancel=false;observed=false;l.setBlock(p,old,2|16);player.getPersistentData().putDouble("worldlogic_exertion",fatigue);Survival.clearExertionTracking(player);}
 }
 private static void require(boolean test,String msg){if(!test)throw new IllegalStateException("excavation: "+msg);}
 static void verify(ServerLevel l){
  var c=WorldLogicCapabilities.find(WorldLogicExcavation.KEY,1).orElseThrow();
  var p=new BlockPos(4,110,4);var old=l.getBlockState(p);
  try{
   var pick=new ItemStack(Items.IRON_PICKAXE);
   for(var b:new net.minecraft.world.level.block.Block[]{Blocks.STONE,Blocks.IRON_ORE,Blocks.DEEPSLATE_IRON_ORE}){
    l.setBlock(p,b.defaultBlockState(),2|16);
    var t=c.inspect(l,p,pick).orElseThrow();
    require(t.state().is(b)&&t.kind()==WorldLogicExcavation.Kind.ROCK_OR_ORE&&t.vanillaResistance()>0&&!t.constituents().isEmpty(),"rock and ore sample "+b);
    require(t.toolSuitableForDrops()&&!c.inspect(l,p,ItemStack.EMPTY).orElseThrow().toolSuitableForDrops(),"tool suitability "+b);
   }
   l.setBlock(p,Blocks.DIRT.defaultBlockState(),2|16);
   var proxy=c.inspectOriginal(l,p,Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(),pick).orElseThrow();
   require(proxy.state().is(Blocks.DEEPSLATE_IRON_ORE)&&proxy.kind()==WorldLogicExcavation.Kind.ROCK_OR_ORE&&proxy.toolSuitableForDrops()&&l.getBlockState(p).is(Blocks.DIRT),"original proxy state read without terrain mutation");
   for(String id:MineralContent.IDS){
    var ore=GeologyModel.Ore.valueOf(id.toUpperCase(java.util.Locale.ROOT));var product=c.mineralProduct(MineralContent.ore(ore,false)).orElseThrow();
    require(product.specimen()==MineralContent.specimen(ore),"registered mineral product "+id);
    require(product.refined()==java.util.Set.of("hematite","magnetite","malachite").contains(id),"refining policy "+id);
   }
   require(c.mineralProduct(Blocks.STONE.defaultBlockState()).isEmpty(),"not a Core mineral");
   require(c.inspect(l,p,new ItemStack(Items.IRON_SHOVEL)).orElseThrow().kind()==WorldLogicExcavation.Kind.SOIL,"soil sample");
   l.setBlock(p,Blocks.BEDROCK.defaultBlockState(),2|16);
   require(c.inspect(l,p,pick).isEmpty(),"unbreakable");
   l.setBlock(p,Blocks.AIR.defaultBlockState(),2|16);
   require(c.inspect(l,p,pick).isEmpty(),"air");
   var strike=c.strike(pick,0,.001,2,4).orElseThrow();
   require(Math.abs(strike.incidentEnergyPerAreaJm2()-16000)<1e-6&&c.strike(pick,0,0,2,4).isEmpty(),"strike energy and invalid contact");
  }finally{l.setBlock(p,old,2|16);}
  verifyProtection(l,p);
 }
}
