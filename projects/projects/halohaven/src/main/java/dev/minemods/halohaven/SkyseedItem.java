package dev.minemods.halohaven;

import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Crafted Eye-of-Ender / feather ritual makes an ascending island oasis. */
public final class SkyseedItem extends Item {
    public SkyseedItem(Properties properties){super(properties);}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack held=player.getItemInHand(hand);
        if(!(player instanceof ServerPlayer sp))return InteractionResultHolder.success(held);
        boolean started=SkyOasis.begin(sp);
        if(started){
            if(!sp.getAbilities().instabuild)held.shrink(1);
            sp.getCooldowns().addCooldown(this,90);
        }
        return InteractionResultHolder.success(held);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag){
        tooltip.add(Component.translatable("tooltip.halohaven.skyseed").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.halohaven.skyseed_bell").withStyle(ChatFormatting.GRAY));
    }
}
