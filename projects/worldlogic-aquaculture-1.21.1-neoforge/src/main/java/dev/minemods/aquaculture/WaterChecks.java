package dev.minemods.aquaculture;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

final class WaterChecks {
 private static void require(boolean good,String message){if(!good)throw new IllegalStateException(message);}
 static void run(ServerLevel l){
  var center=new BlockPos(8,110,8);
  require(l.hasChunkAt(center),"water fixture not loaded");
  var saved=new LinkedHashMap<BlockPos,BlockState>();
  var types=List.of(EntityType.COD,EntityType.SALMON,Aquaculture.TYPES.get("perch").get());
  var ids=List.of("sea_water","river_water","lake_water","spring_water","groundwater","wetland_water","pond_water","brackish_water","water");
  try{
   for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-2;y<=7;y++){
    var pos=center.offset(x,y,z);
    saved.put(pos,l.getBlockState(pos));
    l.setBlock(pos,(Math.abs(x)==2||Math.abs(z)==2||y==-2?Blocks.STONE:Blocks.AIR).defaultBlockState(),2|16);
   }
   for(String id:ids){
    var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(id.equals("water")?"minecraft":"worldlogic",id));
    require(block instanceof LiquidBlock,"missing water "+id);
    for(boolean flowing:new boolean[]{false,true}){
     BlockState state=block.defaultBlockState().setValue(LiquidBlock.LEVEL,flowing?1:0);
     for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=4;y++)l.setBlock(center.offset(x,y,z),state,2|16);
     for(var type:types){
      var entity=type.create(l);
      require(entity instanceof AbstractFish,"fish fixture "+type);
      var fish=(AbstractFish)entity;
      try{
       fish.moveTo(center.getX()+.5,center.getY()+.05,center.getZ()+.5,0,0);
       fish.setAirSupply(-19);
       float health=fish.getHealth();
       require(FishWater.contact(fish)&&fish.isInWater()&&fish.isInWaterOrBubble(),"immersion "+id+" flowing="+flowing+" "+type+" contact="+FishWater.contact(fish)+" vanillaWet="+fish.isInWater()+" bubble="+fish.isInWaterOrBubble());
       for(int n=0;n<25;n++)fish.baseTick();
       require(fish.getHealth()==health&&fish.getAirSupply()>0,"wet survival "+id+" flowing="+flowing+" "+type+" health="+fish.getHealth()+" initialHealth="+health+" air="+fish.getAirSupply());
       fish.moveTo(center.getX()+.5,center.getY()+6,center.getZ()+.5,0,0);
       fish.setAirSupply(5);
       fish.baseTick();
       require(fish.getAirSupply()==4&&fish.getHealth()==health,"dry countdown "+id+" "+type+" air="+fish.getAirSupply());
       fish.setAirSupply(-19);
       fish.baseTick();
       require(fish.getHealth()<health&&fish.getAirSupply()==0,"dry suffocation damage "+id+" "+type+" health="+fish.getHealth()+" air="+fish.getAirSupply());
       for(int n=1;n<25;n++)fish.baseTick();
       require(!FishWater.contact(fish)&&!fish.isInWater()&&fish.getAirSupply()<=0,"dry breathing control "+id+" flowing="+flowing+" "+type+" contact="+FishWater.contact(fish)+" vanillaWet="+fish.isInWater()+" air="+fish.getAirSupply());
      }finally{fish.discard();}
     }
    }
    l.setBlock(center.above(5),Blocks.ICE.defaultBlockState(),2|16);
    require(Aquaculture.findWaterColumn(l,center.getX(),center.getZ(),center.getY()+6)!=null,"water below ice "+id);
    l.setBlock(center.above(5),Blocks.AIR.defaultBlockState(),2|16);
    if(!id.equals("water")){
     var h=Aquaculture.habitat(l,center);
     require(h!=null&&h.kind()!=WaterKind.NONE&&h.kind()!=WaterKind.UNKNOWN,"Core profile "+id);
     if(h.kind()==WaterKind.GROUNDWATER)require(Aquaculture.choose(h,l.random)==null,"groundwater no spawn");
     else{
      // Controlled temperature/risk for habitat positive control; actual registered fluid and physical spawn.
      Species s=Species.ALL.stream().filter(v->v.suitable(h.kind(),(v.minTemp()+v.maxTemp())/2,h.salt(),.1,h.depth())).findFirst().orElseThrow(()->new IllegalStateException("no suitable species for "+id));
      var suitable=new Aquaculture.Habitat(h.kind(),(s.minTemp()+s.maxTemp())/2,h.salt(),.1,h.depth());
      var f=Aquaculture.spawn(l,center,s,suitable);
      require(f!=null&&f.isAlive()&&FishWater.contact(f),"actual spawn "+id);
      f.discard();
     }
    }
   }
   for(int y=-1;y<=4;y++)l.setBlock(center.above(y),Blocks.AIR.defaultBlockState(),2|16);
   require(!Aquaculture.water(l,center)&&Aquaculture.findWaterColumn(l,center.getX(),center.getZ(),center.getY()+6)==null,"dry column");
  }finally{saved.forEach((pos,state)->l.setBlock(pos,state,2|16));}
 }
}
