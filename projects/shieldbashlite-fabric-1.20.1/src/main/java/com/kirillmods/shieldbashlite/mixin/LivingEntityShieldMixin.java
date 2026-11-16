package com.kirillmods.shieldbashlite.mixin;

import com.kirillmods.shieldbashlite.ShieldBashLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Called ONLY after vanilla's directional shield-block check has returned.
 * Avoids the post-damage event, which is not present in Fabric API 1.20.1.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityShieldMixin {
    @Inject(method = "isDamageSourceBlocked", at = @At("RETURN"))
    private void shieldbashlite$afterShieldCheck(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && (Object) this instanceof ServerPlayer player) {
            ShieldBashLogic.onBlockedMelee(player, source);
        }
    }
}
