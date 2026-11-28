package dev.minemods.worldlogic.mixin.client;
import dev.minemods.worldlogic.WaterRenderCompatibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Sodium 0.6.13 common occlusion path. Existing solid-face culling remains intact. */
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache",remap=false)
abstract class SodiumWaterOcclusionMixin {
 @Inject(method="shouldDrawFullBlockFluidSide",at=@At("HEAD"),cancellable=true,require=0,remap=false)
 private void worldlogic$hideWaterInterface(BlockState self,BlockGetter view,BlockPos pos,Direction face,FluidState fluid,VoxelShape shape,CallbackInfoReturnable<Boolean> cir){
  if(WaterRenderCompatibility.hidesAdjacentFace(fluid,view.getBlockState(pos.relative(face))))cir.setReturnValue(false);
 }
}
