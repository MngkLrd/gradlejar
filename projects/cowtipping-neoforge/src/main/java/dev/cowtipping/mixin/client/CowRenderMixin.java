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
    @Inject(method="setupRotations",at=@At("TAIL"))
    private void cowtipping$lieOnSide(LivingEntity entity,PoseStack pose,
        float bob,float bodyYaw,float partialTick,float scale,CallbackInfo callback){
        if(!(entity instanceof Cow cow))return;
        float degrees=CowTipState.rotationDegrees(cow,partialTick);
        if(Math.abs(degrees)<0.01F)return;
        double pivotHeight=cow.isBaby()?-0.20D:-0.37D;
        pose.translate(0D,pivotHeight,0D);
        pose.mulPose(Axis.ZP.rotationDegrees(degrees));
        pose.translate(0D,-pivotHeight,0D);
    }
}
