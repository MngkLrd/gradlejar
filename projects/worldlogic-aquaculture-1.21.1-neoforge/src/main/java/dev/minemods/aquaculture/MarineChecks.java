package dev.minemods.aquaculture;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import net.minecraft.server.level.ServerLevel;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

final class MarineChecks {
 private static void require(boolean good,String msg){if(!good)throw new IllegalStateException("Marine QA: "+msg);}
 static void run(ServerLevel l){
  Set<String> hashes=new HashSet<>();
  require(Aquaculture.MARINE.size()==8&&MarineGeometry.ALL.size()==8,"eight species and meshes");
  for(MarineSpecies s:MarineSpecies.values()){
   double temp=(s.minTemp+s.maxTemp)/2.0;
   var sea=new Aquaculture.Habitat(WaterKind.SEA,temp,35,.1,s.depth);
   require(s.suitable(sea),"positive habitat "+s.id);
   require(!s.suitable(new Aquaculture.Habitat(WaterKind.LAKE,temp,.2,.1,s.depth)),"freshwater reject "+s.id);
   require(!s.suitable(new Aquaculture.Habitat(WaterKind.SEA,temp,35,.1,s.depth-1)),"depth reject "+s.id);
   require(!s.suitable(new Aquaculture.Habitat(WaterKind.SEA,s.maxTemp+1,35,.1,s.depth)),"thermal reject "+s.id);
   var m=Aquaculture.MARINE.get(s).get().create(l);
   require(m!=null&&m.species()==s&&m.getMaxHealth()==s.hp,"entity and attributes "+s.id);
   m.discard();
   try(var in=MarineChecks.class.getResourceAsStream("/assets/aquaculture/textures/entity/marine/"+s.id+".png")){
    require(in!=null,"missing UV "+s.id);byte[] bytes=in.readAllBytes();
    var d=new DataInputStream(new ByteArrayInputStream(bytes));
    require(d.readLong()==0x89504e470d0a1a0aL&&d.readInt()==13&&d.readInt()==0x49484452&&d.readInt()==128&&d.readInt()==128,"invalid PNG "+s.id);
    require(hashes.add(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))),"duplicate PNG "+s.id);
   }catch(Exception e){throw new IllegalStateException(s.id,e);}
  }
  require(MarineSpecies.BLUE_WHALE.length>MarineSpecies.HUMPBACK_WHALE.length&&MarineSpecies.NARWHAL.airBreather&&!MarineSpecies.ELECTRIC_RAY.airBreather,"anatomy and breathing groups");
  require(MarineRuntime.cell(new net.minecraft.core.BlockPos(-1,60,0))!=MarineRuntime.cell(new net.minecraft.core.BlockPos(0,60,0)),"negative coordinate separation");
 }
}
