package dev.minemods.halohaven;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class AetherTonicItem extends Item {
    public AetherTonicItem(Properties p) { super(p); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand) {
        ItemStack stack=p.getItemInHand(hand);
        if(level.isClientSide())return InteractionResultHolder.success(stack);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20*150, 0));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 20*120, 1));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20*100, 0));
        level.playSound(null,p.blockPosition(),SoundEvents.BREWING_STAND_BREW,SoundSource.PLAYERS,1f,1.6f);
        if(!p.getAbilities().instabuild)stack.shrink(1);
        p.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.success(stack);
    }
}
