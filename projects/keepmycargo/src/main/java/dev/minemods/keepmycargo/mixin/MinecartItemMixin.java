package dev.minemods.keepmycargo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.minemods.keepmycargo.CargoTransfer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecartItem.class)
public abstract class MinecartItemMixin {
    @WrapMethod(method = "useOn")
    private InteractionResult keepmycargo$place(UseOnContext context, Operation<InteractionResult> original) {
        if (context.getLevel().isClientSide()) return original.call(context);
        CargoTransfer.beginPlacement(context.getItemInHand());
        try { return original.call(context); }
        finally { CargoTransfer.endPlacement(); }
    }
}
