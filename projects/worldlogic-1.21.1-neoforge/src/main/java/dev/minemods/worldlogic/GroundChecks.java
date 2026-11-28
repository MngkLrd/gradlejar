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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import dev.minemods.worldlogic.api.WorldLogicApi;
final class GroundChecks{
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("ground: "+s);}
 static void verify(ServerLevel l){
  VolcanicTerrain.verify();
  require(GroundMaterials.whiteSand("tropical_sandy_coast",.27,80,false,.15),"wider warm beach sand coverage");
  require(!GroundMaterials.whiteSand("tropical_sandy_coast",.1,80,true,.9),"cold coast exclusion");
  require(!GroundMaterials.whiteSand("tropical_sandy_coast",.5,120,true,.9),"distant coast exclusion");
  require(GroundMaterials.volcanicSand(290,100,.15)&&!GroundMaterials.volcanicSand(301,100,.9),"bounded volcanic sand");
  require(GroundMaterials.soil("prairie",.1,.52,.06,.12).equals("chernozem"),"wider chernozem patches");
  require(GroundMaterials.soil("prairie",.1,.52,.06,-.2).isEmpty(),"retain ordinary soil patches");
  require(GroundMaterials.soil("prairie",.1,.52,.06,.65).equals("chernozem"),"steppe chernozem");
  require(GroundMaterials.soil("hot_desert",.7,.1,.02,.9).isEmpty(),"desert cannot form chernozem");
  require(GroundMaterials.soil("equatorial_rainforest",.6,.8,.1,.8).equals("laterite"),"humid weathered laterite");
  require(GroundMaterials.soil("dry_savanna",.6,.24,.1,.8).equals("sandy_loam"),"dry sandy loam");
  require(GroundMaterials.soil("prairie",.1,.52,.7,.9).isEmpty(),"steep eroding slope");
  var chunk=new ProtoChunk(new ChunkPos(0,0),UpgradeData.EMPTY,LevelHeightAccessor.create(-256,768),l.registryAccess().registryOrThrow(Registries.BIOME),null);
  var p=new BlockPos.MutableBlockPos();
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=94;y<=100;y++)chunk.setBlockState(p.set(x,y,z),y==100?Blocks.GRASS_BLOCK.defaultBlockState():y>=97?Blocks.DIRT.defaultBlockState():Blocks.STONE.defaultBlockState(),false);
  for(int x=2;x<=4;x++){
   String id=List.of("chernozem","sandy_loam","laterite").get(x-2);
   require(GroundMaterials.horizon(chunk,x,4,100,id,3)==3,"actual soil horizon "+id);
   require(chunk.getBlockState(p.set(x,100,4)).is(Blocks.GRASS_BLOCK)&&GroundMaterials.is(chunk.getBlockState(p.set(x,98,4)),id),"grass cap and subsoil "+id);
  }
  for(int y=54;y<=58;y++)chunk.setBlockState(p.set(7,y,7),y>=56?Blocks.SAND.defaultBlockState():Blocks.STONE.defaultBlockState(),false);
  require(GroundMaterials.sand(chunk,7,7,58,"white_sand",2)==2&&GroundMaterials.is(chunk.getBlockState(p.set(7,57,7)),"white_sand"),"actual white sand layer");
  require(GroundMaterials.sand(chunk,7,7,58,"volcanic_sand",2)==2&&GroundMaterials.is(chunk.getBlockState(p.set(7,57,7)),"volcanic_sand"),"actual volcanic sand layer");
  for(int y=94;y<=100;y++)chunk.setBlockState(p.set(10,y,10),Blocks.STONE.defaultBlockState(),false);
  require(VolcanicTerrain.raise(chunk,10,10,100,20,63),"actual volcanic rock column");
  require(chunk.getBlockState(p.set(10,120,10)).is(Blocks.BASALT)||GroundMaterials.is(chunk.getBlockState(p.set(10,120,10)),"volcanic_tuff"),"actual volcanic cap");
  require(!VolcanicTerrain.raise(chunk,10,10,120,44,63)&&!VolcanicTerrain.raise(chunk,7,7,58,8,63),"volcanic vertical and marine guards");
  for(String id:List.of("white_sand","volcanic_sand","chernozem","sandy_loam","laterite","volcanic_ash","volcanic_tuff")){
   var block=GroundMaterials.block(id);var state=block.defaultBlockState();
   require(block.asItem()!=Items.AIR,"registered block item "+id);
   boolean rock=id.equals("volcanic_tuff");
   require(state.is(rock?BlockTags.MINEABLE_WITH_PICKAXE:BlockTags.MINEABLE_WITH_SHOVEL),"tool tag "+id);
   require((block instanceof FallingBlock)==(id.endsWith("_sand")||id.equals("volcanic_ash")),"granular physics "+id);
   var drops=Block.getDrops(state,l,new BlockPos(4,110,4),null,null,new ItemStack(rock?Items.IRON_PICKAXE:Items.IRON_SHOVEL));
   require(drops.stream().anyMatch(v->v.is(block.asItem())),"actual mining loot "+id);
   var profile=WorldLogicApi.soilProfile(state);
   require(profile.soil()!=WorldLogicApi.SoilType.UNKNOWN&&profile.waterCapacityMm()>0&&profile.fertility()>=0&&profile.fertility()<=1,"public profile "+id);
  }
  var rich=WorldLogicApi.soilProfile(GroundMaterials.state("chernozem"));var sand=WorldLogicApi.soilProfile(GroundMaterials.state("white_sand"));var ash=WorldLogicApi.soilProfile(GroundMaterials.state("volcanic_ash"));
  require(rich.fertility()>sand.fertility()&&rich.waterCapacityMm()>sand.waterCapacityMm()&&sand.permeability()>rich.permeability()&&ash.fertility()<rich.fertility(),"physical soil differences");
  var ground=new BlockPos(4,115,4);var old=l.getBlockState(ground);var lower=l.getBlockState(ground.below());
  try{l.setBlock(ground.below(),GroundMaterials.state("chernozem"),2|16);l.setBlock(ground,Blocks.GRASS_BLOCK.defaultBlockState(),2|16);
   require(WorldLogicApi.soilProfile(l,ground).orElseThrow().soil()==WorldLogicApi.SoilType.CHERNOZEM,"loaded root horizon API");
  }finally{l.setBlock(ground,old,2|16);l.setBlock(ground.below(),lower,2|16);}
 }
}
