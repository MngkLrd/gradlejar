package com.kirill.keepsigntext.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Apply the saved text even when vanilla disallows custom block entity NBT for non-operators. */
@Mixin(SignItem.class)
public abstract class SignPlacementMixin {
    @Inject(method="updateCustomBlockEntityTag(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;)Z",
        at=@At("HEAD"), cancellable=true)
    private void keepsigntext$restore(BlockPos pos, Level level, Player player, ItemStack stack, BlockState state,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide()) return;
        TypedEntityData<?> saved = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (saved == null) return;
        var tag = saved.copyTagWithoutId();
        if (!tag.contains("front_text") && !tag.contains("back_text")) return;
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return;
        sign.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag.copy()));
        sign.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        cir.setReturnValue(true);
    }
}
