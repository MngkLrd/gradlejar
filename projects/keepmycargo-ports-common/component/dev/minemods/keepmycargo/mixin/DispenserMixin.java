package dev.minemods.keepmycargo.mixin;
import dev.minemods.keepmycargo.CargoTransfer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.BlockSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(DefaultDispenseItemBehavior.class)
public abstract class DispenserMixin {
    @Inject(method="dispense", at=@At("HEAD"))
    private void keepmycargo$begin(BlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (!(false)) CargoTransfer.beginPlacement(stack);
    }
    @Inject(method="dispense", at=@At("RETURN"))
    private void keepmycargo$end(BlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (!(false)) CargoTransfer.endPlacement();
    }
}
