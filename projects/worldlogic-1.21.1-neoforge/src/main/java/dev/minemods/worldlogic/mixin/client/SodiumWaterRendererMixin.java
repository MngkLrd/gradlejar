package dev.minemods.worldlogic.mixin.client;
import dev.minemods.worldlogic.WaterRenderCompatibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Sodium 0.6.13 / MC 1.21.1 render path; absent Sodium never loads its classes. */
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer",remap=false)
abstract class SodiumWaterRendererMixin {
 @Inject(method="isFullBlockFluidOccluded",at=@At("HEAD"),cancellable=true,require=0,remap=false)
 private void worldlogic$sharedFace(BlockAndTintGetter world,BlockPos pos,Direction dir,BlockState block,FluidState fluid,CallbackInfoReturnable<Boolean> cir){
  var neighbor=world.getFluidState(pos.relative(dir));
  if(!neighbor.isEmpty()&&WaterRenderCompatibility.same(fluid.getType(),neighbor.getType()))cir.setReturnValue(true);
 }
 @Redirect(method="fluidHeight",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/material/Fluid;isSame(Lnet/minecraft/world/level/material/Fluid;)Z",remap=false),require=0,remap=false)
 private boolean worldlogic$continuousHeight(Fluid a,Fluid b){return WaterRenderCompatibility.same(a,b);}
}
