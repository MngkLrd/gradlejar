package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.GeologyGeneration;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ChunkGenerator.class)
public abstract class GeologyDecorationMixin {
 @Inject(method="applyBiomeDecoration",at=@At("HEAD"))
 private void worldlogic$marineBed(WorldGenLevel level,ChunkAccess chunk,StructureManager structures,CallbackInfo ci){
  var generator=(ChunkGenerator)(Object)this;
  if(GeologyGeneration.enabled(generator))dev.minemods.worldlogic.MarineTerrain.prepare(level.getSeed(),chunk,generator.getSeaLevel());
 }
 @Inject(method="applyBiomeDecoration",at=@At("RETURN"))
 private void worldlogic$deposits(WorldGenLevel level,ChunkAccess chunk,StructureManager structures,CallbackInfo ci){
  var generator=(ChunkGenerator)(Object)this;
  if(GeologyGeneration.enabled(generator))GeologyGeneration.mineralize(level.getSeed(),chunk);
  dev.minemods.worldlogic.NaturalWaterGeneration.convert(level,chunk,generator);
  if(level instanceof net.minecraft.server.level.WorldGenRegion region&&generator instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator noise)
   dev.minemods.worldlogic.ReefTerrain.decorate(noise,region.getLevel().getChunkSource().randomState(),region.registryAccess(),chunk,level.getSeed());
 }
}
