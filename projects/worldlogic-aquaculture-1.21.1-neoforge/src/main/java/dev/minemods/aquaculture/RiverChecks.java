package dev.minemods.aquaculture;
import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import java.util.*;
final class RiverChecks{
 private static void require(boolean good,String msg){if(!good)throw new IllegalStateException("River: "+msg);}
 static void run(ServerLevel l){
  require(WorldLogicApi.API_REVISION>=21,"Core API revision");
  var p=new BlockPos(8,110,8);require(l.hasChunkAt(p),"loaded fixture");
  var river=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogic","river_water")).defaultBlockState();
  IntegerProperty current=(IntegerProperty)river.getProperties().stream().filter(x->x.getName().equals("current")).findFirst().orElseThrow();
  Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
  try{
   for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-4;y<=2;y++){
    BlockPos q=p.offset(x,y,z);saved.put(q,l.getBlockState(q));
    l.setBlock(q,y==-4?Blocks.STONE.defaultBlockState():river.setValue(current,1),2|16);
   }
   var c=RiverBehavior.sample(l,p).orElseThrow();
   require(c.velocityMps().x>0&&Math.abs(c.velocityMps().z)<.001&&c.speedMps()>3,"actual directed Core river");
   var trout=Species.BY_ID.get("trout");var tench=Species.BY_ID.get("tench");
   require(RiverBehavior.weight(trout,Optional.of(c))>0&&RiverBehavior.weight(tench,Optional.of(c))==0,"species current suitability");
   require(RiverBehavior.weight(tench,Optional.empty())==1,"undirected unknown not rejected");
   require(RiverBehavior.intention(c,false).dot(c.velocityMps())<0&&RiverBehavior.intention(c,true).dot(c.velocityMps())>0,"up/downstream intention");
   var h=new Aquaculture.Habitat(WorldLogicApi.WaterKind.RIVER,12,0,.1,8);
   for(int i=0;i<100;i++){var s=SpawnSelection.choose(h,Map.of(),l.random,Optional.of(c));require(s!=null&&RiverBehavior.weight(s,Optional.of(c))>0,"weighted current candidate");}
   Fish fish=(Fish)Aquaculture.TYPES.get("trout").get().create(l);
   try{
    fish.moveTo(p.getX()+.5,p.getY()+.1,p.getZ()+.5,0,0);
    require(FishWater.contact(fish)&&RiverBehavior.step(l,fish,trout),"actual swimming intention");
    require(fish.getPersistentData().getString("wl_fish_activity").startsWith("current_"),"intent state");
   }finally{fish.discard();}
   l.setBlock(p,river.setValue(current,0),2|16);require(RiverBehavior.sample(l,p).isEmpty(),"undirected river unknown");
   l.setBlock(p,Blocks.AIR.defaultBlockState(),2|16);require(RiverBehavior.sample(l,p).isEmpty(),"dry no prescribed current");
  }finally{saved.forEach((q,state)->l.setBlock(q,state,2|16));}
 }
}
