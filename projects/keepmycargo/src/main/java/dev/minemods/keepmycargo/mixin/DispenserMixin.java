package dev.minemods.keepmycargo.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.minemods.keepmycargo.CargoTransfer;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DefaultDispenseItemBehavior.class)
public abstract class DispenserMixin {
    @WrapMethod(method = "dispense")
    private ItemStack keepmycargo$dispense(BlockSource source, ItemStack stack, Operation<ItemStack> original) {
        CargoTransfer.beginPlacement(stack);
        try { return original.call(source, stack); }
        finally { CargoTransfer.endPlacement(); }
    }
}
