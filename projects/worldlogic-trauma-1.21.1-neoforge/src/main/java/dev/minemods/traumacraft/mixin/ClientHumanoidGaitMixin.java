package dev.minemods.traumacraft.mixin;

import dev.minemods.traumacraft.client.ClientGaitState;
import dev.minemods.traumacraft.client.ClientClimbState;
import dev.minemods.traumacraft.client.gui.ClimbVisual;
import dev.minemods.traumacraft.client.gui.GaitVisual;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class ClientHumanoidGaitMixin {
    @Shadow public ModelPart leftLeg;
    @Shadow public ModelPart rightLeg;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart body;
    // Before PlayerModel copies its leg transforms to the outer skin layers.
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void traumatic$gait(LivingEntity entity,float swing,float amount,float age,float yaw,float pitch,CallbackInfo ci){
        if(entity instanceof Player climber){
            var pose=ClientClimbState.pose(climber,age-(float)Math.floor(age));
            if(pose!=null){
                float f=pose.progress();int mask=pose.arms();
                leftArm.xRot=ClimbVisual.arm(mask,true,f);rightArm.xRot=ClimbVisual.arm(mask,false,f);
                leftArm.yRot=0;rightArm.yRot=0;leftArm.zRot=ClimbVisual.uses(mask,true)?-.07f:.12f;rightArm.zRot=ClimbVisual.uses(mask,false)?.07f:-.12f;
                body.xRot=ClimbVisual.lean(f);body.zRot=(mask==1?-.10f:mask==2?.10f:0)*ClimbVisual.grip(f);
                leftLeg.xRot=ClimbVisual.leg(f,true);rightLeg.xRot=ClimbVisual.leg(f,false);
                return;
            }
        }
        if(!(entity instanceof Player p)||!p.isAlive()||p.isCreative()||p.isSpectator()||p.isPassenger()||!p.onGround()||p.isSwimming()||p.isFallFlying()||p.isSleeping()||amount<.025f)return;
        leftLeg.xRot=GaitVisual.leg(leftLeg.xRot,ClientGaitState.leg(p,true),swing,amount);
        rightLeg.xRot=GaitVisual.leg(rightLeg.xRot,ClientGaitState.leg(p,false),swing+3.14159f,amount);
    }
}
