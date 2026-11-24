package dev.minemods.keepmycargo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.minemods.keepmycargo.CargoTransfer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BoatItem.class)
public abstract class BoatItemMixin {
    @WrapMethod(method = "use")
    private InteractionResultHolder<ItemStack> keepmycargo$place(Level level, Player player, InteractionHand hand, Operation<InteractionResultHolder<ItemStack>> original) {
        if (level.isClientSide()) return original.call(level, player, hand);
        CargoTransfer.beginPlacement(player.getItemInHand(hand));
        try { return original.call(level, player, hand); }
        finally { CargoTransfer.endPlacement(); }
    }
}
