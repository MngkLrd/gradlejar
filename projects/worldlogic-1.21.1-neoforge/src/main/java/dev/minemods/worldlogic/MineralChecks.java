package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
final class MineralChecks{
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("mineral deposits: "+s);}
 static void verify(ServerLevel l){
  var minerals=new EnumMap<MineralDeposits.Mineral,MineralDeposits.Deposit>(MineralDeposits.Mineral.class);
  for(int gx=-12;gx<=12;gx++)for(int gz=-12;gz<=12;gz++){
   var ds=MineralDeposits.district(42,gx,gz);require(ds.equals(MineralDeposits.district(42,gx,gz)),"deterministic district");
   for(var d:ds){
    require(d.minY()>-248&&d.maxY()<150&&d.reach()<96,"bounded deposit");
    var col=RockStrata.column(42,(int)d.x(),(int)d.z());
    require(d.mineral().hosts.contains(col.at((int)d.y()).host()),"modeled center host");
    minerals.putIfAbsent(d.mineral(),d);
   }
  }
  require(minerals.size()==7,"all seven mineral families reachable");
  var gradeSet=new TreeSet<Integer>();
  var bounds=LevelHeightAccessor.create(-256,768);
  int total=0;
  for(var entry:minerals.entrySet()){
   var m=entry.getKey();var d=entry.getValue();
   var chunk=new ProtoChunk(new ChunkPos(Math.floorDiv((int)d.x(),16),Math.floorDiv((int)d.z(),16)),UpgradeData.EMPTY,bounds,l.registryAccess().registryOrThrow(Registries.BIOME),null);
   var pos=new BlockPos.MutableBlockPos();
   var host=m.hosts.stream().sorted().findFirst().orElseThrow().block;
   int x0=chunk.getPos().getMinBlockX(),z0=chunk.getPos().getMinBlockZ();
   for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++)for(int y=Math.max(-248,d.minY());y<=Math.min(110,d.maxY()+8);y++)
    chunk.setBlockState(pos.set(x,y,z),host.defaultBlockState(),false);
   var center=BlockPos.containing(d.x(),d.y(),d.z());
   chunk.setBlockState(center,Blocks.CAVE_AIR.defaultBlockState(),false);
   chunk.setBlockState(center.above(),Blocks.BEDROCK.defaultBlockState(),false);
   Heightmap.primeHeightmaps(chunk,EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG));
   MineralDeposits.place(chunk,d);int found=0;
   for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++)for(int y=d.minY();y<=d.maxY();y++){
    var s=chunk.getBlockState(pos.set(x,y,z));
    if(s.getBlock() instanceof MineralOreBlock b){
     int grade=s.getValue(MineralOreBlock.RICHNESS);
     require(b.mineral()==m&&grade==d.grade(x,y,z),"actual generated grade");gradeSet.add(grade);found++;
    }
   }
   require(found>0,"actual host mineralized "+m);
   require(chunk.getBlockState(center).isAir()&&chunk.getBlockState(center.above()).is(Blocks.BEDROCK),"cavity and nonhost protected");
   total+=found;
   for(int grade=1;grade<=3;grade++){
    var state=MineralContent.state(m,grade);
    require(state.is(BlockTags.MINEABLE_WITH_PICKAXE),"registered mining tag");
    require(Block.stateById(Block.getId(state))==state,"grade palette identity");
    var drops=Block.getDrops(state,l,new BlockPos(4,110,4),null,null,new ItemStack(Items.IRON_PICKAXE));
    int count=drops.stream().filter(v->v.is(MineralContent.get(m).raw().get())).mapToInt(ItemStack::getCount).sum();
    require(count==grade,"actual loot grade "+m+"/"+grade+" count "+count);
    var info=dev.minemods.worldlogic.api.WorldLogicApi.mineral(state).orElseThrow();
    require(info.grade()==grade&&info.mineral().equals(m.id()),"mineral API grade");
   }
  }
  require(gradeSet.equals(Set.of(1,2,3)),"all grades generated");
  require(dev.minemods.worldlogic.api.WorldLogicApi.mineral(Blocks.STONE.defaultBlockState()).isEmpty(),"non-mineral negative control");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_MINERALS types={} generatedBlocks={} grades={} lootCases=21",minerals.size(),total,gradeSet);
 }
}
