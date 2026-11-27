package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
/** Bounded climate-only lookup. No chunk loads and no live world access. */
public final class BiomeSurvey{
 private BiomeSurvey(){}
 private static final Set<net.minecraft.server.MinecraftServer> BUSY=Collections.newSetFromMap(new WeakHashMap<>());
 static int request(net.minecraft.commands.CommandSourceStack source,NoiseBasedChunkGenerator gen,RandomState state,BlockPos origin,ResourceLocation id){
  var server=source.getServer();
  if(!gen.getBiomeSource().possibleBiomes().stream().anyMatch(b->b.unwrapKey().map(k->k.location().equals(id)).orElse(false))){
   source.sendFailure(net.minecraft.network.chat.Component.literal("Biome is not in this generator: "+id));return 0;
  }
  if(!BUSY.add(server)){source.sendFailure(net.minecraft.network.chat.Component.literal("A biome survey is already running."));return 0;}
  java.util.concurrent.CompletableFuture.supplyAsync(()->find(gen,state,origin,id),net.minecraft.Util.backgroundExecutor())
   .whenComplete((result,error)->server.execute(()->{
    BUSY.remove(server);
    if(error!=null){source.sendFailure(net.minecraft.network.chat.Component.literal("Biome survey failed: "+error.getMessage()));return;}
    source.sendSuccess(()->net.minecraft.network.chat.Component.literal(result.map(p->"Predicted biome "+id+" at "+p.getX()+" "+p.getY()+" "+p.getZ()+" (sampled, not exact nearest)").orElse("No sampled match for "+id+" within 12288 blocks on the search grid. This does not prove absence.")),false);
   }));
  source.sendSuccess(()->net.minecraft.network.chat.Component.literal("Searching seeded climate grid without loading chunks."),false);return 1;
 }
 static int modelY(RandomState state,int x,int z){
  return (int)Math.round(128+128*state.router().depth().compute(new DensityFunction.SinglePointContext(x,128,z)));
 }
 static ResourceLocation at(ChunkGenerator gen,RandomState state,int x,int y,int z){
  return gen.getBiomeSource().getNoiseBiome(x>>2,y>>2,z>>2,state.sampler()).unwrapKey().orElseThrow().location();
 }
 static boolean supported(ChunkGenerator gen){
  return gen instanceof NoiseBasedChunkGenerator n&&n.generatorSettings().unwrapKey().map(k->k.location().equals(ResourceLocation.fromNamespaceAndPath("worldlogic","realistic_unified"))).orElse(false)&&!gen.getBiomeSource().possibleBiomes().isEmpty()&&gen.getBiomeSource().possibleBiomes().stream().allMatch(v->v.unwrapKey().map(k->k.location().getNamespace().equals("worldlogic")).orElse(false));
 }
 static int sampleY(RandomState s,int x,int z,ResourceLocation id){
  return switch(id.getPath()){case "deep_rock_cavern"->-180;case "limestone_cavern","humid_cavern"->modelY(s,x,z)-80;default->Math.max(63,modelY(s,x,z));};
 }
 static Optional<BlockPos> find(NoiseBasedChunkGenerator gen,RandomState state,BlockPos origin,ResourceLocation target){
  if(!supported(gen)||!gen.getBiomeSource().possibleBiomes().stream().anyMatch(b->b.unwrapKey().map(k->k.location().equals(target)).orElse(false)))return Optional.empty();
  // First try a local 9x9 grid; then broad search across latitude bands (25x25 km).
  // Return a sampled candidate rather than claiming exact nearest.
  for(int step:new int[]{256,3072})for(int ring=0;ring<=4;ring++)for(int dz=-ring;dz<=ring;dz++)for(int dx=-ring;dx<=ring;dx++){
   if(Math.max(Math.abs(dx),Math.abs(dz))!=ring)continue;
   int x=origin.getX()+dx*step,z=origin.getZ()+dz*step;
   if(Math.abs(x)>29900000||Math.abs(z)>29900000)continue;
   int y=sampleY(state,x,z,target);
   if(!at(gen,state,x,y,z).equals(target))continue;
   if(!target.getPath().endsWith("cavern")){
    y=Math.max(gen.getSeaLevel(),gen.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,LevelHeightAccessor.create(gen.getMinY(),gen.getGenDepth()),state));
    if(!at(gen,state,x,y,z).equals(target))continue;
   }
   return Optional.of(new BlockPos(x,y,z));
  }
  return Optional.empty();
 }
}
