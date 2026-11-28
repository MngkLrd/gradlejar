package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicExcavation;
import dev.minemods.worldlogic.api.WorldLogicCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
final class ExcavationChecks{
 private static void require(boolean test,String msg){if(!test)throw new IllegalStateException("excavation: "+msg);}
 static void verify(ServerLevel l){
  var c=WorldLogicCapabilities.find(WorldLogicExcavation.KEY,0).orElseThrow();
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
   require(c.inspect(l,p,new ItemStack(Items.IRON_SHOVEL)).orElseThrow().kind()==WorldLogicExcavation.Kind.SOIL,"soil sample");
   l.setBlock(p,Blocks.BEDROCK.defaultBlockState(),2|16);
   require(c.inspect(l,p,pick).isEmpty(),"unbreakable");
   l.setBlock(p,Blocks.AIR.defaultBlockState(),2|16);
   require(c.inspect(l,p,pick).isEmpty(),"air");
   var strike=c.strike(pick,0,.001,2,4).orElseThrow();
   require(Math.abs(strike.incidentEnergyPerAreaJm2()-16000)<1e-6&&c.strike(pick,0,0,2,4).isEmpty(),"strike energy and invalid contact");
  }finally{l.setBlock(p,old,2|16);}
 }
}
