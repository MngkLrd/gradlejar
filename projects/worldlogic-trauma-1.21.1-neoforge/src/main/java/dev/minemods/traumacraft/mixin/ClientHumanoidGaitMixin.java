package dev.minemods.traumacraft.mixin;

import dev.minemods.traumacraft.client.ClientGaitState;
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
    // Before PlayerModel copies its leg transforms to the outer skin layers.
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void traumatic$gait(LivingEntity entity,float swing,float amount,float age,float yaw,float pitch,CallbackInfo ci){
        if(!(entity instanceof Player p)||!p.isAlive()||p.isCreative()||p.isSpectator()||p.isPassenger()||!p.onGround()||p.isSwimming()||p.isFallFlying()||p.isSleeping()||amount<.025f)return;
        leftLeg.xRot=GaitVisual.leg(leftLeg.xRot,ClientGaitState.leg(p,true),swing,amount);
        rightLeg.xRot=GaitVisual.leg(rightLeg.xRot,ClientGaitState.leg(p,false),swing+3.14159f,amount);
    }
}
