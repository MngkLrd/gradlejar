package dev.minemods.aquaculture;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

final class CoastBiteChecks{
 private static void require(boolean good,String msg){if(!good)throw new IllegalStateException("Coast/bite: "+msg);}
 private static Aquaculture.Habitat habitat(WaterKind k,double t,double salt,int depth){return new Aquaculture.Habitat(k,t,salt,.1,depth);}
 private static long count(Aquaculture.Habitat h){return Species.ALL.stream().filter(s->CoastalHabitat.eligible(s,h)).count();}
 private static AABB box(Vec3 p){return new AABB(p.x-.15,p.y-.15,p.z-.15,p.x+.15,p.y+.15,p.z+.15);}
 static void run(ServerLevel l){
  for(var h:List.of(habitat(WaterKind.RIVER,5,.2,2),habitat(WaterKind.LAKE,15,.2,2),habitat(WaterKind.WETLAND,20,.2,2),habitat(WaterKind.POND,28,.2,2),habitat(WaterKind.SEA,4,35,2),habitat(WaterKind.SEA,25,35,2),habitat(WaterKind.BRACKISH,15,12,2)))require(count(h)>=2,"coastal diversity "+h);
  require(count(habitat(WaterKind.GROUNDWATER,15,.2,2))==0&&count(habitat(WaterKind.SEA,25,35,12))==0,"no groundwater/deep shoreline");
  var p=new BlockPos(8,110,8);require(l.hasChunkAt(p),"loaded fixture");
  Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
  try{
   var water=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogic","lake_water")).defaultBlockState();
   for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-2;y<=3;y++){
    BlockPos q=p.offset(x,y,z);saved.put(q,l.getBlockState(q));
    l.setBlock(q,y==-2||Math.abs(x)==3||Math.abs(z)==3?Blocks.STONE.defaultBlockState():y<=0?water:Blocks.AIR.defaultBlockState(),2|16);
   }
   require(CoastalHabitat.nearBank(l,p),"actual shallow bank");
   require(!CoastalHabitat.nearBank(l,p.above()),"dry reject");
   var s=Species.BY_ID.get("perch");var a=PopulationData.area(p,WaterKind.LAKE);var d=new PopulationData();
   require(CoastalHabitat.canFound(d,a,s),"unseeded shore");
   d.markSeeded(a,s);require(!CoastalHabitat.canFound(d,a,s),"extinct not resurrected");
   UUID id=UUID.randomUUID();d.observe(id,a,s);require(CoastalHabitat.canFound(d,a,s),"existing viable population can seed shore");
   d.markShoreSeeded(a,s);d=PopulationData.load(d.save(new CompoundTag(),l.registryAccess()),l.registryAccess());require(d.shoreSeeded(a,s)&&!CoastalHabitat.canFound(d,a,s),"persisted shore origin");
  }finally{saved.forEach((q,b)->l.setBlock(q,b,2|16));}
  Vec3 center=new Vec3(0,1,0);
  for(var s:List.of(MarineSpecies.GREAT_WHITE_SHARK,MarineSpecies.TIGER_SHARK)){
   double reach=SharkBite.reach(s);require(reach>1&&reach<s.length,"mesh mouth offset "+s.id);
   for(float yaw:new float[]{0,90,180,-90}){
    Vec3 forward=SharkBite.forward(yaw),mouth=SharkBite.mouth(s,center,yaw);
    require(SharkBite.contact(s,center,yaw,box(mouth)),"frontal contact "+s.id+" "+yaw);
    require(!SharkBite.contact(s,center,yaw,box(center.subtract(forward.scale(reach)))),"rear rejected");
    require(!SharkBite.contact(s,center,yaw,box(center.add(new Vec3(forward.z,0,-forward.x).scale(reach)))),"flank rejected");
    require(!SharkBite.contact(s,center,yaw,box(mouth.add(forward.scale(2)))),"out of reach rejected");
   }
  }
 }
}
