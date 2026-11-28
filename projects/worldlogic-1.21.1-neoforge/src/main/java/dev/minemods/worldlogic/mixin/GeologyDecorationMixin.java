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
 @Inject(method="applyBiomeDecoration",at=@At("RETURN"))
 private void worldlogic$deposits(WorldGenLevel level,ChunkAccess chunk,StructureManager structures,CallbackInfo ci){
  if(GeologyGeneration.enabled((ChunkGenerator)(Object)this))GeologyGeneration.mineralize(level.getSeed(),chunk);
 }
}
