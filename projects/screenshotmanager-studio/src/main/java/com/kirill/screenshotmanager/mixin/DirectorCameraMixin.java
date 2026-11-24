package com.kirill.screenshotmanager.mixin;
import com.kirill.screenshotmanager.client.Director;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class DirectorCameraMixin {
    @Shadow protected abstract void setPosition(double x,double y,double z);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Shadow private boolean detached;
    @Inject(method="setup",at=@At("RETURN"))
    private void screenshotmanager$director(BlockGetter level,Entity entity,boolean thirdPerson,boolean inverse,float partialTick,CallbackInfo ci){
        var pose=Director.currentPose();
        if(pose==null)return;
        setPosition(pose.x(),pose.y(),pose.z());
        setRotation(pose.yaw(),pose.pitch());
        detached=true;
    }
}
