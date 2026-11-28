package dev.minemods.aquaculture;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

final class SpawnChecks {
 private static void require(boolean good,String message){if(!good)throw new IllegalStateException(message);}
 private record Water(WaterKind kind,String id,double salt,double risk){}
 private static final List<Water> WATERS=List.of(
  new Water(WaterKind.SEA,"sea_water",35,.30),new Water(WaterKind.BRACKISH,"brackish_water",12,.30),
  new Water(WaterKind.RIVER,"river_water",.2,.12),new Water(WaterKind.LAKE,"lake_water",.2,.32),
  new Water(WaterKind.SPRING,"spring_water",.2,.05),new Water(WaterKind.WETLAND,"wetland_water",.2,.70),
  new Water(WaterKind.POND,"pond_water",.2,.48),new Water(WaterKind.GROUNDWATER,"groundwater",.2,.23));
 static void run(ServerLevel l){
  Map<String,Double> best=new HashMap<>();
  Map<String,Aquaculture.Habitat> habitats=new HashMap<>();
  for(var w:WATERS)for(int temp=-2;temp<=36;temp++)for(int depth=1;depth<=8;depth++){
   var h=new Aquaculture.Habitat(w.kind,temp,w.salt,w.risk,depth);
   var p=SpawnSelection.probabilities(h,Map.of());
   if(w.kind==WaterKind.GROUNDWATER)require(p.isEmpty(),"groundwater");
   if(!p.isEmpty())require(Math.abs(p.values().stream().mapToDouble(Double::doubleValue).sum()-1)<1e-8,"weight sum");
   for(var e:p.entrySet())if(e.getValue()>best.getOrDefault(e.getKey(),0.0)){best.put(e.getKey(),e.getValue());habitats.put(e.getKey(),h);}
  }
  require(best.size()==36,"unreachable species "+best.keySet());
  RandomSource rng=RandomSource.create(385027L);
  var pos=new BlockPos(8,110,8);
  require(l.hasChunkAt(pos),"loaded spawn fixture");
  Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
  try{
   for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-2;y<=7;y++){
    var q=pos.offset(x,y,z);saved.put(q,l.getBlockState(q));
    l.setBlock(q,(Math.abs(x)==2||Math.abs(z)==2||y==-2?Blocks.STONE:Blocks.AIR).defaultBlockState(),2|16);
   }
   for(Species s:Species.ALL){
    var ideal=habitats.get(s.id());
    require(best.get(s.id())>=.02,"practically rare "+s.id());
    var saturated=Map.of(s.id(),SpawnSelection.speciesCap(s));
    require(!SpawnSelection.probabilities(ideal,saturated).containsKey(s.id()),"saturated "+s.id());
    int hits=0;
    for(int i=0;i<2000;i++){
     var selected=SpawnSelection.choose(ideal,Map.of(),rng);
     require(selected!=null&&selected.suitable(ideal.kind(),ideal.temp(),ideal.salt(),ideal.risk(),ideal.depth()),"unsuitable draw");
     if(selected==s)hits++;
    }
    require(hits>0,"not selected "+s.id());
    var water=WATERS.stream().filter(w->w.kind==ideal.kind()).findFirst().orElseThrow();
    var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogic",water.id));
    require(block instanceof LiquidBlock,"missing "+water.id);
    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=4;y++)l.setBlock(pos.offset(x,y,z),block.defaultBlockState(),2|16);
    var actual=Aquaculture.habitat(l,pos);
    require(actual!=null&&actual.kind()==ideal.kind(),"registered water "+s.id());
    var controlled=new Aquaculture.Habitat(actual.kind(),ideal.temp(),actual.salt(),actual.risk(),actual.depth());
    require(s.suitable(controlled.kind(),controlled.temp(),controlled.salt(),controlled.risk(),controlled.depth()),"real native water eligibility "+s.id());
    var f=Aquaculture.spawn(l,pos,s,controlled);
    require(f!=null&&f.isAlive()&&FishWater.contact(f),"real spawn "+s.id());
    f.discard();
   }
   var peat=new Aquaculture.Habitat(WaterKind.WETLAND,15,.2,1.0,3);
   require(SpawnSelection.probabilities(peat,Map.of()).containsKey("loach"),"natural peat refuge");
   require(!SpawnSelection.probabilities(peat,Map.of()).containsKey("trout"),"wetland rejection");
  }finally{saved.forEach((q,b)->l.setBlock(q,b,2|16));}
 }
}
