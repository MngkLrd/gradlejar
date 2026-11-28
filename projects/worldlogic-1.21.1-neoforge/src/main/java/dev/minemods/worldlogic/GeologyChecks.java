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
  RockStrata.verify();
  verifyStrata(l);
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
 public static void verifyOres(ServerLevel l){
  var bounds=LevelHeightAccessor.create(-256,768);
  var deposits=new java.util.EnumMap<GeologyModel.Ore,GeologyModel.Body>(GeologyModel.Ore.class);
  for(int gx=-12;gx<=12;gx++)for(int gz=-12;gz<=12;gz++)
   for(var o:GeologyModel.district(42,gx,gz).ores())deposits.putIfAbsent(o.ore(),o);
  require(deposits.size()==8,"all eight ore families present");
  var counts=new java.util.EnumMap<GeologyModel.Ore,Integer>(GeologyModel.Ore.class);
  for(var entry:deposits.entrySet()){
   var ore=entry.getKey();var body=entry.getValue();
   var chunk=fixture(l,bounds,(int)Math.floor(body.x()),(int)Math.floor(body.z()));
   var center=BlockPos.containing(body.x(),body.y(),body.z());
   chunk.setBlockState(center,Blocks.CAVE_AIR.defaultBlockState(),false);
   GeologyGeneration.mineralize(42,chunk);
   require(chunk.getBlockState(center).isAir(),"mineralization preserves cave air "+ore);
   var p=new BlockPos.MutableBlockPos();int count=0;
   for(int x=chunk.getPos().getMinBlockX();x<chunk.getPos().getMinBlockX()+16;x++)
    for(int z=chunk.getPos().getMinBlockZ();z<chunk.getPos().getMinBlockZ()+16;z++){
     require(chunk.getBlockState(p.set(x,-256,z)).is(Blocks.BEDROCK),"bedrock preserved "+ore);
     for(int y=Math.max(-248,body.minY());y<=Math.min(96,body.maxY());y++)
      if(chunk.getBlockState(p.set(x,y,z)).equals(GeologyGeneration.ore(ore,y<0)))count++;
    }
   require(count>0,"actual chunk generated ore "+ore);counts.put(ore,count);
  }
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_ORE_FIXTURES {}",counts);
 }
 public static void verifyBalance(ServerLevel l){
  var bounds=LevelHeightAccessor.create(-256,768);
  int maxBody=0,bodies=0,small=0,maxSmall=0;
  for(long seed:new long[]{42,-6487130996193510395L}){
   for(int gx=-3;gx<=3;gx++)for(int gz=-3;gz<=3;gz++){
    for(var o:GeologyModel.district(seed,gx,gz).ores()){
     int count=countBody(o);
     require(count<=300,"oversized regional deposit "+o.ore()+" blocks="+count);
     maxBody=Math.max(maxBody,count);bodies++;
    }
    var pockets=GeologyModel.pockets(seed,gx,gz);
    require(pockets.size()>=6&&pockets.equals(GeologyModel.pockets(seed,gx,gz)),"frequent deterministic pockets");
    require(pockets.stream().filter(GeologyModel.Pocket::relative).count()>=4,"four shallow/intermediate pockets");
    for(var p:pockets){int count=countBody(p.body());maxSmall=Math.max(maxSmall,count);small++;require(count>0&&count<=120,"small pocket block count "+count);}
   }
  }
  int chunks=0,total=0,min=Integer.MAX_VALUE,max=0,minShallow=Integer.MAX_VALUE;
  // Sample widely spaced ordinary rock columns, not chosen deposit centers.
  for(int gx=-2;gx<=2;gx++)for(int gz=-1;gz<=1;gz++){
   int x=gx*176+37,z=gz*144+61;
   var chunk=fixture(l,bounds,x,z);
   GeologyGeneration.mineralize(42,chunk);
   var p=new BlockPos.MutableBlockPos();int count=0,shallow=0;
   for(int xx=chunk.getPos().getMinBlockX();xx<chunk.getPos().getMinBlockX()+16;xx++)
    for(int zz=chunk.getPos().getMinBlockZ();zz<chunk.getPos().getMinBlockZ()+16;zz++)
     for(int y=-248;y<=96;y++){
      var s=chunk.getBlockState(p.set(xx,y,zz));
      if(isOre(s)){count++;if(y>=60)shallow++;}
     }
   require(count>=18&&shallow>=5,"resource desert in ordinary fixture "+x+","+z+" total="+count+" shallow="+shallow);
   chunks++;total+=count;min=Math.min(min,count);max=Math.max(max,count);minShallow=Math.min(minShallow,shallow);
  }
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_ORE_BALANCE bodies={} maxBody={} pockets={} maxPocket={} syntheticChunks={} total={} min={} max={} minShallow={}",bodies,maxBody,small,maxSmall,chunks,total,min,max,minShallow);
 }
 private static boolean isOre(net.minecraft.world.level.block.state.BlockState s){
  return s.is(net.minecraft.tags.BlockTags.COAL_ORES)||s.is(net.minecraft.tags.BlockTags.IRON_ORES)||s.is(net.minecraft.tags.BlockTags.COPPER_ORES)||s.is(net.minecraft.tags.BlockTags.GOLD_ORES)||s.is(net.minecraft.tags.BlockTags.DIAMOND_ORES)||s.is(net.minecraft.tags.BlockTags.EMERALD_ORES)||s.is(net.minecraft.tags.BlockTags.LAPIS_ORES)||s.is(net.minecraft.tags.BlockTags.REDSTONE_ORES);
 }
 private static int countBody(GeologyModel.Body o){
  int radius=(int)Math.ceil(Math.max(o.along(),o.across()))+2,count=0;
  int cx=(int)Math.floor(o.x()),cz=(int)Math.floor(o.z());
  for(int x=cx-radius;x<=cx+radius;x++)for(int z=cz-radius;z<=cz+radius;z++)
   for(int y=o.minY();y<=o.maxY();y++)if(o.contains(x,y,z))count++;
  return count;
 }
 public static void verifyKarst(ServerLevel l){
  var bounds=LevelHeightAccessor.create(-256,768);
  GeologyModel.District d=null;
  outer:for(int gx=-12;gx<=12;gx++)for(int gz=-12;gz<=12;gz++){
   var candidate=GeologyModel.district(42,gx,gz);
   if(candidate.province()==GeologyModel.Province.CARBONATE&&candidate.caves().get(0).shaft()){d=candidate;break outer;}
  }
  require(d!=null&&d.caves().size()>=4,"shaft, network and lake district");
  var shaft=d.caves().get(0);var lake=d.caves().get(3);
  var chunks=new java.util.HashMap<ChunkPos,ProtoChunk>();
  java.util.function.Function<BlockPos,net.minecraft.world.level.block.state.BlockState> block=pos->{
   var key=new ChunkPos(pos);
   var ch=chunks.computeIfAbsent(key,k->{var made=fixture(l,bounds,pos.getX(),pos.getZ());GeologyGeneration.carve(42,made);return made;});
   return ch.getBlockState(pos);
  };
  int x=(int)Math.floor(shaft.x()),z=(int)Math.floor(shaft.z());
  for(int y=(int)Math.ceil(shaft.y());y<=100;y++)require(block.apply(new BlockPos(x,y,z)).isAir(),"open shaft at "+y);
  require(block.apply(new BlockPos(x+5,100,z)).is(Blocks.STONE),"narrow shaft does not erase nearby surface");
  int lx=(int)Math.floor(lake.x()),lz=(int)Math.floor(lake.z()),water=lake.waterY();
  require(block.apply(new BlockPos(lx,water-2,lz)).is(Blocks.WATER),"subterranean lake water");
  require(block.apply(new BlockPos(lx,water+3,lz)).isAir(),"lake headspace");
  int connected=0;
  // Follow actual carved tunnel centerline across chunk boundaries, not only shape score.
  for(int a=0;a<=28;a+=2){
   double xx=shaft.x()+a*Math.cos(shaft.angle()),zz=shaft.z()+a*Math.sin(shaft.angle());
   double yy=shaft.y()+a*shaft.dip()+3*Math.sin(a*.06);
   var pos=BlockPos.containing(xx,yy,zz);var s=block.apply(pos);
   require(s.isAir()||s.is(Blocks.WATER),"karst passage obstructed at "+pos);connected++;
  }
  require(block.apply(new BlockPos(lx,-256,lz)).is(Blocks.BEDROCK),"lake preserves bedrock");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_KARST shaftDepth={} waterY={} connectedSamples={} syntheticChunks={}",100-(int)shaft.y(),water,connected,chunks.size());
 }
 private static ProtoChunk fixture(ServerLevel l,LevelHeightAccessor bounds,int x,int z){
  var chunk=new ProtoChunk(new ChunkPos(Math.floorDiv(x,16),Math.floorDiv(z,16)),UpgradeData.EMPTY,bounds,l.registryAccess().registryOrThrow(Registries.BIOME),null);
  var p=new BlockPos.MutableBlockPos();
  for(int xx=chunk.getPos().getMinBlockX();xx<chunk.getPos().getMinBlockX()+16;xx++)for(int zz=chunk.getPos().getMinBlockZ();zz<chunk.getPos().getMinBlockZ()+16;zz++)for(int y=-256;y<=100;y++){
   p.set(xx,y,zz);chunk.setBlockState(p,(y==-256?Blocks.BEDROCK:y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState(),false);
  }
  Heightmap.primeHeightmaps(chunk,EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG));return chunk;
 }
 private static void verifyStrata(ServerLevel l){
  var bounds=LevelHeightAccessor.create(-256,768);
  var chunk=fixture(l,bounds,-33,65);int x=chunk.getPos().getMinBlockX(),z=chunk.getPos().getMinBlockZ();
  var cavity=new BlockPos(x,-150,z);var water=new BlockPos(x,-149,z);
  chunk.setBlockState(cavity,Blocks.CAVE_AIR.defaultBlockState(),false);
  chunk.setBlockState(water,Blocks.WATER.defaultBlockState(),false);
  GeologyGeneration.carve(42,chunk);
  int matched=0;var column=RockStrata.column(42,x,z);
  for(int y=-248;y<94;y++){
   var b=chunk.getBlockState(new BlockPos(x,y,z));if(!GeologyGeneration.rock(b))continue;
   var host=column.at(y).host();var expected=host==RockStrata.Host.BASEMENT&&y<0?Blocks.DEEPSLATE:host.block;
   require(b.is(expected),"actual stratigraphic host "+y);matched++;
  }
  require(matched>=200,"meaningful actual rock column");
  require(chunk.getBlockState(cavity).isAir()&&chunk.getBlockState(water).is(Blocks.WATER),"stratigraphy preserves cavities and water");
  require(chunk.getBlockState(new BlockPos(x,-256,z)).is(Blocks.BEDROCK),"stratigraphy preserves bedrock");
  require(chunk.getBlockState(new BlockPos(x,100,z)).is(Blocks.STONE),"surface protection");
 }

}
