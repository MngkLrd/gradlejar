package dev.minemods.halohaven;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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

/**
 * Permanent post-boss trinket: activation invokes the Sunward Ward
 * (slow falling, absorption, fire resistance), 90 second cooldown.
 */
public final class SunwardRelicItem extends Item {
    public SunwardRelicItem(Properties props){super(props);}
    @Override
    public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack item=player.getItemInHand(hand);
        if(level.isClientSide())return InteractionResultHolder.success(item);
        if(!(player instanceof ServerPlayer p))return InteractionResultHolder.pass(item);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,20*120,0,false,true));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,20*90,2,false,true));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,20*75,0,false,true));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,20*12,1,false,true));
        p.getCooldowns().addCooldown(this,20*90);
        level.playSound(null,p.blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.PLAYERS,1.2f,1.5f);
        ((ServerLevel)level).sendParticles(ParticleTypes.END_ROD,p.getX(),p.getY()+0.9,p.getZ(),
            85,1,1.3,1,0.08);
        return InteractionResultHolder.success(item);
    }
}
