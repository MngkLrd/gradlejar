package dev.minemods.traumacraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.minemods.traumacraft.client.ClientClimbState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class ClientClimbCameraMixin {
    @Inject(method="bobView",at=@At("HEAD"),cancellable=true)
    private void traumatic$noWalkingBob(PoseStack pose,float partial,CallbackInfo ci){
        var mc=Minecraft.getInstance();
        if(mc.getCameraEntity() instanceof net.minecraft.world.entity.player.Player p&&ClientClimbState.pose(p,partial)!=null)ci.cancel();
    }
}
