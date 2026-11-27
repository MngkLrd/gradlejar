package dev.minemods.thermal.server;
import dev.minemods.thermal.*;
import dev.minemods.thermal.content.ThermalContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.*;
final class ClimateEvolutionChecks {
 private static void require(boolean value,String detail){if(!value){org.slf4j.LoggerFactory.getLogger("Thermal evolution QA").error(detail);throw new IllegalStateException(detail);}}
 private static ThermalModel.Exposure exp(double air,double cold,double heat,double nutrition,double hydration){
  return new ThermalModel.Exposure(air,0,0,0,0,0,false,air,0,0,0,cold,heat,nutrition,hydration,0);
 }
 static void adaptation(ServerLevel l,ServerPlayer p){
  var c=new Adaptation.State(0,0);for(int i=0;i<4800;i++)c=Adaptation.step(c,5,37);
  require(c.cold()>.6&&c.heat()==0,"moderate cold builds cold adaptation alone");
  var h=c;for(int i=0;i<4800;i++)h=Adaptation.step(h,34,37);
  require(h.heat()>.6&&h.cold()<c.cold(),"hot climate builds heat adaptation while cold fades");
  require(Adaptation.step(new Adaptation.State(0,0),-30,34).cold()==0,"dangerous cold cannot train");
  var initial=ThermalModel.initial();
  require(ThermalModel.tick(initial,exp(0,1,0,1,1),1).targetC()>ThermalModel.tick(initial,exp(0,0,0,1,1),1).targetC(),"cold adaptation changes model");
  require(ThermalModel.tick(initial,exp(39,0,1,1,1),1).targetC()<ThermalModel.tick(initial,exp(39,0,0,1,1),1).targetC(),"heat adaptation changes model");
  var n=new net.minecraft.nbt.CompoundTag();n.putDouble("adaptCold",.5);n.putDouble("adaptHeat",.4);p.getPersistentData().put(ThermalEvents.KEY,n);
  ThermalEvents.update(p);var updated=p.getPersistentData().getCompound(ThermalEvents.KEY);
  require(updated.getDouble("adaptCold")>.49&&updated.getDouble("adaptHeat")>.39,"live update retains acclimation");
 }
 static void frostbite(ServerLevel l,ServerPlayer p){
  double naked=0,clothed=0,wet=0;
  for(int i=0;i<90;i++){naked=Adaptation.frostDose(naked,-15,0,0);clothed=Adaptation.frostDose(clothed,-15,1,0);wet=Adaptation.frostDose(wet,-15,1,1);}
  require(naked>=120&&clothed==0&&wet>clothed,"local insulation and wetness affect frost dose");
  require(Adaptation.frostDose(naked,20,0,0)<naked,"warming reduces exposure before tissue injury");
  var n=new net.minecraft.nbt.CompoundTag();
  var e=exp(-30,0,0,1,1);var s=ThermalModel.tick(ThermalModel.initial(),e,1);
  LimbExposure.update(p,n,e,s);
  for(String limb:LimbExposure.LIMBS)require(n.contains("frost_"+limb),"separate limb accumulation "+limb);
 }
 static void nutrition(ServerLevel l,ServerPlayer p){
  require(Adaptation.nutrition(20,5)==1&&Adaptation.nutrition(0,0)==0,"nutrition bounds");
  var a=ThermalModel.initial();
  require(ThermalModel.tick(a,exp(0,0,0,0,1),1).targetC()<ThermalModel.tick(a,exp(0,0,0,1,1),1).targetC(),"hunger worsens cold");
  require(ThermalModel.tick(a,exp(40,0,0,1,.1),1).targetC()>ThermalModel.tick(a,exp(40,0,0,1,1),1).targetC(),"dehydration worsens heat");
  int old=p.getFoodData().getFoodLevel();float sat=p.getFoodData().getSaturationLevel();
  try{p.getFoodData().setFoodLevel(1);p.getFoodData().setSaturation(0);
   require(EnvironmentSampler.sample(p,0).orElseThrow().exposure().nutrition()<.1,"live hunger affects exposure");
  }finally{p.getFoodData().setFoodLevel(old);p.getFoodData().setSaturation(sat);}
 }
 static void room(ServerLevel l,ServerPlayer p){
  var center=new BlockPos(8,108,8);Map<BlockPos,BlockState> old=new HashMap<>();
  try{
   for(BlockPos b:BlockPos.betweenClosed(center.offset(-2,-2,-2),center.offset(2,2,2))){
    BlockPos q=b.immutable();old.put(q,l.getBlockState(q));
    boolean edge=Math.abs(q.getX()-center.getX())==2||Math.abs(q.getY()-center.getY())==2||Math.abs(q.getZ()-center.getZ())==2;
    l.setBlockAndUpdate(q,edge?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
   }
   long now=l.getGameTime();
   double initial=RoomHeat.sampleAt(l,center,30,0,true,now);
   double warmed=RoomHeat.sampleAt(l,center,30,0,true,now+1200);
   double retained=RoomHeat.sampleAt(l,center,0,0,true,now+1400);
   require(warmed>initial&&retained>0&&retained<warmed,"enclosed room warms and retains residual heat");
   l.setBlockAndUpdate(center.above(2),Blocks.AIR.defaultBlockState());
   require(RoomHeat.sampleAt(l,center,30,0,true,now+1600)==0,"open roof prevents room heat bonus");
  }finally{old.forEach(l::setBlockAndUpdate);}
 }
 static void settlement(ServerLevel l,ServerPlayer p){
  require(SettlementClimate.choose(-5)==1&&SettlementClimate.choose(35)==-1&&SettlementClimate.choose(18)==0,"climate selects heater/cooler/no fixture");
  var bed=new BlockPos(8,116,8);var bounds=new BoundingBox(5,115,5,11,119,11);Map<BlockPos,BlockState> old=new HashMap<>();
  try{
   for(BlockPos b:BlockPos.betweenClosed(new BlockPos(5,115,5),new BlockPos(11,119,11))){
    BlockPos q=b.immutable();old.put(q,l.getBlockState(q));
    boolean edge=q.getY()==115||q.getY()==119||q.getX()==5||q.getX()==11||q.getZ()==5||q.getZ()==11;
    l.setBlockAndUpdate(q,edge?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
   }
   l.setBlockAndUpdate(bed,Blocks.WHITE_WOOL.defaultBlockState());
   var chunk=l.getChunkSource().getChunkNow(0,0);
   require(chunk!=null&&SettlementClimate.install(l,chunk,bed,bounds,1),"cold bedroom gets existing hearth");
   require(!SettlementClimate.install(l,chunk,bed,bounds,1),"duplicate heater rejected");
   require(l.getBlockState(bed).is(Blocks.WHITE_WOOL),"bed/occupied position unchanged");
   for(BlockPos q:old.keySet())if(l.getBlockState(q).is(ThermalContent.HEARTH.get()))l.setBlockAndUpdate(q,Blocks.AIR.defaultBlockState());
   require(SettlementClimate.install(l,chunk,bed,bounds,-1),"hot bedroom gets existing cooler");
  }finally{old.forEach(l::setBlockAndUpdate);}
 }
}
