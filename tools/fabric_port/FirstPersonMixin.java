package dev.minemods.blowpipes.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.minemods.blowpipes.BlowpipeItem;
import dev.minemods.blowpipes.client.BlowpipeAnimationCurves;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Animates the entire hand+held 3D blowpipe together; works with resource packs
 * because Minecraft continues to render the actual skin arm and baked item model.
 * No new render layer or custom packet needed. Both side and offhand supported.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void blowpipes$begin(AbstractClientPlayer player, float partialTick, float pitch,
                                  InteractionHand hand, float swing, ItemStack stack,
                                  float equip, PoseStack poses, MultiBufferSource buffers,
                                  int light, CallbackInfo ci) {
        if (!(stack.getItem() instanceof BlowpipeItem)) return;
        poses.pushPose();
        float sign = ((hand == InteractionHand.MAIN_HAND
                ? player.getMainArm() : player.getMainArm().getOpposite())
                == HumanoidArm.RIGHT) ? 1.0F : -1.0F;
        boolean using = player.isUsingItem() && player.getUsedItemHand() == hand
                && player.getUseItem().is(stack.getItem());
        float t = using ? player.getTicksUsingItem() + partialTick : 0.0F;
        float recoveryTime = player.getAttackAnim(partialTick) * 8.0F;
        float lift = using ? BlowpipeAnimationCurves.raise(t)
                : (player.swinging ? BlowpipeAnimationCurves.lower(recoveryTime) : 0.0F);
        float recoil = !using && player.swinging ? BlowpipeAnimationCurves.impulse(recoveryTime) : 0.0F;
        float preparation = using ? BlowpipeAnimationCurves.prepare(t) : 0.0F;
        float inhale = using ? BlowpipeAnimationCurves.inhale(t) : 0.0F;
        float breathing = using ? BlowpipeAnimationCurves.breath(t, player.tickCount + partialTick) : 0.0F;

        // The hand follows the pipe into a sightline near the avatar's mouth.
        // Delay and easing avoid instantly teleporting the object at use start.
        poses.translate(-sign * (0.22F * lift + 0.02F * inhale),
                0.35F * lift - preparation * 0.35F + breathing - 0.025F * recoil,
                -0.28F * lift + 0.06F * recoil);
        poses.mulPose(Axis.XP.rotationDegrees(-20.0F * lift + 5.0F * recoil));
        poses.mulPose(Axis.YP.rotationDegrees(-sign * 14.0F * lift));
        poses.mulPose(Axis.ZP.rotationDegrees(-sign * 16.0F * lift));
        poses.translate(sign * (0.03F * lift), -0.02F * lift, 0.0F);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void blowpipes$end(AbstractClientPlayer player, float partialTick, float pitch,
                                InteractionHand hand, float swing, ItemStack stack,
                                float equip, PoseStack poses, MultiBufferSource buffers,
                                int light, CallbackInfo ci) {
        if (stack.getItem() instanceof BlowpipeItem) poses.popPose();
    }
}
