package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
/** Natural rock families and standard vanilla-shaped worked stone. */
public final class RockContent{
 private RockContent(){}
 public record Family(String id,float hardness,float blastResistance){}
 public static final List<Family> FAMILIES=List.of(new Family("limestone",1.35f,4.0f),new Family("shale",.95f,3.0f),new Family("slate",2.0f,5.5f),new Family("gneiss",3.3f,8.0f),new Family("volcanic_tuff",1.1f,3.5f));
 private static final Map<String,Supplier<Block>> BLOCKS=new LinkedHashMap<>();
 private static BlockBehaviour.Properties properties(Family f,Block reference){return BlockBehaviour.Properties.ofFullCopy(reference).strength(f.hardness,f.blastResistance).requiresCorrectToolForDrops();}
 private static void add(String id,Supplier<Block> factory){
  var b=Content.BLOCKS.register(id,factory);BLOCKS.put(id,b);Content.ITEMS.register(id,()->new BlockItem(b.get(),new Item.Properties()));
 }
 static{
  for(var f:FAMILIES){
   if(!f.id.equals("volcanic_tuff"))add(f.id,()->new Block(properties(f,Blocks.STONE)));
   add("polished_"+f.id,()->new Block(properties(f,Blocks.POLISHED_ANDESITE)));
   add(f.id+"_bricks",()->new Block(properties(f,Blocks.STONE_BRICKS)));
   for(String base:List.of(f.id,"polished_"+f.id,f.id+"_bricks")){
    add(base+"_slab",()->new SlabBlock(properties(f,Blocks.STONE_SLAB)));
    add(base+"_stairs",()->new StairBlock(block(base).defaultBlockState(),properties(f,Blocks.STONE_BRICK_STAIRS)));
    add(base+"_wall",()->new WallBlock(properties(f,Blocks.STONE_BRICK_WALL)));
   }
  }
 }
 static void register(){}
 public static Block block(String id){return id.equals("volcanic_tuff")?GroundMaterials.block(id):BLOCKS.get(id).get();}
 public static boolean natural(BlockState b){for(var f:FAMILIES)if(b.is(block(f.id)))return true;return false;}
 public static float hardness(BlockState b,float original){
  if(original<=0)return original;
  if(b.is(Blocks.STONE))return 2.0f;
  if(b.is(Blocks.DEEPSLATE))return 3.8f;
  if(b.is(Blocks.GRANITE))return 3.1f;
  if(b.is(Blocks.DIORITE))return 2.8f;
  if(b.is(Blocks.ANDESITE))return 2.4f;
  if(b.is(Blocks.CALCITE))return 1.15f;
  if(b.is(Blocks.TUFF))return 1.05f;
  if(b.is(Blocks.SANDSTONE)||b.is(Blocks.RED_SANDSTONE))return 1.25f;
  if(b.is(Blocks.BASALT)||b.is(Blocks.SMOOTH_BASALT))return 3.3f;
  return original;
 }
 /** Continuous folded sediment/metamorphic horizons, bounded to existing solid rock. */
 static Block host(GeologyModel.Province p,int y,int fold){
  int band=Math.floorMod(y+fold,64);
  return switch(p){
   case CARBONATE->band<22?block("limestone"):band<32?Blocks.CALCITE:band<38?block("shale"):null;
   case SEDIMENTARY->band<19?block("shale"):band<33?Blocks.SANDSTONE:band<42?block("limestone"):null;
   case INTRUSIVE->band<20?Blocks.GRANITE:band<30?Blocks.DIORITE:band<38?Blocks.ANDESITE:null;
   case METAMORPHIC->band<22?block("gneiss"):band<38?block("slate"):band<46?Blocks.DIORITE:null;
   case CRATON->y< -48&&band<24?block("gneiss"):band<12?Blocks.GRANITE:null;
  };
 }
 static void stratify(net.minecraft.world.level.chunk.ChunkAccess c,int x,int z,int top,GeologyModel.Province p,int fold){
  var pos=new net.minecraft.core.BlockPos.MutableBlockPos();
  for(int y=c.getMinBuildHeight()+8;y<=top;y++){
   Block host=host(p,y,fold);if(host==null)continue;
   var s=c.getBlockState(pos.set(x,y,z));
   if(s.is(Blocks.STONE)||s.is(Blocks.DEEPSLATE))c.setBlockState(pos,host.defaultBlockState(),false);
  }
 }
 static String audit(net.minecraft.server.level.ServerLevel l,net.minecraft.core.BlockPos origin){
  var c=l.getChunkSource().getChunkNow(origin.getX()>>4,origin.getZ()>>4);if(c==null)return "ROCK_AUDIT unavailable";
  var counts=new LinkedHashMap<String,Integer>();for(var f:FAMILIES)counts.put(f.id,0);
  var pos=new net.minecraft.core.BlockPos.MutableBlockPos();
  for(int x=2;x<16;x+=4)for(int z=2;z<16;z+=4){
   int top=c.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR,x,z)-6;
   for(int y=Math.max(c.getMinBuildHeight()+8,-240);y<=top;y+=4){
    var s=c.getBlockState(pos.set(c.getPos().getMinBlockX()+x,y,c.getPos().getMinBlockZ()+z));
    for(var f:FAMILIES)if(s.is(block(f.id)))counts.put(f.id,counts.get(f.id)+1);
   }
  }
  var out=new StringJoiner(" ","ROCK_AUDIT ","");counts.forEach((k,v)->out.add(k+"="+v));return out.toString();
 }
 public static void creative(BuildCreativeModeTabContentsEvent e){
  if(e.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS))for(var b:BLOCKS.values())e.accept(b.get());
  if(e.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS)){
   for(var f:FAMILIES)e.accept(block(f.id));
   for(String id:List.of("white_sand","volcanic_sand","chernozem","sandy_loam","laterite","volcanic_ash"))e.accept(GroundMaterials.block(id));
  }
 }
}
