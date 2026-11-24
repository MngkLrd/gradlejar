package dev.minemods.keepmycargo.mixin;

import dev.minemods.keepmycargo.CargoTransfer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class CargoSlotMixin {
    @Shadow @Final public Container container;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void keepmycargo$noNesting(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (container instanceof ContainerEntity && CargoTransfer.containsPackedCargo(stack, 0)) {
            cir.setReturnValue(false);
        }
    }
}
