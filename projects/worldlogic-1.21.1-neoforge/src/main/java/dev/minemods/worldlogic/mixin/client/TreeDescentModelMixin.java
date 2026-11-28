package dev.minemods.worldlogic.mixin.client;
import dev.minemods.worldlogic.TreeDescentSync;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Vanilla model proportions; original alternating hand/foot grip motion, no replacement skins. */
@Mixin(PlayerModel.class)
public abstract class TreeDescentModelMixin{
 @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
 private void worldlogic$descentPose(LivingEntity entity,float limbSwing,float limbAmount,float age,float yaw,float pitch,CallbackInfo ci){
  if(!(entity instanceof Player player)||!TreeDescentSync.clientGripping(player)||player.onGround()||!player.isShiftKeyDown())return;
  var model=(PlayerModel<?>)(Object)this;
  float phase=(float)Math.sin(age*.22f);
  model.leftArm.xRot=-1.52f+phase*.18f;model.rightArm.xRot=-1.52f-phase*.18f;
  model.leftArm.yRot=-.16f;model.rightArm.yRot=.16f;
  model.leftArm.zRot=-.09f;model.rightArm.zRot=.09f;
  model.leftLeg.xRot=-.23f-phase*.18f;model.rightLeg.xRot=-.23f+phase*.18f;
  model.leftLeg.zRot=-.10f;model.rightLeg.zRot=.10f;
  model.leftSleeve.copyFrom(model.leftArm);model.rightSleeve.copyFrom(model.rightArm);
  model.leftPants.copyFrom(model.leftLeg);model.rightPants.copyFrom(model.rightLeg);
 }
}
