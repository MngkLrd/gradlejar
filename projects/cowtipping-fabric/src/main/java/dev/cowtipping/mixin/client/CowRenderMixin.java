package dev.cowtipping.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.cowtipping.gameplay.CowTipState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class CowRenderMixin {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void cowtipping$lieOnSide(
            LivingEntity entity, PoseStack pose,
            float bob, float bodyYaw, float partialTick, float scale,
            CallbackInfo callback
    ) {
        if (!(entity instanceof Cow cow)) {
            return;
        }
        float degrees = CowTipState.rotationDegrees(cow, partialTick);
        if (Math.abs(degrees) < 0.01F) {
            return;
        }

        // setupRotations operates in world-oriented coordinates before the
        // vanilla model scale/translation. The pivot must be ABOVE the floor.
        // The original negative pivot put most of the sideways model in the
        // ground when the cow was tipped.
        double pivotHeight = cow.isBaby() ? 0.34D : 0.60D;
        double halfWidth = cow.isBaby() ? 0.32D : 0.55D;

        // During a roll, the legs on the lower side otherwise pass through
        // the ground. Lift by exactly the estimated below-ground extent,
        // leaving the vanilla cow resting on the block surface.
        double radians = Math.toRadians(degrees);
        double lowerSide = halfWidth * Math.abs(Math.sin(radians));
        double pivotClearance = pivotHeight * (1.0D - Math.cos(radians));
        double floorLift = Math.max(0.0D, lowerSide - pivotClearance);

        pose.translate(0.0D, floorLift, 0.0D);
        pose.translate(0.0D, pivotHeight, 0.0D);
        pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        pose.translate(0.0D, -pivotHeight, 0.0D);
    }
}
