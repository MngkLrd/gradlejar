package dev.minemods.keepmycargo.mixin;
import dev.minemods.keepmycargo.CargoTransfer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MinecartItem.class)
public abstract class MinecartItemMixin {
    @Inject(method="useOn", at=@At("HEAD"))
    private void keepmycargo$begin(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel().isClientSide())) CargoTransfer.beginPlacement(context.getItemInHand());
    }
    @Inject(method="useOn", at=@At("RETURN"))
    private void keepmycargo$end(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel().isClientSide())) CargoTransfer.endPlacement();
    }
}
