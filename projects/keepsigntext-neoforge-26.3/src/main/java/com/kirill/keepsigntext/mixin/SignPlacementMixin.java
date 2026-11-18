package com.kirill.keepsigntext.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The 26.3 sign text and wax are direct item data components. */
@Mixin(BlockItem.class)
public abstract class SignPlacementMixin {
    @Inject(method="updateCustomBlockEntityTag(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)Z",
        at=@At("RETURN"), cancellable=true)
    private static void keepsigntext$restore(Level level, Player player, BlockPos pos, ItemStack stack,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return;
        var front = stack.get(DataComponents.SIGN_TEXT_FRONT);
        var back = stack.get(DataComponents.SIGN_TEXT_BACK);
        boolean waxed = stack.has(DataComponents.WAXED);
        if (front == null && back == null && !waxed) return;
        if (front != null) sign.setText(front, SignTextSlot.FRONT);
        if (back != null) sign.setText(back, SignTextSlot.BACK);
        sign.setWaxed(waxed);
        sign.setChanged();
        var state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
        cir.setReturnValue(true);
    }
}
