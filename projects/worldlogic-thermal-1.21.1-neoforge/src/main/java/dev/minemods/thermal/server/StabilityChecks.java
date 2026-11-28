package dev.minemods.thermal.server;
import dev.minemods.thermal.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

final class StabilityChecks {
 private static void require(boolean ok,String name){if(!ok)throw new IllegalStateException("Stability QA: "+name);}
 private static ThermalModel.State model(double water,double heat,double cooling,double wind,boolean submerged,double room){
   return ThermalModel.tick(ThermalModel.initial(),new ThermalModel.Exposure(0,heat,wind,submerged?1:0,0,0,submerged,water,0,cooling,0,0,0,1,1,room),1);
 }
 static void verify(ServerLevel l,ServerPlayer p,String name){
   switch(name){
    case "water_exposure" -> {
      double sea=ClimateModel.naturalWater(35,15,"SEA",0),pond=ClimateModel.naturalWater(35,15,"POND",0);
      require(pond>sea,"shallow pond responds faster to warm weather");
      require(ClimateModel.naturalWater(35,15,"SEA",30)<sea,"depth reduces warm marine surface temperature");
      require(ClimateModel.naturalWater(-30,-15,"SEA",0)<0&&ClimateModel.naturalWater(-30,-15,"RIVER",0)>=0,"saltwater lower freezing floor");
      require(ClimateModel.naturalWater(5,15,"GROUNDWATER",0)>ClimateModel.naturalWater(-15,15,"POND",0),"groundwater buffered from cold");
      var plain=model(12,0,0,0,true,0);
      require(model(12,20,0,0,true,0).feelsLikeC()>plain.feelsLikeC(),"immersed radiant heating");
      require(model(12,0,8,0,true,0).feelsLikeC()<plain.feelsLikeC(),"immersed local cooling");
      require(model(12,0,0,1,true,0).feelsLikeC()==plain.feelsLikeC(),"no duplicate evaporative wind chill underwater");
      BlockPos at=p.blockPosition();var old=l.getBlockState(at);var above=l.getBlockState(at.above());
      try{
        l.setBlock(at,Blocks.WATER.defaultBlockState(),2);l.setBlock(at.above(),Blocks.WATER.defaultBlockState(),2);
        var w=WaterExposure.sample(l,at,15);
        require(w.depth()>=1&&Double.isFinite(w.celsius()),"actual loaded water column");
        var sample=EnvironmentSampler.sample(p,0).orElseThrow();
        require(sample.exposure().submerged()&&Math.abs(sample.exposure().waterC()-w.celsius())<.01&&sample.skinWetness()==1,"real water contact and body input");
      }finally{l.setBlock(at,old,2);l.setBlock(at.above(),above,2);}
    }
    case "room_response" -> {
      var heat=new RoomPhysics.Inputs(0,50,.02,.5,1.5,27,240,0,0);
      var warm=RoomPhysics.step(RoomPhysics.initial(0,50),heat,300);
      require(warm.air()>14,"heating not pinned to old fourteen degree ceiling");
      var cold=new RoomPhysics.Inputs(35,20,.02,.5,1.5,27,0,100,0);
      require(RoomPhysics.step(RoomPhysics.initial(35,20),cold,300).air()<27,"cooling not pinned to old eight degree floor");
      var changed=RoomPhysics.step(warm,new RoomPhysics.Inputs(-30,50,.02,.5,1.5,27,0,0,0),1);
      require(Math.abs(warm.air()-changed.air())<5,"room thermal inertia survives weather shock");
      require(model(0,0,0,0,false,30).feelsLikeC()==30&&model(0,0,0,0,false,-20).feelsLikeC()==-20,"body uses full room-air differential");
      ThermalEvents.update(p);
      var n=p.getPersistentData().getCompound(ThermalEvents.KEY);
      require(dev.minemods.thermal.api.ThermalApi.status(p).isPresent(),"fresh status visible");
      n.putLong("sampleTick",l.getGameTime()-41);
      require(dev.minemods.thermal.api.ThermalApi.status(p).isEmpty(),"expired status rejected");
    }
    case "hud_signals" -> {
      require(HudSignals.jump(20,25)==5&&HudSignals.jump(20,15)==-5&&HudSignals.jump(20,21)==0,"signed sudden change threshold");
      require(HudSignals.trend(.6).equals("↑↑")&&HudSignals.trend(-.6).equals("↓↓")&&HudSignals.trend(0).equals("="),"strong trend distinguishable");
      require(HudSignals.bodyPosition(30)==0&&HudSignals.bodyPosition(43)==1&&HudSignals.bodyPosition(37)>.5,"continuous body marker");
      for(int width:new int[]{160,320,480,960}){
        var r=dev.minemods.thermal.client.ThermalLayout.hud(width,180);
        require(r.x()>=0&&r.y()>=0&&r.x()+r.w()<=width&&r.y()+r.h()<=180,"HUD in viewport");
      }
    }
    default -> throw new IllegalArgumentException(name);
   }
 }
}
