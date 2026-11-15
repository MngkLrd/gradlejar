package dev.norsegear.item;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Multiplayer-safe, scoreboard team-based rally effect with cooldown. */
public final class WarHornItem extends Item {
    public WarHornItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (level instanceof ServerLevel server) {
            for (ServerPlayer target : server.players()) {
                if (target.distanceToSqr(player) > 100.0D) continue;
                if (target != player && !player.isAlliedTo(target)) continue;
                target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 140, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 140, 0));
            }
            player.getCooldowns().addCooldown(this, 900);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            server.playSound(null, player.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(),
                SoundSource.PLAYERS, 1.3F, 0.95F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.norsegear.war_horn"));
    }
}
