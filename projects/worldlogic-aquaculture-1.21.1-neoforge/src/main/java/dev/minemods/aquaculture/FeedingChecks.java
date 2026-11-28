package dev.minemods.aquaculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

final class FeedingChecks {
 private static void require(boolean good,String message){if(!good)throw new IllegalStateException(message);}
 static void run(ServerLevel level){
  BlockPos pos=new BlockPos(4,110,4);
  require(level.hasChunkAt(pos),"feeding fixture not loaded");
  var old=level.getBlockState(pos);
  try {
   level.setBlock(pos,Blocks.WATER.defaultBlockState(),2|16);
   for(Species s:Species.ALL){
    var mob=Aquaculture.TYPES.get(s.id()).get().create(level);
    require(mob instanceof Fish,"missing fish "+s.id());
    Fish fish=(Fish)mob;
    try{
     fish.moveTo(pos.getX()+.5,pos.getY()+.1,pos.getZ()+.5,0,0);
     var item=switch(s.diet()){
      case "predator","invertebrate" -> Items.COD;
      default -> Items.KELP;
     };
     ItemStack food=new ItemStack(item,3),wrong=new ItemStack(Items.STONE,3);
     require(FishFeeding.offer(fish,s,wrong,false)==FishFeeding.Result.WRONG_FOOD&&wrong.getCount()==3,"wrong food "+s.id());
     require(FishFeeding.offer(fish,s,food,false)==FishFeeding.Result.FED&&food.getCount()==2,"first feed "+s.id());
     require(FishFeeding.satiety(fish,level.getGameTime())==50,"satiety "+s.id());
     require(FishFeeding.offer(fish,s,food,false)==FishFeeding.Result.COOLDOWN&&food.getCount()==2,"cooldown "+s.id());
     fish.getPersistentData().putLong("wl_fish_last_feed",level.getGameTime()-100);
     require(FishFeeding.offer(fish,s,food,false)==FishFeeding.Result.FED&&food.getCount()==1,"second feed "+s.id());
     require(FishFeeding.offer(fish,s,food,false)==FishFeeding.Result.FULL&&food.getCount()==1,"full "+s.id());
     require(FishFeeding.satiety(fish,level.getGameTime()+FishFeeding.DIGEST_TICKS)==0,"digestion "+s.id());
     var nbt=new net.minecraft.nbt.CompoundTag();fish.saveWithoutId(nbt);
     var copy=Aquaculture.TYPES.get(s.id()).get().create(level);
     require(copy instanceof Fish,"reload "+s.id());
     try{
      copy.load(nbt);
      require(FishFeeding.satiety((Fish)copy,level.getGameTime())==100,"persistent satiety "+s.id());
     }finally{copy.discard();}
     var traits=Aquaculture.traits(fish,s);
     traits.putInt("wl_fish_age_years",s.maturityYears()-1);
     require(!FishFeeding.mature(fish,s),"immature "+s.id());
     traits.putInt("wl_fish_age_years",s.maturityYears());
     require(FishFeeding.mature(fish,s),"mature "+s.id());
     // Dry rejection uses the same server-side offer path.
     var dry=pos.above(3);
     if(level.getBlockState(dry).isAir()){
      fish.moveTo(dry.getX()+.5,dry.getY()+.1,dry.getZ()+.5,0,0);
      require(FishFeeding.offer(fish,s,food,false)==FishFeeding.Result.DRY&&food.getCount()==1,"dry "+s.id());
     }else throw new IllegalStateException("dry fixture obstructed");
    }finally{fish.discard();}
   }
  }finally{level.setBlock(pos,old,2|16);}
 }
}
