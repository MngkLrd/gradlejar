package dev.minemods.riverweir.item;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.logic.WeirLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class NetRollItem extends Item {
    public NetRollItem(Properties properties){super(properties);}
    @Override public void appendHoverText(ItemStack stack,@Nullable Level level,List<Component> tooltip,TooltipFlag flag){
        tooltip.add(Component.translatable("tooltip.riverweir.dimensions").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.riverweir.capacity",stack.getCount(),Math.min(WeirLogic.MAX_SPAN,stack.getCount()*4)).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.riverweir.depth_rule").withStyle(ChatFormatting.DARK_GRAY));
    }
    @Override public InteractionResult useOn(UseOnContext context){
        Level level=context.getLevel();
        BlockPos pos=context.getClickedPos();
        if(!level.getBlockState(pos).is(RiverWeirMod.POST.get()))return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        Player player=context.getPlayer();
        if(player==null)return InteractionResult.FAIL;
        String key="RiverWeirFirst";
        String worldKey="RiverWeirDim";
        String dimension=level.dimension().location().toString();
        var data=player.getPersistentData();
        if(!data.contains(key)||!dimension.equals(data.getString(worldKey))){
            data.putLong(key,pos.asLong());
            data.putString(worldKey,dimension);
            player.displayClientMessage(Component.translatable("message.riverweir.first",WeirLogic.countRolls(player)),true);
            return InteractionResult.CONSUME;
        }
        BlockPos first=BlockPos.of(data.getLong(key));
        if(first.equals(pos)){
            data.remove(key);data.remove(worldKey);
            player.displayClientMessage(Component.translatable("message.riverweir.cancel"),true);
            return InteractionResult.CONSUME;
        }
        if(WeirLogic.connect((ServerLevel)level,first,pos,player)){
            data.remove(key);data.remove(worldKey);
        }
        return InteractionResult.CONSUME;
    }
}
