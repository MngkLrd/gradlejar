package dev.minemods.worldlogic;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
public final class GeologyChecks {
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("geology: "+s);}
 public static void verify(ServerLevel l){
  var key=ResourceKey.create(Registries.WORLD_PRESET,ResourceLocation.fromNamespaceAndPath("worldlogic","earthlike_extended"));
  var preset=l.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(key).value();
  var gen=(NoiseBasedChunkGenerator)preset.createWorldDimensions().overworld();
  require(GeologyGeneration.enabled(gen)&&gen.getMinY()==-256&&gen.getGenDepth()==768&&gen.getSeaLevel()==63,"new preset bounds");
  require(!GeologyGeneration.enabled(l.getChunkSource().getGenerator()),"old generator isolated");
  var bounds=LevelHeightAccessor.create(-256,768);
  var state=RandomState.create(gen.generatorSettings().value(),l.registryAccess().lookupOrThrow(Registries.NOISE),42);
  int min=512,max=-256;
  for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
   int h=gen.getBaseHeight(x*512,z*512,Heightmap.Types.OCEAN_FLOOR_WG,bounds,state);
   require(h>-256&&h<464,"extended base height "+h);min=Math.min(min,h);max=Math.max(max,h);
  }
  require(max>min,"relief present");
  var types=EnumSet.noneOf(GeologyModel.Ore.class);var provinces=EnumSet.noneOf(GeologyModel.Province.class);
  GeologyModel.Cave chamber=null;GeologyModel.Body deposit=null;
  for(int gx=-12;gx<=12;gx++)for(int gz=-12;gz<=12;gz++){
   var d=GeologyModel.district(42,gx,gz);
   require(d.equals(GeologyModel.district(42,gx,gz)),"deterministic district");
   provinces.add(d.province());
   for(var o:d.ores()){
    types.add(o.ore());require(o.minY()>-248&&o.maxY()<150&&o.grade()>0&&o.grade()<1,"body envelope");
    if(o.ore()==GeologyModel.Ore.DIAMOND)require(o.maxY()<-80,"deep diamond pipe");
    if(deposit==null&&o.ore()==GeologyModel.Ore.IRON)deposit=o;
   }
   if(chamber==null&&d.province()==GeologyModel.Province.CARBONATE)chamber=d.caves().get(2);
  }
  require(types.size()==8&&provinces.size()==5&&chamber!=null&&deposit!=null,"all mineral and province families");
  require(!GeologyModel.district(42,0,0).equals(GeologyModel.district(43,0,0)),"seed affects geology");
  var fixture=fixture(l,bounds,(int)Math.floor(chamber.x()),(int)Math.floor(chamber.z()));
  GeologyGeneration.carve(42,fixture);
  var p=BlockPos.containing(chamber.x(),chamber.y(),chamber.z());
  require(fixture.getBlockState(p).isAir()||fixture.getBlockState(p).is(Blocks.WATER),"real cave block changed");
  require(fixture.getBlockState(new BlockPos(p.getX(),-256,p.getZ())).is(Blocks.BEDROCK),"bedrock preserved");
  var oreChunk=fixture(l,bounds,(int)Math.floor(deposit.x()),(int)Math.floor(deposit.z()));
  GeologyGeneration.mineralize(42,oreChunk);
  int found=0;
  for(int x=oreChunk.getPos().getMinBlockX();x<oreChunk.getPos().getMinBlockX()+16;x++)for(int z=oreChunk.getPos().getMinBlockZ();z<oreChunk.getPos().getMinBlockZ()+16;z++)for(int y=-248;y<80;y++){
   var b=oreChunk.getBlockState(new BlockPos(x,y,z));
   if(b.is(Blocks.IRON_ORE)||b.is(Blocks.DEEPSLATE_IRON_ORE))found++;
  }
  require(found>0,"actual iron blocks mineralized");
  require(oreChunk.getBlockState(new BlockPos(oreChunk.getPos().getMinBlockX(),-256,oreChunk.getPos().getMinBlockZ())).is(Blocks.BEDROCK),"ore does not replace bedrock");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_GEOLOGY_FIXTURE min={} max={} provinces={} ores={} ironBlocks={}",min,max,provinces.size(),types.size(),found);
 }
 private static ProtoChunk fixture(ServerLevel l,LevelHeightAccessor bounds,int x,int z){
  var chunk=new ProtoChunk(new ChunkPos(Math.floorDiv(x,16),Math.floorDiv(z,16)),UpgradeData.EMPTY,bounds,l.registryAccess().registryOrThrow(Registries.BIOME),null);
  var p=new BlockPos.MutableBlockPos();
  for(int xx=chunk.getPos().getMinBlockX();xx<chunk.getPos().getMinBlockX()+16;xx++)for(int zz=chunk.getPos().getMinBlockZ();zz<chunk.getPos().getMinBlockZ()+16;zz++)for(int y=-256;y<=100;y++){
   p.set(xx,y,zz);chunk.setBlockState(p,(y==-256?Blocks.BEDROCK:y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState(),false);
  }
  Heightmap.primeHeightmaps(chunk,EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG));return chunk;
 }
}
