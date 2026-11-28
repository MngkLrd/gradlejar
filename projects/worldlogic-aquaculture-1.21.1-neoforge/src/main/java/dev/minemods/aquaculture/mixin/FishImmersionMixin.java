package dev.minemods.aquaculture.mixin;
import dev.minemods.aquaculture.FishWater;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class FishImmersionMixin {
 @Inject(method="isInWater()Z",at=@At("RETURN"),cancellable=true)
 private void worldlogic_aqua$water(CallbackInfoReturnable<Boolean> cir){
  if(!cir.getReturnValue()&&(Object)this instanceof AbstractFish fish&&FishWater.contact(fish))cir.setReturnValue(true);
 }
 @Inject(method="isInWaterOrBubble()Z",at=@At("RETURN"),cancellable=true)
 private void worldlogic_aqua$waterOrBubble(CallbackInfoReturnable<Boolean> cir){
  if(!cir.getReturnValue()&&(Object)this instanceof AbstractFish fish&&FishWater.contact(fish))cir.setReturnValue(true);
 }
}
