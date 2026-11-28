package dev.minemods.aquaculture.mixin;
import dev.minemods.aquaculture.FishWater;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.WaterAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WaterAnimal.class)
public abstract class FishAirMixin {
 @Inject(method="handleAirSupply(I)V",at=@At("HEAD"),cancellable=true)
 private void worldlogic_aqua$nativeWaterAir(int previous,CallbackInfo ci){
  if((Object)this instanceof AbstractFish fish&&fish.isAlive()&&FishWater.contact(fish)){
   fish.setAirSupply(fish.getMaxAirSupply());
   ci.cancel();
  }
 }
}
