package dev.minemods.traumacraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.minemods.traumacraft.client.ClientClimbState;
import dev.minemods.traumacraft.client.gui.ClimbVisual;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ClientClimbHandsMixin {
    @Shadow private void renderPlayerArm(PoseStack pose,MultiBufferSource buffers,int light,float equip,float swing,HumanoidArm arm){throw new AssertionError();}
    @Inject(method="renderHandsWithItems",at=@At("HEAD"),cancellable=true)
    private void traumatic$climbHands(float partial,PoseStack pose,MultiBufferSource.BufferSource buffers,LocalPlayer p,int light,CallbackInfo ci){
        var state=ClientClimbState.pose(p,partial);
        if(state==null)return;
        float grip=ClimbVisual.grip(state.progress());
        for(HumanoidArm arm:HumanoidArm.values()){
            boolean left=arm==HumanoidArm.LEFT;
            if(!ClimbVisual.uses(state.arms(),left))continue;
            pose.pushPose();
            pose.translate(left?.08:-.08,.22*grip,-.16*grip);
            pose.mulPose(Axis.XP.rotationDegrees(-24*grip+15*state.progress()));
            renderPlayerArm(pose,buffers,light,1-grip,0,arm);
            pose.popPose();
        }
        buffers.endBatch();
        ci.cancel();
    }
}
