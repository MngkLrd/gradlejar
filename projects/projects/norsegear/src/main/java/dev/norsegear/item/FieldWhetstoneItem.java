package dev.norsegear.item;

import dev.norsegear.registry.ModTags;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Right-click with damaged Norse gear in the opposite hand. */
public final class FieldWhetstoneItem extends Item {
    public FieldWhetstoneItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tool = player.getItemInHand(hand);
        ItemStack target = player.getItemInHand(hand == InteractionHand.MAIN_HAND
            ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!target.is(ModTags.FIELD_REPAIRABLE) || !target.isDamageableItem() || !target.isDamaged()) {
            return InteractionResultHolder.pass(tool);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(tool);

        if (!level.isClientSide) {
            target.setDamageValue(Math.max(0, target.getDamageValue() - 20));
            if (!player.getAbilities().instabuild) tool.shrink(1);
            player.getCooldowns().addCooldown(this, 40);
            level.playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE,
                SoundSource.PLAYERS, 0.6F, 1.1F);
        }
        return InteractionResultHolder.sidedSuccess(tool, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.norsegear.field_whetstone"));
    }
}
