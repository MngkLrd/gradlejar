package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.NaturalFluids;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** NeoForge 1.21.1 legacy WATER queries only inspect its vanilla FluidType.
 * Keep our registered types and extend those compatibility queries, both sides. */
@Mixin(Entity.class)
public abstract class NativeWaterEntityMixin {
 private Entity worldlogic$self(){return (Entity)(Object)this;}
 private double worldlogic$depth(){
  double h=0;
  for(var f:NaturalFluids.families())h=Math.max(h,worldlogic$self().getFluidTypeHeight(f.type.get()));
  return h;
 }
 @Inject(method="updateFluidHeightAndDoFluidPushing(Lnet/minecraft/tags/TagKey;D)Z",at=@At("RETURN"),cancellable=true)
 private void worldlogic$waterContact(TagKey<Fluid> tag,double motion,CallbackInfoReturnable<Boolean> cir){
  if(tag==FluidTags.WATER&&!cir.getReturnValue()&&worldlogic$depth()>0)cir.setReturnValue(true);
 }
 @Inject(method="getFluidHeight",at=@At("RETURN"),cancellable=true)
 private void worldlogic$waterHeight(TagKey<Fluid> tag,CallbackInfoReturnable<Double> cir){
  if(tag==FluidTags.WATER)cir.setReturnValue(Math.max(cir.getReturnValue(),worldlogic$depth()));
 }
 @Inject(method="isEyeInFluid",at=@At("RETURN"),cancellable=true)
 private void worldlogic$waterEyes(TagKey<Fluid> tag,CallbackInfoReturnable<Boolean> cir){
  if(tag!=FluidTags.WATER||cir.getReturnValue())return;
  for(var f:NaturalFluids.families())if(worldlogic$self().isEyeInFluidType(f.type.get())){cir.setReturnValue(true);return;}
 }
}
