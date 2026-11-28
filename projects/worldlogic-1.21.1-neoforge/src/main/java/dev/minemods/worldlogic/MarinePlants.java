package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.Supplier;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.*;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
/** Native seawater variants of vanilla marine vegetation, not a second flora system. */
public final class MarinePlants {
 private MarinePlants(){}
 private static final Supplier<Block> KELP=Content.BLOCKS.register("sea_kelp",()->new SeaKelp(Properties.ofFullCopy(Blocks.KELP).dropsLike(Blocks.KELP)));
 private static final Supplier<Block> STEM=Content.BLOCKS.register("sea_kelp_plant",()->new SeaStem(Properties.ofFullCopy(Blocks.KELP_PLANT).dropsLike(Blocks.KELP_PLANT)));
 private static final Supplier<Block> GRASS=Content.BLOCKS.register("sea_seagrass",()->new SeaGrass(Properties.ofFullCopy(Blocks.SEAGRASS).dropsLike(Blocks.SEAGRASS)));
 private static final Supplier<Block> TALL=Content.BLOCKS.register("sea_tall_seagrass",()->new SeaTall(Properties.ofFullCopy(Blocks.TALL_SEAGRASS)));
 static void register(){} // initialize deferred holders before Content.BLOCKS registers
 public static List<Block> blocks(){return List.of(KELP.get(),STEM.get(),GRASS.get(),TALL.get());}
 private static FluidState sea(){return NaturalFluids.get(WaterKind.SEA).source.get().defaultFluidState();}
 private static BlockState water(){return NaturalFluids.get(WaterKind.SEA).block.get().defaultBlockState();}
 static boolean seawater(BlockState s){return s.is(NaturalFluids.get(WaterKind.SEA).block.get())&&s.getFluidState().isSource();}
 static boolean vanillaPlant(BlockState s){return s.is(Blocks.KELP)||s.is(Blocks.KELP_PLANT)||s.is(Blocks.SEAGRASS)||s.is(Blocks.TALL_SEAGRASS);}
 static boolean isPlant(BlockState s){return vanillaPlant(s)||s.is(KELP.get())||s.is(STEM.get())||s.is(GRASS.get())||s.is(TALL.get());}
 static BlockState nativeState(BlockState s){
  if(s.is(Blocks.KELP))return KELP.get().defaultBlockState().setValue(GrowingPlantHeadBlock.AGE,s.getValue(GrowingPlantHeadBlock.AGE));
  if(s.is(Blocks.KELP_PLANT))return STEM.get().defaultBlockState();
  if(s.is(Blocks.SEAGRASS))return GRASS.get().defaultBlockState();
  if(s.is(Blocks.TALL_SEAGRASS))return TALL.get().defaultBlockState().setValue(TallSeagrassBlock.HALF,s.getValue(TallSeagrassBlock.HALF));
  return s;
 }
 public static BlockState placement(BlockPlaceContext c,boolean kelp){
  var l=c.getLevel();var p=c.getClickedPos();if(!seawater(l.getBlockState(p)))return null;
  var s=(kelp?KELP:GRASS).get().defaultBlockState();
  if(kelp&&isPlant(l.getBlockState(p.below()))&&l.getBlockState(p.below()).is(KELP.get()))s=s.setValue(GrowingPlantHeadBlock.AGE,0);
  return s.canSurvive(l,p)?s:null;
 }
 private static final class SeaKelp extends KelpBlock {
  SeaKelp(Properties p){super(p);}
  @Override protected Block getBodyBlock(){return STEM.get();}
  @Override protected boolean canGrowInto(BlockState s){return seawater(s);}
  @Override protected FluidState getFluidState(BlockState s){return sea();}
  @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){return new ItemStack(Items.KELP);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){return placement(c,true);}
 }
 private static final class SeaStem extends KelpPlantBlock {
  SeaStem(Properties p){super(p);}
  @Override protected GrowingPlantHeadBlock getHeadBlock(){return (GrowingPlantHeadBlock)KELP.get();}
  @Override protected FluidState getFluidState(BlockState s){return sea();}
  @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){return new ItemStack(Items.KELP);}
 }
 private static final class SeaGrass extends SeagrassBlock {
  SeaGrass(Properties p){super(p);}
  @Override protected FluidState getFluidState(BlockState s){return sea();}
  @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){return new ItemStack(Items.SEAGRASS);}
  @Override public BlockState getStateForPlacement(BlockPlaceContext c){return placement(c,false);}
  @Override public boolean isValidBonemealTarget(LevelReader l,BlockPos p,BlockState s){return seawater(l.getBlockState(p.above()));}
  @Override public void performBonemeal(ServerLevel l,RandomSource r,BlockPos p,BlockState s){
   if(!isValidBonemealTarget(l,p,s))return;
   var lower=TALL.get().defaultBlockState().setValue(TallSeagrassBlock.HALF,DoubleBlockHalf.LOWER);
   l.setBlock(p,lower,2|16);l.setBlock(p.above(),lower.setValue(TallSeagrassBlock.HALF,DoubleBlockHalf.UPPER),3);
  }
  @Override protected BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos np){
   var next=super.updateShape(s,d,n,l,p,np);return next.isAir()?water():next;
  }
 }
 private static final class SeaTall extends TallSeagrassBlock {
  SeaTall(Properties p){super(p);}
  @Override protected FluidState getFluidState(BlockState s){return sea();}
  @Override protected BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos np){
   var next=super.updateShape(s,d,n,l,p,np);return next.isAir()?water():next;
  }
 }
 private static final TagKey<Biome> OCEAN=TagKey.create(Registries.BIOME,ResourceLocation.withDefaultNamespace("is_ocean"));
 private static List<BlockPos> candidates(ChunkAccess c,net.minecraft.core.Holder<Biome> fixture,boolean existing){
  int x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ();var list=new ArrayList<BlockPos>();var p=new BlockPos.MutableBlockPos();
  for(int i=0;i<c.getSections().length;i++){
   if(!c.getSections()[i].maybeHas(MarinePlants::vanillaPlant))continue;
   int y0=c.getMinBuildHeight()+i*16;
   for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=y0;y<y0+16;y++){
    p.set(x0+x,y,z0+z);if(!vanillaPlant(c.getBlockState(p)))continue;
    int floor=c.getHeight(existing?Heightmap.Types.OCEAN_FLOOR:Heightmap.Types.OCEAN_FLOOR_WG,x,z);
    if(y<floor-1)continue;
    int surface=c.getHeight(existing?Heightmap.Types.WORLD_SURFACE:Heightmap.Types.WORLD_SURFACE_WG,x,z)-1;
    var b=fixture!=null?fixture:c.getNoiseBiome(p.getX()>>2,Math.max(y,surface)>>2,p.getZ()>>2);
    if(!b.is(OCEAN))continue;
    // Existing worlds only: require actual native SEA nearby, not merely an ocean label.
    if(existing&&!nearSea(c,p,surface))continue;
    list.add(p.immutable());
   }
  }
  return list;
 }
 private static boolean nearSea(ChunkAccess c,BlockPos p,int surface){
  if(seawater(c.getBlockState(new BlockPos(p.getX(),surface,p.getZ()))))return true;
  for(var d:Direction.values()){
   var q=p.relative(d);if(!c.getPos().equals(new ChunkPos(q))||q.getY()<c.getMinBuildHeight()||q.getY()>=c.getMaxBuildHeight())continue;
   if(NaturalFluids.kind(c.getFluidState(q).getType())==WaterKind.SEA)return true;
  }
  return false;
 }
 static void convertGenerated(ChunkAccess c,net.minecraft.core.Holder<Biome> fixture){
  for(var p:candidates(c,fixture,false))c.setBlockState(p,nativeState(c.getBlockState(p)),false);
 }
 private record Pending(ServerLevel level,ChunkPos pos){}
 private static final java.util.concurrent.ConcurrentLinkedQueue<Pending> QUEUE=new java.util.concurrent.ConcurrentLinkedQueue<>();
 static void chunkLoaded(ChunkEvent.Load e){
  if(e.getLevel() instanceof ServerLevel l&&e.getChunk() instanceof LevelChunk c&&NaturalWaterGeneration.enabled(l.getChunkSource().getGenerator())&&Arrays.stream(c.getSections()).anyMatch(s->s.maybeHas(MarinePlants::vanillaPlant)))QUEUE.add(new Pending(l,c.getPos()));
 }
 static void serverTick(ServerTickEvent.Post e){
  for(int n=0;n<4;n++){
   var work=QUEUE.poll();if(work==null)return;var l=work.level();if(l.getServer()!=e.getServer())continue;
   var c=l.getChunkSource().getChunkNow(work.pos().x,work.pos().z);if(c==null)continue;
   for(var p:candidates(c,null,true)){var old=l.getBlockState(p);if(vanillaPlant(old))l.setBlock(p,nativeState(old),2|16);}
  }
 }
 private static void require(boolean v,String msg){if(!v)throw new IllegalStateException("marine plants: "+msg);}
 static void verify(ServerLevel l){
  for(var b:blocks())require(b.defaultBlockState().getFluidState().getType()==sea().getType(),"native fluid "+b);
  require(Blocks.KELP.defaultBlockState().getFluidState().getType()==Fluids.WATER&&Blocks.SEAGRASS.defaultBlockState().getFluidState().getType()==Fluids.WATER,"vanilla freshwater unchanged");
  require(nativeState(Blocks.KELP.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE,12)).getValue(GrowingPlantHeadBlock.AGE)==12,"kelp age");
  require(nativeState(Blocks.TALL_SEAGRASS.defaultBlockState().setValue(TallSeagrassBlock.HALF,DoubleBlockHalf.UPPER)).getValue(TallSeagrassBlock.HALF)==DoubleBlockHalf.UPPER,"tall half");
  var p=new BlockPos(6,110,6);var saved=new LinkedHashMap<BlockPos,BlockState>();
  try{
   for(int y=-1;y<=4;y++){var q=p.offset(0,y,0);saved.put(q,l.getBlockState(q));l.setBlock(q,y==-1?Blocks.SAND.defaultBlockState():water(),2|16);}
   var head=KELP.get().defaultBlockState().setValue(GrowingPlantHeadBlock.AGE,0);l.setBlock(p,head,2|16);
   require(((BonemealableBlock)KELP.get()).isValidBonemealTarget(l,p,head),"kelp grows into SEA");
   ((BonemealableBlock)KELP.get()).performBonemeal(l,l.random,p,head);
   require(isPlant(l.getBlockState(p.above()))&&l.getFluidState(p.above()).getType()==sea().getType(),"grown kelp retains seawater");
   for(int y=0;y<=4;y++)l.setBlock(p.offset(0,y,0),water(),2|16);
   var grass=GRASS.get().defaultBlockState();l.setBlock(p,grass,2|16);
   ((BonemealableBlock)GRASS.get()).performBonemeal(l,l.random,p,grass);
   require(l.getBlockState(p).is(TALL.get())&&l.getBlockState(p.above()).is(TALL.get())&&l.getFluidState(p.above()).getType()==sea().getType(),"tall seagrass retains SEA");
   l.setBlock(p.above(),Blocks.WATER.defaultBlockState(),2|16);
   require(!((BonemealableBlock)GRASS.get()).isValidBonemealTarget(l,p,grass),"no growth into freshwater");
  }finally{saved.forEach((q,s)->l.setBlock(q,s,2|16));}
 }
}
