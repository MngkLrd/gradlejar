package dev.minemods.blowpipes.mixin.client;

import dev.minemods.blowpipes.BlowpipeItem;
import dev.minemods.blowpipes.client.BlowpipeAnimationCurves;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The third-person shoulder/elbow follows the head instead of TOOT_HORN.
 * Only affects players holding our blowpipes, never skeleton/zombie models.
 */
@Mixin(HumanoidModel.class)
public abstract class ThirdPersonMixin<T extends LivingEntity> {
    @Shadow @Final public ModelPart head;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void blowpipes$pose(T entity, float stride, float strideAmount,
                                 float age, float headYaw, float headPitch, CallbackInfo info) {
        if (!(entity instanceof Player player)) return;
        ItemStack active = player.getUseItem();
        boolean using = player.isUsingItem() && active.getItem() instanceof BlowpipeItem;
        InteractionHand hand = using ? player.getUsedItemHand() : InteractionHand.MAIN_HAND;
        ItemStack candidate = using ? active : player.getItemInHand(hand);
        if (!(candidate.getItem() instanceof BlowpipeItem)) return;

        float t = using ? player.getTicksUsingItem() : 0.0F;
        float attack = player.getAttackAnim(0.0F);
        float raised = using ? BlowpipeAnimationCurves.raise(t)
                : (player.swinging ? BlowpipeAnimationCurves.lower(attack * 8.0F) : 0.0F);
        if (raised <= 0.005F) return;
        float side = ((hand == InteractionHand.MAIN_HAND
                ? player.getMainArm() : player.getMainArm().getOpposite())
                == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        ModelPart arm = side > 0 ? rightArm : leftArm;
        float pitch = Mth.clamp(head.xRot, -0.8F, 0.8F);
        float yaw = Mth.clamp(head.yRot, -0.7F, 0.7F);
        float breath = using ? BlowpipeAnimationCurves.breath(t, age) : 0.0F;

        arm.xRot = Mth.lerp(raised, arm.xRot, -1.42F + pitch * 0.53F + breath);
        arm.yRot = Mth.lerp(raised, arm.yRot, -side * 0.26F + yaw * 0.30F);
        arm.zRot = Mth.lerp(raised, arm.zRot, side * 0.10F);
    }
}
