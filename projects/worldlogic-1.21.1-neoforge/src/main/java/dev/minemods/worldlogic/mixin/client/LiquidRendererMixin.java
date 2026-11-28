package dev.minemods.worldlogic.mixin.client;
import dev.minemods.worldlogic.WaterRenderCompatibility;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlockRenderer.class)
abstract class LiquidRendererMixin {
 @Inject(method="isNeighborSameFluid",at=@At("HEAD"),cancellable=true)
 private static void worldlogic$sharedFace(FluidState a,FluidState b,CallbackInfoReturnable<Boolean> cir){
  if(WaterRenderCompatibility.same(a.getType(),b.getType()))cir.setReturnValue(true);
 }
 @Redirect(method="getHeight",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/material/Fluid;isSame(Lnet/minecraft/world/level/material/Fluid;)Z"),require=0)
 private boolean worldlogic$continuousHeight(Fluid a,Fluid b){return WaterRenderCompatibility.same(a,b);}
}
