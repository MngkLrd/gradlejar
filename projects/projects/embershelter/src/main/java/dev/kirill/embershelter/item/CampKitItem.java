package dev.kirill.embershelter.item;

import dev.kirill.embershelter.camp.CampBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class CampKitItem extends Item {
    public CampKitItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != net.minecraft.core.Direction.UP) {
            return InteractionResult.FAIL;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.FAIL;
        }
        BlockPos center = context.getClickedPos().above();
        boolean started = CampBuilder.start(level, player, center, context.getItemInHand());
        return started ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
}
