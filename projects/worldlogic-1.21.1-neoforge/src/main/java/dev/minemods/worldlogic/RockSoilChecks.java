package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import dev.minemods.worldlogic.api.WorldLogicApi;
final class RockSoilChecks{
 private static void require(boolean value,String what){if(!value)throw new IllegalStateException("rock/soil: "+what);}
 private static int drops(ServerLevel l,BlockState s,Item item,Item tool){
  return Block.getDrops(s,l,new BlockPos(4,110,4),null,null,new ItemStack(tool)).stream().filter(v->v.is(item)).mapToInt(ItemStack::getCount).sum();
 }
 static void verify(ServerLevel l){
  var origin=new BlockPos(4,112,4);var saved=new LinkedHashMap<BlockPos,BlockState>();
  for(int dx=-1;dx<=2;dx++)for(int dy=-1;dy<=2;dy++)for(int dz=-1;dz<=1;dz++){
   var pos=origin.offset(dx,dy,dz);saved.put(pos,l.getBlockState(pos));
  }
  var player=FakePlayerFactory.getMinecraft(l);
  var oldHand=player.getMainHandItem().copy();double px=player.getX(),py=player.getY(),pz=player.getZ();
  var water=NaturalFluids.get(WorldLogicApi.WaterKind.LAKE).block.get().defaultBlockState();
  int farmCases=0;
  try{
   player.setPos(origin.getX()+.5,origin.getY()+1,origin.getZ()+.5);
   for(String id:SoilFarmland.IDS){
    var parent=GroundMaterials.state(id);var farm=SoilFarmland.block(id);
    l.setBlock(origin.below(),parent,2|16);l.setBlock(origin,parent,2|16);
    l.setBlock(origin.above(),Blocks.AIR.defaultBlockState(),2|16);l.setBlock(origin.east(),water,2|16);
    var hoe=new ItemStack(Items.IRON_HOE);player.setItemInHand(InteractionHand.MAIN_HAND,hoe);
    var hit=new BlockHitResult(Vec3.atCenterOf(origin).add(0,.5,0),Direction.UP,origin,false);
    var result=hoe.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
    var tilled=l.getBlockState(origin);
    require(result.consumesAction()&&tilled.is(farm)&&hoe.getDamageValue()==1,"actual hoe action "+id);
    require(WorldLogicApi.cultivatedSoil(tilled)&&WorldLogicApi.parentSoil(tilled).equals(parent)&&WorldLogicApi.soilProfile(tilled).equals(WorldLogicApi.soilProfile(parent)),"preserved soil API "+id);
    tilled.randomTick(l,origin,l.random);
    require(l.getBlockState(origin).getValue(FarmBlock.MOISTURE)==7,"actual native-water moisture "+id);
    require(Blocks.WHEAT.defaultBlockState().canSurvive(l,origin.above()),"vanilla crop survival "+id);
    require(drops(l,tilled,parent.getBlock().asItem(),Items.IRON_SHOVEL)==1,"soil-specific drop "+id);
    FarmBlock.turnToDirt(null,l.getBlockState(origin),l,origin);
    require(l.getBlockState(origin).equals(parent),"actual farmland reversion "+id);
    l.setBlock(origin.above(),Blocks.STONE.defaultBlockState(),2|16);
    var blocked=new ItemStack(Items.IRON_HOE);player.setItemInHand(InteractionHand.MAIN_HAND,blocked);
    blocked.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
    require(l.getBlockState(origin).equals(parent)&&blocked.getDamageValue()==0,"blocked hoe control "+id);
    require(SoilFarmland.tilled(Blocks.GRASS_BLOCK.defaultBlockState(),parent).is(farm),"grass cap root soil "+id);
    farmCases++;
   }
   require(SoilFarmland.tilled(GroundMaterials.state("frozen_soil"),Blocks.STONE.defaultBlockState())==null&&SoilFarmland.tilled(GroundMaterials.state("white_sand"),Blocks.STONE.defaultBlockState())==null,"frozen/sand not tillable");
  }finally{player.setItemInHand(InteractionHand.MAIN_HAND,oldHand);player.setPos(px,py,pz);saved.forEach((q,s)->l.setBlock(q,s,2|16));}
  int blocks=0,recipes=0;
  var all=l.getRecipeManager().getAllRecipesFor(RecipeType.STONECUTTING);
  for(var f:RockContent.FAMILIES){
   String id=f.id();
   for(String base:List.of(id,"polished_"+id,id+"_bricks")){
    var b=RockContent.block(base);var bs=b.defaultBlockState();
    require(Math.abs(bs.getDestroySpeed(l,origin)-f.hardness())<.001,"actual hardness "+base);
    require(bs.is(BlockTags.MINEABLE_WITH_PICKAXE)&&drops(l,bs,b.asItem(),Items.IRON_PICKAXE)==1,"full stone mining "+base);blocks++;
    if(!base.equals(id)){recipe(l,all,id,base,1);recipes++;}
    for(String shape:List.of("slab","stairs","wall")){
     String out=base+"_"+shape;var block=RockContent.block(out);var s=block.defaultBlockState();
     require(s.is(BlockTags.MINEABLE_WITH_PICKAXE)&&drops(l,s,block.asItem(),Items.IRON_PICKAXE)==1,"shape loot "+out);
     require(Math.abs(s.getDestroySpeed(l,origin)-f.hardness())<.001,"shape hardness "+out);
     if(shape.equals("slab")){
      require(block instanceof SlabBlock&&s.is(BlockTags.SLABS),"real slab "+out);
      require(drops(l,s.setValue(SlabBlock.TYPE,SlabType.DOUBLE),block.asItem(),Items.IRON_PICKAXE)==2,"double slab loot "+out);
     }else if(shape.equals("stairs"))require(block instanceof StairBlock&&s.is(BlockTags.STAIRS),"real stairs "+out);
     else require(block instanceof WallBlock&&s.is(BlockTags.WALLS),"real wall "+out);
     int count=shape.equals("slab")?2:1;
     recipe(l,all,base,out,count);recipes++;
     if(!base.equals(id)){recipe(l,all,id,out,count);recipes++;}
     blocks++;
    }
   }
  }
  require(blocks==60&&recipes==85,"complete family matrix");
  require(Blocks.CALCITE.defaultBlockState().getDestroySpeed(l,origin)<Blocks.STONE.defaultBlockState().getDestroySpeed(l,origin)&&Blocks.GRANITE.defaultBlockState().getDestroySpeed(l,origin)>Blocks.STONE.defaultBlockState().getDestroySpeed(l,origin)&&Blocks.DEEPSLATE.defaultBlockState().getDestroySpeed(l,origin)>Blocks.GRANITE.defaultBlockState().getDestroySpeed(l,origin),"actual vanilla hardness ordering");
  require(Blocks.BEDROCK.defaultBlockState().getDestroySpeed(l,origin)<0,"unbreakable negative control");
  var chunk=new ProtoChunk(new ChunkPos(0,0),UpgradeData.EMPTY,LevelHeightAccessor.create(-256,768),l.registryAccess().registryOrThrow(Registries.BIOME),null);
  var pos=new BlockPos.MutableBlockPos();var found=new HashSet<Block>();
  for(int x=0;x<GeologyModel.Province.values().length;x++){
   var province=GeologyModel.Province.values()[x];
   for(int y=-256;y<=74;y++)chunk.setBlockState(pos.set(x,y,0),y==-256?Blocks.BEDROCK.defaultBlockState():y==-80?Blocks.CAVE_AIR.defaultBlockState():y==-81?Blocks.IRON_ORE.defaultBlockState():y<0?Blocks.DEEPSLATE.defaultBlockState():Blocks.STONE.defaultBlockState(),false);
   RockContent.stratify(chunk,x,0,74,province,0);
   require(chunk.getBlockState(pos.set(x,-256,0)).is(Blocks.BEDROCK)&&chunk.getBlockState(pos.set(x,-80,0)).isAir()&&chunk.getBlockState(pos.set(x,-81,0)).is(Blocks.IRON_ORE),"bedrock/cave/ore preservation");
   for(int y=-248;y<-112;y++){var state=chunk.getBlockState(pos.set(x,y,0));if(RockContent.natural(state))found.add(state.getBlock());}
  }
  require(found.containsAll(List.of(RockContent.block("limestone"),RockContent.block("shale"),RockContent.block("slate"),RockContent.block("gneiss"))),"actual deep horizon family reachability");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_ROCK_SOIL farms={} stoneBlocks={} stonecuttingRecipes={} deepFamilies={}",farmCases,blocks,recipes,found.size());
 }
 private static void recipe(ServerLevel l,List<RecipeHolder<StonecutterRecipe>> all,String from,String to,int count){
  var input=new SingleRecipeInput(new ItemStack(RockContent.block(from)));
  var match=all.stream().filter(r->r.id().getNamespace().equals("worldlogic")&&r.id().getPath().equals(to+"_from_"+from+"_stonecutting")).findFirst().orElseThrow(()->new IllegalStateException("missing stonecutting "+from+" -> "+to));
  var result=match.value().assemble(input,l.registryAccess());
  require(match.value().matches(input,l)&&result.is(RockContent.block(to).asItem())&&result.getCount()==count&&!match.value().matches(new SingleRecipeInput(new ItemStack(Items.DIRT)),l),"actual recipe "+from+" -> "+to);
 }
}
