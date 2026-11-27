package dev.minemods.hydration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import java.util.Set;

/** RCON-only fixture: actual block entities and the exact periodic transfer implementation. Not player packet evidence. */
public final class InfrastructureChecks {
 private static void require(boolean c,String message){if(!c)throw new IllegalStateException(message);}
 public static void run(ServerLevel l){
  BlockPos from=new BlockPos(8,210,8),pipe=from.east(),down=pipe.east(),to=down.below(),same=down.east();
  BlockPos intake=new BlockPos(12,210,8),collector=intake.east();
  BlockPos[] fixture={from,pipe,down,to,same,pipe.above(),intake,intake.above(),collector,collector.above()};
  for(var p:fixture)require(l.hasChunkAt(p)&&l.getBlockState(p).isAir(),"fixture must be loaded and empty "+p);
  try{
   l.setBlock(from,WaterInfrastructure.BARREL.get().defaultBlockState(),Block.UPDATE_ALL);
   l.setBlock(to,WaterInfrastructure.FILTER.get().defaultBlockState(),Block.UPDATE_ALL);
   l.setBlock(pipe,WaterInfrastructure.PIPE.get().defaultBlockState().setValue(ClayPipeBlock.OUT,Direction.EAST).setValue(ClayPipeBlock.IN,Direction.WEST),Block.UPDATE_ALL);
   l.setBlock(down,WaterInfrastructure.PIPE.get().defaultBlockState().setValue(ClayPipeBlock.OUT,Direction.DOWN).setValue(ClayPipeBlock.IN,Direction.WEST),Block.UPDATE_ALL);
   var source=(TankEntity)l.getBlockEntity(from);var target=(TankEntity)l.getBlockEntity(to);
   require(source!=null&&target!=null&&source.capacity()==16000&&target.capacity()==6000,"registered block entities");
   var b=new WaterBatch(1000,WaterBalance.Quality.DESALINATE,35,.4,Set.of("SEA"));source.insert(b);
   source.step(l);require(source.water().ml()==750&&target.water().ml()==250&&target.water().salinity()==35,"downhill flow conserved");
   require(target.extract(WaterBatch.empty(),250,250).ml()==0,"no filter cartridge no output");
   require(target.installFilter(),"cartridge installs");var filtered=target.extract(WaterBatch.empty(),250,250);
   require(filtered.ml()==250&&filtered.salinity()==35&&filtered.risk()<.4&&target.charge()==11750&&target.water().ml()==0,"filter charge and salt");
   l.setBlock(same,WaterInfrastructure.BASIN.get().defaultBlockState(),Block.UPDATE_ALL);
   l.setBlock(down,WaterInfrastructure.PIPE.get().defaultBlockState().setValue(ClayPipeBlock.OUT,Direction.EAST).setValue(ClayPipeBlock.IN,Direction.WEST),Block.UPDATE_ALL);
   source.step(l);require(source.water().ml()==750&&((TankEntity)l.getBlockEntity(same)).water().ml()==0,"no equal-height destination");
   l.setBlock(down,WaterInfrastructure.PIPE.get().defaultBlockState().setValue(ClayPipeBlock.OUT,Direction.WEST).setValue(ClayPipeBlock.IN,Direction.WEST),Block.UPDATE_ALL);
   source.step(l);require(source.water().ml()==750,"loop rejected");
   l.setBlock(down,WaterInfrastructure.PIPE.get().defaultBlockState().setValue(ClayPipeBlock.OUT,Direction.DOWN).setValue(ClayPipeBlock.IN,Direction.WEST),Block.UPDATE_ALL);
   l.setBlock(down,WaterInfrastructure.PIPE.get().defaultBlockState().setValue(ClayPipeBlock.OUT,Direction.DOWN).setValue(ClayPipeBlock.IN,Direction.NORTH),Block.UPDATE_ALL);
   source.step(l);require(source.water().ml()==750,"disconnected elbow rejected");
   l.setBlock(down,WaterInfrastructure.PIPE.get().defaultBlockState().setValue(ClayPipeBlock.OUT,Direction.DOWN).setValue(ClayPipeBlock.IN,Direction.WEST),Block.UPDATE_ALL);
   l.setBlock(pipe.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),Block.UPDATE_ALL);
   require(l.hasNeighborSignal(pipe),"redstone fixture powered");source.step(l);
   require(source.water().ml()==750&&target.water().ml()==0,"powered pipe stops flow");
   l.setBlock(pipe.above(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);source.step(l);
   require(source.water().ml()==500&&target.water().ml()==250,"unpowered pipe resumes flow");
   l.setBlock(intake,WaterInfrastructure.INTAKE.get().defaultBlockState(),Block.UPDATE_ALL);
   l.setBlock(collector,WaterInfrastructure.COLLECTOR.get().defaultBlockState(),Block.UPDATE_ALL);
   var in=(TankEntity)l.getBlockEntity(intake);var rain=(TankEntity)l.getBlockEntity(collector);
   require(in!=null&&in.capacity()==1000&&rain!=null&&rain.capacity()==8000,"intake/collector registration and capacities");
   in.step(l);require(in.water().ml()==0,"intake without source");
   l.setBlock(intake.above(),Blocks.WATER.defaultBlockState(),Block.UPDATE_ALL);in.step(l);
   require(in.water().ml()==250,"natural intake draws actual source");
   l.setBlock(collector.above(),Blocks.GLASS.defaultBlockState(),Block.UPDATE_ALL);rain.step(l);
   require(rain.water().ml()==0,"covered collector stays empty");
   var saved=source.saveWithoutMetadata(l.registryAccess());
   var copy=new TankEntity(from,source.getBlockState());copy.loadWithComponents(saved,l.registryAccess());
   require(copy.water().equals(source.water()),"NBT roundtrip");
  }finally{for(var p:fixture)l.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);}
 }
}
