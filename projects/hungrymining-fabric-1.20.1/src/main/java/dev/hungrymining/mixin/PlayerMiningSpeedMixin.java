package dev.hungrymining.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adjusts the vanilla block-breaking numerator after tools, enchantments,
 * potion effects, water and other vanilla modifiers have been applied.
 * This method is evaluated by client prediction and by the logical server.
 */
@Mixin(Player.class)
public abstract class PlayerMiningSpeedMixin {
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void hungrymining$modifyMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return;
        }
        int food = Math.max(0, Math.min(20, player.getFoodData().getFoodLevel()));
        float multiplier;
        if (food >= 18) {
            multiplier = 1.08F;
        } else {
            // At food=17 the multiplier is 1.0; at food=0 it is 0.3.
            multiplier = 0.30F + 0.70F * food / 17.0F;
        }
        if (multiplier != 1.0F) {
            cir.setReturnValue(cir.getReturnValueF() * multiplier);
        }
    }
}
