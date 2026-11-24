package dev.minemods.keepmycargo.mixin;
import dev.minemods.keepmycargo.CargoTransfer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({ChestBoat.class, AbstractMinecartContainer.class})
public abstract class CargoDestroyMixin {
    @Inject(method = "destroy(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"), cancellable = true)
    private void keepmycargo$pack(DamageSource source, CallbackInfo ci) {
        if (CargoTransfer.packOnDestroy((Entity)(Object)this, source)) ci.cancel();
    }
}
