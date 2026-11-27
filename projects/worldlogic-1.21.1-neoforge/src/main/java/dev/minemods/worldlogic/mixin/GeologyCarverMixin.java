package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.GeologyGeneration;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class GeologyCarverMixin {
 @Inject(method="applyCarvers",at=@At("HEAD"),cancellable=true)
 private void worldlogic$geologicalCaves(WorldGenRegion region,long seed,RandomState random,BiomeManager biomes,StructureManager structures,ChunkAccess chunk,GenerationStep.Carving step,CallbackInfo ci){
  if(!GeologyGeneration.enabled((ChunkGenerator)(Object)this))return;
  if(step==GenerationStep.Carving.AIR){
   dev.minemods.worldlogic.PhysicalBiomeReconciliation.apply((NoiseBasedChunkGenerator)(Object)this,random,chunk);
   GeologyGeneration.carve(seed,chunk);
   dev.minemods.worldlogic.RiverTerrain.prepare((NoiseBasedChunkGenerator)(Object)this,random,region.registryAccess(),chunk);
  }
  ci.cancel();
 }
}
