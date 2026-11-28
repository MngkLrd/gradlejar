package dev.minemods.aquaculture;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Two commands separated by real server ticks, no player or manual ticking involved. */
final class PopulationTickChecks {
 private record Fixture(ServerLevel level,BlockPos center,Map<BlockPos,BlockState> saved,UUID wet,UUID dry,String species){}
 private static Fixture fixture;
 private static void require(boolean b,String m){if(!b)throw new IllegalStateException("Population ticking: "+m);}
 static void prepare(ServerLevel l){
  require(fixture==null&&l.players().isEmpty(),"empty server required");
  BlockPos center=new BlockPos(8,110,8);
  require(l.hasChunkAt(center)&&l.isPositionEntityTicking(center),"force-loaded fixture not ticking");
  Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
  RoeClutch wet=null,dry=null;
  try{
   var sea=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogic","sea_water")).defaultBlockState();
   for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-2;y<=6;y++){
    BlockPos p=center.offset(x,y,z);saved.put(p,l.getBlockState(p));
    l.setBlock(p,(y==-2||Math.abs(x)==3||Math.abs(z)==3)?Blocks.STONE.defaultBlockState():y<=4?sea:Blocks.AIR.defaultBlockState(),2|16);
   }
   var h=Aquaculture.habitat(l,center);
   require(h!=null&&h.kind()==WaterKind.SEA,"Core sea water fixture");
   var area=PopulationData.area(center,h.kind());var pop=PopulationData.get(l);
   Species s=Species.ALL.stream().filter(v->v.nursery().equals("open_water")&&FishLife.nursery(l,center,v)&&pop.room(area,v)>=2).findFirst().orElseThrow(()->new IllegalStateException("No suitable native seawater nursery"));
   wet=make(l,center,s,false);dry=make(l,center.above(5),s,true);
   require(pop.reserve(wet.getUUID(),area,s,1)&&pop.reserve(dry.getUUID(),area,s,1),"reserve actual clutches");
   require(l.addFreshEntity(wet)&&l.addFreshEntity(dry),"add actual clutches");
   fixture=new Fixture(l,center,saved,wet.getUUID(),dry.getUUID(),s.id());
  }catch(RuntimeException ex){if(wet!=null)wet.discard();if(dry!=null)dry.discard();saved.forEach((p,b)->l.setBlock(p,b,2|16));throw ex;}
 }
 private static RoeClutch make(ServerLevel l,BlockPos p,Species s,boolean dry){
  RoeClutch roe=Aquaculture.ROE.get().create(l);
  if(roe==null)throw new IllegalStateException("clutch entity missing");
  roe.moveTo(p.getX()+.5,p.getY()+.25,p.getZ()+.5,0,0);
  roe.initialize(s,WaterKind.SEA,1);
  CompoundTag n=new CompoundTag();roe.addAdditionalSaveData(n);
  if(dry)n.putInt("bad",2300);else n.putLong("progress",roe.duration()-100);
  roe.readAdditionalSaveData(n);
  return roe;
 }
 static void verify(ServerLevel l){
  Fixture f=fixture;require(f!=null&&f.level==l&&l.players().isEmpty(),"same empty server");
  try{
   require(l.getEntity(f.wet)==null,"wet clutch hatched");
   require(l.getEntity(f.dry)==null,"dry clutch died");
   var babies=l.getEntitiesOfClass(Fish.class,new AABB(f.center).inflate(8),v->v.getPersistentData().hasUUID("wl_roe_origin")&&v.getPersistentData().getUUID("wl_roe_origin").equals(f.wet));
   require(babies.size()==1,"exactly one real hatchling, got "+babies.size());
   Fish baby=babies.getFirst();require(baby.isAlive()&&Aquaculture.species(baby).id().equals(f.species)&&baby.getPersistentData().getBoolean("wl_juvenile"),"juvenile identity");
   baby.discard();
  }finally{
   for(UUID id:List.of(f.wet,f.dry)){var e=l.getEntity(id);if(e!=null)e.discard();}
   f.saved.forEach((p,b)->l.setBlock(p,b,2|16));fixture=null;
  }
 }
}
