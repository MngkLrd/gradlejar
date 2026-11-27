package dev.minemods.worldlogic;
import java.util.*;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogic.api.WorldLogicApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
final class NaturalWaterChecks{
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("native water: "+s);}
 static void verify(ServerLevel l){
  require(NaturalFluids.families().size()==8,"eight registered families");
  var p=new BlockPos(4,110,4);require(l.hasChunkAt(p),"loaded fixture");
  var original=l.getBlockState(p);var old=new LinkedHashMap<BlockPos,BlockState>();
  try{
   for(var d:net.minecraft.core.Direction.values()){var q=p.relative(d);old.put(q,l.getBlockState(q));l.setBlock(q,Blocks.STONE.defaultBlockState(),2);}
   for(var f:NaturalFluids.families()){
    var s=f.source.get().defaultFluidState();var flowing=f.flowing.get().defaultFluidState();
    require(s.is(FluidTags.WATER)&&flowing.is(FluidTags.WATER)&&s.isSource()&&!flowing.isSource(),"water behavior tags "+f.id);
    require(s.getType()!=Fluids.WATER&&s.getType().getBucket()==f.bucket.get(),"identity and bucket "+f.id);
    l.setBlock(p,f.block.get().defaultBlockState(),2);
    var w=WorldLogicApi.waterProfile(l,p);
    require(w.kind()==f.kind&&w.evidence()==WaterEvidence.REGISTERED_FLUID,"world API identity "+f.id);
    require(WorldLogicApi.waterProfile(f.source.get()).kind()==f.kind,"registry API "+f.id);
    var stack=f.block.get().pickupBlock(null,l,p,l.getBlockState(p));
    require(stack.is(f.bucket.get())&&l.getBlockState(p).isAir(),"actual bucket pickup "+f.id);
    var contents=FluidUtil.getFluidContained(stack);
    require(contents.isPresent()&&contents.get().getFluid()==f.source.get()&&contents.get().getAmount()==1000,"NeoForge container capability "+f.id);
    require(WorldLogicApi.waterProfile(f.flowing.get()).flowing(),"flowing profile "+f.id);
   }
   var pond=NaturalFluids.get(WaterKind.POND);l.setBlock(p,pond.block.get().defaultBlockState(),2);
   var clean=WorldLogicApi.waterProfile(l,p);
   require(clean.quality()==WaterQuality.DRINKABLE,"clean pond");
   for(var d:net.minecraft.core.Direction.values())if(d!=net.minecraft.core.Direction.UP)l.setBlock(p.relative(d),Content.SOILS.get("peat").get().defaultBlockState(),2);
   var dirty=WorldLogicApi.waterProfile(l,p);
   require(dirty.kind()==WaterKind.POND&&dirty.contaminationRisk()>clean.contaminationRisk()&&dirty.quality()==WaterQuality.TREAT_RECOMMENDED,"peat water degradation");
   l.setBlock(p,NaturalFluids.get(WaterKind.SEA).block.get().defaultBlockState(),2);
   require(WorldLogicApi.waterProfile(l,p).quality()==WaterQuality.DESALINATE,"salt cannot become drinkable by soil");
  }finally{l.setBlock(p,original,2);old.forEach((q,s)->l.setBlock(q,s,2));}
  verifyMotion(l);
  verifyDirectedRiver(l);
  var chunk=new ProtoChunk(new ChunkPos(0,0),UpgradeData.EMPTY,LevelHeightAccessor.create(-256,768),l.registryAccess().registryOrThrow(Registries.BIOME),null);
  // Construct an actual vertical underground pool and surface pool. This is a chunk fixture, not a fully generated world.
  var pos=new BlockPos.MutableBlockPos();
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=-256;y<=64;y++){
   pos.set(x,y,z);
   chunk.setBlockState(pos,(y==-256?Blocks.BEDROCK:y>=63||y>=-22&&y<=-20?Blocks.WATER:y==-19?Blocks.CAVE_AIR:Blocks.STONE).defaultBlockState(),false);
  }
  Heightmap.primeHeightmaps(chunk,EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG,Heightmap.Types.WORLD_SURFACE_WG));
  int count=NaturalWaterGeneration.convertChunk(chunk,Optional.empty(),l.getBiome(p));
  require(count==16*16*5,"converted all standalone water "+count);
  require(chunk.getBlockState(new BlockPos(4,-21,4)).is(NaturalFluids.get(WaterKind.GROUNDWATER).block.get()),"underground native fluid");
  require(!chunk.getBlockState(new BlockPos(4,64,4)).is(Blocks.WATER)&&chunk.getFluidState(new BlockPos(4,64,4)).is(FluidTags.WATER),"surface native fluid");
  require(chunk.getBlockState(new BlockPos(4,-256,4)).is(Blocks.BEDROCK),"bedrock unchanged");
  require(NaturalWaterGeneration.convertChunk(chunk,Optional.empty(),l.getBiome(p))==0,"idempotence");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_NATIVE_WATER families=8 bucketPickup=8 capabilities=8 convertedBlocks={} soilQuality=PASS",count);
 }
 private static void verifyMotion(ServerLevel l){
  var origin=new BlockPos(10,110,10);var saved=new LinkedHashMap<BlockPos,BlockState>();
  var pig=net.minecraft.world.entity.EntityType.PIG.create(l);
  require(pig!=null,"motion fixture entity");
  try{
   for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-2;y<=3;y++){
    var p=origin.offset(x,y,z);saved.put(p,l.getBlockState(p));l.setBlock(p,(y==-2||Math.abs(x)==2||Math.abs(z)==2?Blocks.STONE:Blocks.AIR).defaultBlockState(),2|16);
   }
   for(var f:NaturalFluids.families()){
    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=2;y++)l.setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),2|16);
    l.setBlock(origin,f.block.get().defaultBlockState(),2|16);
    require(NativeWaterTicks.needsTick(l,origin),"exposed source rearm "+f.id);
    // Execute the actual Fluid tick, not an emulated movement equation.
    var fs=l.getFluidState(origin);fs.tick(l,origin);
    require(NaturalFluids.kind(l.getFluidState(origin.below()).getType())==f.kind&&!l.getFluidState(origin.below()).isSource(),"downward fluid spread "+f.id);
    for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=2;y++)l.setBlock(origin.offset(x,y,z),f.block.get().defaultBlockState(),2|16);
    pig.setPos(origin.getX()+.5,origin.getY()+.1,origin.getZ()+.5);pig.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);pig.baseTick();
    require(pig.isInWater()&&pig.getFluidHeight(FluidTags.WATER)>.5&&pig.isEyeInFluid(FluidTags.WATER),"entity native immersion "+f.id);
   }
   for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=2;y++)l.setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),2|16);
   pig.baseTick();require(!pig.isInWater()&&!pig.isEyeInFluid(FluidTags.WATER),"dry entity negative control");
  }finally{pig.discard();saved.forEach((p,s)->l.setBlock(p,s,2|16));}
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_NATIVE_WATER_MOTION families=8 verticalSpread=8 immersion=8 dryControl=PASS");
 }

 private static void verifyDirectedRiver(ServerLevel l){
  var p=new BlockPos(4,110,4);var old=l.getBlockState(p);
  try{
   var family=NaturalFluids.get(WaterKind.RIVER);var base=family.block.get().defaultBlockState();
   require(base.getValue(DirectedRiverBlock.CURRENT)==0,"bucket and old-state current default");
   for(int sector=1;sector<=16;sector++){
    var state=base.setValue(DirectedRiverBlock.CURRENT,sector);
    require(net.minecraft.world.level.block.Block.stateById(net.minecraft.world.level.block.Block.getId(state))==state,"current block palette identity");
    l.setBlock(p,state,2|16);
    var f=l.getFluidState(p);var vector=f.getFlow(l,p);var expected=DirectedRiverBlock.vector(sector);
    require(f.isSource()&&NaturalFluids.kind(f.getType())==WaterKind.RIVER&&vector.distanceTo(expected)<1e-8,"stored current vector "+sector);
    require(DirectedRiverBlock.encode(expected.x,expected.z)==sector,"direction quantization");
   }
  }finally{l.setBlock(p,old,2|16);}
 }

}
