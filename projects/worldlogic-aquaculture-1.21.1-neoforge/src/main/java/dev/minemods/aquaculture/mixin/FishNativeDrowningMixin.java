package dev.minemods.aquaculture.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.neoforged.neoforge.fluids.FluidType;
import dev.minemods.aquaculture.FishWater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Restore the air buffer before NeoForge's fluid-type drowning check, not after it. */
@Mixin(LivingEntity.class)
public abstract class FishNativeDrowningMixin {
 @Inject(method="baseTick()V",at=@At("HEAD"))
 private void worldlogic_aqua$refreshWaterAir(CallbackInfo ci){
  if((Object)this instanceof AbstractFish fish&&fish.isAlive()&&FishWater.contact(fish))fish.setAirSupply(fish.getMaxAirSupply());
 }
}
