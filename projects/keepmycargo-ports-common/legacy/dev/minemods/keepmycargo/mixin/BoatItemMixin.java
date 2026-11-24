package dev.minemods.keepmycargo.mixin;
import dev.minemods.keepmycargo.CargoTransfer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BoatItem.class)
public abstract class BoatItemMixin {
    @Inject(method="use", at=@At("HEAD"))
    private void keepmycargo$begin(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!(level.isClientSide())) CargoTransfer.beginPlacement(player.getItemInHand(hand));
    }
    @Inject(method="use", at=@At("RETURN"))
    private void keepmycargo$end(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!(level.isClientSide())) CargoTransfer.endPlacement();
    }
}
