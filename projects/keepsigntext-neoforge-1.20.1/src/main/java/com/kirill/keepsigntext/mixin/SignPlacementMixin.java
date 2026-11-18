package com.kirill.keepsigntext.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.nbt.CompoundTag;

@Mixin(SignItem.class)
public abstract class SignPlacementMixin {
    @Inject(method="updateCustomBlockEntityTag(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;)Z",
        at=@At("HEAD"), cancellable=true)
    private void keepsigntext$restore(BlockPos pos, Level level, Player player, ItemStack stack,
                                       BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide) return;
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag == null || (!tag.contains("front_text") && !tag.contains("back_text"))) return;
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return;
        sign.load(tag.copy());
        sign.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        cir.setReturnValue(true);
    }
}
