package dev.minemods.aquaculture.mixin;

import dev.minemods.aquaculture.FishWater;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.WaterAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One server-side air transaction per fish tick, using actual fluid contact. */
@Mixin(WaterAnimal.class)
public abstract class FishAirMixin {
 @Inject(method="handleAirSupply(I)V",at=@At("HEAD"),cancellable=true)
 private void worldlogic_aqua$nativeWaterAir(int previous,CallbackInfo ci){
  if(!((Object)this instanceof AbstractFish fish)||fish.level().isClientSide)return;
  ci.cancel();
  if(!fish.isAlive())return;
  if(FishWater.contact(fish)){
   fish.setAirSupply(fish.getMaxAirSupply());
   return;
  }
  int remaining=Math.min(previous,fish.getMaxAirSupply())-1;
  if(remaining<=-20){
   fish.setAirSupply(0);
   fish.hurt(fish.damageSources().drown(),2.0f);
  }else fish.setAirSupply(remaining);
 }
}
