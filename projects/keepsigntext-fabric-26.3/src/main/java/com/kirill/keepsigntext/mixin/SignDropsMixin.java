package com.kirill.keepsigntext.mixin;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class SignDropsMixin {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;", at = @At("RETURN"))
    private static void keepsigntext$onDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity entity,
                                              CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!(entity instanceof SignBlockEntity sign)) return;
        for (ItemStack stack : cir.getReturnValue()) {
            if (stack.getItem() instanceof StandingAndWallBlockItem) {
                stack.set(DataComponents.SIGN_TEXT_FRONT, sign.getText(SignTextSlot.FRONT));
                stack.set(DataComponents.SIGN_TEXT_BACK, sign.getText(SignTextSlot.BACK));
                if (sign.isWaxed()) stack.set(DataComponents.WAXED, Unit.INSTANCE);
            }
        }
    }
}
