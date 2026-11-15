package dev.witchhunted.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ObserverEyeItem extends Item {
    public ObserverEyeItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this))
            return InteractionResultHolder.pass(stack);
        if (level instanceof ServerLevel serverLevel) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20*18, 0));
            serverLevel.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(22))
                .forEach(mob -> mob.addEffect(new MobEffectInstance(MobEffects.GLOWING,20*18,0)));
            player.getCooldowns().addCooldown(this,20*45);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide());
    }
}
