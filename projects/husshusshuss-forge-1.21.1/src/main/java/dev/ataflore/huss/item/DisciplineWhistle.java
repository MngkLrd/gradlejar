package dev.ataflore.huss.item;

import dev.ataflore.huss.training.Training;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

public final class DisciplineWhistle extends Item {
    public DisciplineWhistle(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) Training.whistle(server, player, null);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
