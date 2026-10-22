package dev.minemods.leashwrap.mixin;

import dev.minemods.leashwrap.AnchorLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla only accepts fences; preserve its original behavior on all other blocks. */
@Mixin(LeashFenceKnotEntity.class)
public abstract class LeashKnotMixin {
    @Inject(method = "survives", at = @At("HEAD"), cancellable = true)
    private void leashwrap$acceptSolidBlocks(CallbackInfoReturnable<Boolean> cir) {
        LeashFenceKnotEntity knot = (LeashFenceKnotEntity) (Object) this;
        BlockPos pos = BlockPos.containing(knot.getX(), knot.getY(), knot.getZ());
        if (AnchorLogic.canAnchor(knot.level(), pos)) {
            cir.setReturnValue(true);
        }
    }
}
