package dev.minemods.riverweir.item;

import dev.minemods.riverweir.logic.WeirLogic;
import dev.minemods.riverweir.RiverWeirMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class NetRollItem extends Item {
    public NetRollItem(Properties properties) {super(properties);}
    @Override public InteractionResult useOn(UseOnContext context) {
        Level level=context.getLevel();
        BlockPos pos=context.getClickedPos();
        if (!level.getBlockState(pos).is(RiverWeirMod.POST.get())) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player=context.getPlayer();
        if (player==null) return InteractionResult.FAIL;
        ItemStack stack=context.getItemInHand();
        CompoundTag tag=stack.getOrCreateTag();
        String dimension=level.dimension().location().toString();
        if (!tag.contains("FirstPost") || !dimension.equals(tag.getString("FirstDimension"))) {
            tag.putLong("FirstPost",pos.asLong());
            tag.putString("FirstDimension",dimension);
            player.displayClientMessage(Component.translatable("message.riverweir.first"),true);
            return InteractionResult.CONSUME;
        }
        BlockPos first=BlockPos.of(tag.getLong("FirstPost"));
        if (first.equals(pos)) {
            tag.remove("FirstPost");
            tag.remove("FirstDimension");
            player.displayClientMessage(Component.translatable("message.riverweir.cancel"),true);
            return InteractionResult.CONSUME;
        }
        boolean placed=WeirLogic.connect((ServerLevel)level,first,pos,player);
        if (placed) {
            tag.remove("FirstPost");tag.remove("FirstDimension");
        }
        return InteractionResult.CONSUME;
    }
}
