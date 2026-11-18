package com.kirill.keepsigntext.mixin;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class SignDropsMixin {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", at = @At("RETURN"))
    private static void keepsigntext$onDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity entity,
                                              Entity breaker, ItemStack tool, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!(entity instanceof SignBlockEntity sign)) return;
        var data = CustomData.of(sign.saveWithFullMetadata(level.registryAccess()));
        for (ItemStack stack : cir.getReturnValue()) {
            if (stack.getItem() instanceof StandingAndWallBlockItem) {
                stack.set(DataComponents.BLOCK_ENTITY_DATA, data);
            }
        }
    }
}
