package dev.minemods.aquaculture;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;

final class EcologyChecks {
 private static void require(boolean b,String m){if(!b)throw new IllegalStateException("Aqua ecology: "+m);}
 private static Fish create(ServerLevel l,String id,BlockPos p,double length){
  Species s=Species.BY_ID.get(id);var e=Aquaculture.TYPES.get(id).get().create(l);
  require(e instanceof Fish,"fixture "+id);Fish f=(Fish)e;
  f.moveTo(p.getX()+.5,p.getY()+.05,p.getZ()+.5,0,0);
  Aquaculture.traits(f,s).putDouble("wl_fish_length_m",length);Aquaculture.traits(f,s);
  return f;
 }
 static void run(ServerLevel l){
  BlockPos p=new BlockPos(8,110,8);long now=l.getGameTime();
  require(l.hasChunkAt(p),"loaded fixture");
  var data=new AquaEcologyData();var food=AquaEcologyData.Food.BENTHOS;
  for(int i=0;i<AquaEcologyData.FOOD_MAX;i++)require(data.takeFood(p,WaterKind.LAKE,food,now),"food stock "+i);
  require(!data.takeFood(p,WaterKind.LAKE,food,now),"depletion");
  require(data.foodAvailable(p,WaterKind.LAKE,food,now+AquaEcologyData.FORAGE_REGEN)==1,"food renewal");
  require(data.foodAvailable(p.offset(64,0,0),WaterKind.LAKE,food,now)==AquaEcologyData.FOOD_MAX,"cell isolation");
  require(data.foodAvailable(p,WaterKind.SEA,food,now)==AquaEcologyData.FOOD_MAX,"water-kind isolation");
  var s=Species.BY_ID.get("roach");
  for(int i=0;i<SpawnSelection.speciesCap(s)+2;i++)data.harvest(p,WaterKind.LAKE,s,now);
  require(data.pressure(p,WaterKind.LAKE,s.id(),now)==SpawnSelection.speciesCap(s),"harvest cap");
  require(data.pressure(p,WaterKind.LAKE,s.id(),now+AquaEcologyData.PRESSURE_REGEN)==SpawnSelection.speciesCap(s)-1,"harvest recovery");
  var copy=AquaEcologyData.load(data.save(new CompoundTag(),l.registryAccess()),l.registryAccess());
  require(copy.pressure(p,WaterKind.LAKE,s.id(),now)==SpawnSelection.speciesCap(s)&&copy.foodAvailable(p,WaterKind.LAKE,food,now)==0,"persistent state");
  var habitat=new Aquaculture.Habitat(WaterKind.LAKE,15,.2,.1,4);
  require(!SpawnSelection.probabilities(habitat,copy.pressures(p,WaterKind.LAKE,now)).containsKey(s.id()),"depleted species spawn selection");
  require(FishEcology.habitatStress(s,habitat)==0&&FishEcology.habitatStress(s,new Aquaculture.Habitat(WaterKind.SEA,30,35,.3,4))>.5,"habitat stress");
  Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
  Fish predator=null,prey=null,other=null,loach=null;ItemEntity bait=null,wrong=null;
  try{
   for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-2;y<=3;y++){
    BlockPos q=p.offset(x,y,z);saved.put(q,l.getBlockState(q));
    l.setBlock(q,(y==-2||Math.abs(x)==3||Math.abs(z)==3?Blocks.STONE:y==3?Blocks.AIR:Blocks.WATER).defaultBlockState(),2|16);
   }
   predator=create(l,"pike",p.offset(-1,0,0),1.0);
   prey=create(l,"roach",p,.23);
   other=create(l,"roach",p.offset(2,0,0),.24);
   loach=create(l,"loach",p.below(),.2);
   require(FishEcology.canHunt(predator,Species.BY_ID.get("pike"),prey,s),"predator prey");
   require(!FishEcology.canHunt(prey,s,predator,Species.BY_ID.get("pike"))&&!FishEcology.canHunt(predator,Species.BY_ID.get("pike"),predator,Species.BY_ID.get("pike")),"negative hunt");
   require(FishEcology.schoolTarget(other,s,List.of(prey,predator))!=null,"same-species school");
   require(FishEcology.schoolTarget(predator,Species.BY_ID.get("pike"),List.of(prey,other))==null,"solitary hunter");
   Vec3 from=prey.position(),to=other.position();
   require(FishEcology.clearWaterPath(l,from,to),"water route");
   l.setBlock(p.east(),Blocks.STONE.defaultBlockState(),2|16);
   require(!FishEcology.clearWaterPath(l,from,to),"solid barrier");
   l.setBlock(p.east(),Blocks.WATER.defaultBlockState(),2|16);
   bait=new ItemEntity(l,prey.getX(),prey.getY(),prey.getZ(),new ItemStack(Items.KELP,2));
   wrong=new ItemEntity(l,prey.getX(),prey.getY(),prey.getZ(),new ItemStack(Items.STONE,2));
   require(!FishEcology.feedDropped(prey,s,wrong)&&wrong.getItem().getCount()==2,"wrong bait");
   require(FishEcology.feedDropped(prey,s,bait)&&bait.getItem().getCount()==1&&FishFeeding.satiety(prey,now)==50,"real bait consumption");
   require(!FishEcology.feedDropped(prey,s,bait)&&bait.getItem().getCount()==1,"bait cooldown");
   var fresh=new AquaEcologyData();
   require(FishEcology.forage(l,loach,Species.BY_ID.get("loach"),fresh,WaterKind.LAKE),"benthic forage");
   require(fresh.foodAvailable(p,WaterKind.LAKE,food,now)==AquaEcologyData.FOOD_MAX-1&&FishFeeding.satiety(loach,now)==50,"forage stock and satiety");
   require(!FishEcology.forage(l,predator,Species.BY_ID.get("pike"),fresh,WaterKind.LAKE),"no fictitious predator food");
  }finally{
   if(predator!=null)predator.discard();if(prey!=null)prey.discard();if(other!=null)other.discard();if(loach!=null)loach.discard();
   if(bait!=null)bait.discard();if(wrong!=null)wrong.discard();
   saved.forEach((q,b)->l.setBlock(q,b,2|16));
  }
 }
}
