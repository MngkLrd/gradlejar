package dev.minemods.worldlogicstructures;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.minemods.worldlogic.api.WorldLogicApi;
import dev.minemods.worldlogicstructures.api.StructuresApi;
import dev.minemods.worldlogicstructures.api.StructuresApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import java.util.*;

public final class RegionalStructure extends Structure {
 public static final MapCodec<RegionalStructure> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
  settingsCodec(i),StringRepresentable.fromEnum(Family::values).fieldOf("family").forGetter(s->s.family)
 ).apply(i,RegionalStructure::new));
 private final Family family;
 public RegionalStructure(StructureSettings settings,Family family){super(settings);this.family=family;}
 @Override public StructureType<?> type(){return StructuresMod.REGIONAL.get();}
 private static int height(GenerationContext c,int x,int z,Heightmap.Types type){
  return c.chunkGenerator().getBaseHeight(x,z,type,c.heightAccessor(),c.randomState());
 }
 @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
  var sampler=WorldLogicApi.worldgenSampler(c.randomState(),c.registryAccess(),c.chunkGenerator());
  if(sampler.isEmpty())return Optional.empty();
  int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ();
  int y=height(c,x,z,Heightmap.Types.OCEAN_FLOOR_WG);
  int surface=height(c,x,z,Heightmap.Types.WORLD_SURFACE_WG);
  if(y<=c.heightAccessor().getMinBuildHeight()+8||surface-y>2)return Optional.empty();
  BlockPos center=new BlockPos(x,y,z);
  var biome=c.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(y),QuartPos.fromBlock(z),c.randomState().sampler());
  var ground=c.chunkGenerator().getBaseColumn(x,z,c.heightAccessor(),c.randomState()).getBlock(y-1);
  var habitat=sampler.get().sample(center,biome,ground);
  if(StructuresApi.CATALOGUE.family(habitat).orElse(null)!=family||!Double.isFinite(habitat.slopeDegrees())||habitat.slopeDegrees()>22)return Optional.empty();
  var designs=StructuresApi.CATALOGUE.designs(family);
  int start=c.random().nextInt(designs.size()),turn=c.random().nextInt(4);
  long seed=c.random().nextLong();
  boolean warm=habitat.temperatureCelsius()>=20;
  Map<Long,Column> columns=new HashMap<>();
  // One template and a bounded second choice; rotate to find a viable approach.
  for(int n=0;n<Math.min(2,designs.size());n++){
   Design d=designs.get((start+n)%designs.size());
   for(int j=0;j<4;j++){
    Rotation rotation=Rotation.values()[(turn+j)%4];
    var floor=site(c,x,z,d,rotation,columns);
    if(floor.isEmpty())continue;
    int fy=floor.getAsInt();
    return Optional.of(new GenerationStub(new BlockPos(x,fy,z),b->b.addPiece(new ArchitecturePiece(d,x,fy,z,rotation,seed,warm))));
   }
  }
  return Optional.empty();
 }
 private record Column(int bottom,int top){}
 static OptionalInt site(GenerationContext c,int x,int z,Design d,Rotation rotation,Map<Long,Column> columns){
  int w=d.halfWidth(),l=d.halfDepth(),max=Integer.MIN_VALUE,min=Integer.MAX_VALUE;
  boolean shallow=false;
  // Check the whole building, eaves and access envelope; never flatten terrain.
  int front=l+d.raised()+2;
  for(int a=-w-1;a<=w+1;a++)for(int b=-l-1;b<=front;b++){
   if(b>l+1&&Math.abs(a)>1)continue;
   BlockPos offset=Architecture.transform(a,0,b,rotation);
   int wx=x+offset.getX(),wz=z+offset.getZ();
   var column=columns.computeIfAbsent((((long)wx)<<32)^(wz&0xffffffffL),k->new Column(height(c,wx,wz,Heightmap.Types.OCEAN_FLOOR_WG),height(c,wx,wz,Heightmap.Types.WORLD_SURFACE_WG)));
   int bottom=column.bottom(),top=column.top();
   boolean access=b>=l+1&&Math.abs(a)<=1;
   int water=top-bottom;
   if(water>0){
    if(access||d.raised()==0||water>2)return OptionalInt.empty();
    shallow=true;
   }
   min=Math.min(min,bottom);max=Math.max(max,top);
   if(max-min>2)return OptionalInt.empty();
  }
  if(d.kind()==Kind.PLATFORM&&!shallow)return OptionalInt.empty();
  int floor=max+d.raised();
  if(floor-7<c.heightAccessor().getMinBuildHeight()||floor+Architecture.maxHeight(d)>=c.heightAccessor().getMaxBuildHeight())return OptionalInt.empty();
  return OptionalInt.of(floor);
 }
}
