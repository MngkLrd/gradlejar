package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
/** Native seawater support for vanilla aquatic spawn predicates. No manual entity injection. */
final class MarineEcology {
 private static boolean nativeSea(LevelAccessor l,BlockPos p){
  var kind=NaturalFluids.kind(l.getFluidState(p).getType());
  return kind==WaterKind.SEA||kind==WaterKind.BRACKISH;
 }
 private static boolean submerged(LevelAccessor l,BlockPos p){
  return nativeSea(l,p)&&l.getFluidState(p.above()).is(FluidTags.WATER)&&l.getFluidState(p.below()).is(FluidTags.WATER);
 }
 private static boolean surface(LevelAccessor l,BlockPos p){int y=p.getY(),sea=l.getSeaLevel();return y>=sea-13&&y<=sea&&submerged(l,p);}
 static void registerSpawns(RegisterSpawnPlacementsEvent e){
  var or=RegisterSpawnPlacementsEvent.Operation.OR;
  e.register(EntityType.COD,(t,l,s,p,r)->submerged(l,p),or);
  e.register(EntityType.SALMON,(t,l,s,p,r)->submerged(l,p),or);
  e.register(EntityType.TROPICAL_FISH,(t,l,s,p,r)->submerged(l,p),or);
  e.register(EntityType.PUFFERFISH,(t,l,s,p,r)->submerged(l,p),or);
  e.register(EntityType.SQUID,(t,l,s,p,r)->surface(l,p),or);
  e.register(EntityType.DOLPHIN,(t,l,s,p,r)->surface(l,p),or);
 }
 private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("marine: "+detail);}
 static void verify(ServerLevel l){
  var registry=l.registryAccess().lookupOrThrow(Registries.BIOME);
  for(String id:List.of("polar_ocean","polar_deep_ocean","cold_ocean","cold_deep_ocean","temperate_ocean","temperate_deep_ocean","subtropical_ocean","warm_deep_ocean","tropical_ocean")){
   var b=registry.getOrThrow(ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogic",id))).value();
   var features=b.getGenerationSettings().features();
   require(features.get(6).stream().anyMatch(h->h.unwrapKey().map(k->k.location().toString().equals("minecraft:disk_clay")).orElse(false)),"sediment "+id);
   if(id.startsWith("polar"))require(features.get(2).stream().anyMatch(h->h.unwrapKey().map(k->k.location().toString().equals("minecraft:iceberg_packed")).orElse(false)),"polar icebergs "+id);
   require(!b.getMobSettings().getMobs(MobCategory.WATER_AMBIENT).isEmpty(),"fish inventory "+id);
   if(!id.startsWith("polar"))require(features.get(9).stream().anyMatch(h->h.unwrapKey().map(k->k.location().getPath().startsWith("seagrass_")).orElse(false)),"vanilla seagrass "+id);
  }
  BlockPos p=new BlockPos(4,l.getSeaLevel()-5,4);
  require(l.hasChunkAt(p),"loaded aquatic fixture");
  var above=l.getBlockState(p.above());var center=l.getBlockState(p);var below=l.getBlockState(p.below());
  try{
   var sea=NaturalFluids.get(WaterKind.SEA).block.get().defaultBlockState();
   for(var q:List.of(p,p.above(),p.below()))l.setBlock(q,sea,2);
   require(submerged(l,p)&&surface(l,p),"native seawater column");
   require(SpawnPlacements.checkSpawnRules(EntityType.COD,l,MobSpawnType.NATURAL,p,l.random),"actual registered cod predicate");
   require(SpawnPlacements.checkSpawnRules(EntityType.SQUID,l,MobSpawnType.NATURAL,p,l.random),"actual registered squid predicate");
   l.setBlock(p.above(),Blocks.AIR.defaultBlockState(),2);
   require(!submerged(l,p)&&!surface(l,p),"air negative control");
   l.setBlock(p.above(),Blocks.LAVA.defaultBlockState(),2);
   require(!submerged(l,p),"lava negative control");
  }finally{l.setBlock(p.above(),above,2);l.setBlock(p,center,2);l.setBlock(p.below(),below,2);}
 }
 /** Read actual loaded terrain, never generate a chunk for diagnostics. */
 static String audit(ServerLevel l,BlockPos origin){
  var chunk=l.getChunkSource().getChunkNow(origin.getX()>>4,origin.getZ()>>4);
  if(chunk==null)return "MARINE_AUDIT unavailable";
  int ocean=0,wet=0,wrongLand=0,sea=0,stone=0,sediment=0,dirt=0,habitat=0,plants=0;
  int level=l.getSeaLevel(),x0=chunk.getPos().getMinBlockX(),z0=chunk.getPos().getMinBlockZ();
  var p=new BlockPos.MutableBlockPos();
  for(int x=0;x<16;x++)for(int z=0;z<16;z++){
   int wx=x0+x,wz=z0+z;
   int h=chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR,x,z);
   p.set(wx,Math.max(level-1,h),wz);
   var key=l.getBiome(p).unwrapKey().orElseThrow().location();
   if(!key.getNamespace().equals("worldlogic")||!key.getPath().endsWith("ocean"))continue;
   ocean++;
   if(h>=level+4){wrongLand++;continue;}
   p.set(wx,level-1,wz);
   if(!chunk.getFluidState(p).is(FluidTags.WATER))continue;
   wet++;
   if(nativeSea(l,p))sea++;
   if(submerged(l,p.set(wx,level-5,wz)))habitat++;
   for(int yy=h+1;yy<level;yy++){var b=chunk.getBlockState(p.set(wx,yy,wz));if(b.is(Blocks.KELP)||b.is(Blocks.KELP_PLANT)||b.is(Blocks.SEAGRASS)||b.is(Blocks.TALL_SEAGRASS))plants++;}
   var bed=chunk.getBlockState(p.set(wx,h,wz));
   if(GeologyGeneration.rock(bed))stone++;
   else if(bed.is(Blocks.SAND)||bed.is(Blocks.GRAVEL)||bed.is(Blocks.CLAY)||bed.is(Blocks.RED_SAND))sediment++;
   else if(bed.is(Blocks.DIRT)||bed.is(Blocks.GRASS_BLOCK)||bed.is(Blocks.COARSE_DIRT))dirt++;
  }
  return "MARINE_AUDIT ocean="+ocean+" wet="+wet+" wrongLand="+wrongLand+" sea="+sea+" stone="+stone+" sediment="+sediment+" dirt="+dirt+" habitat="+habitat+" plants="+plants;
 }

}
