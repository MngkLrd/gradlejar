package dev.minemods.redstonevision.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.minemods.redstonevision.client.VisionState;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Experimental terrain ghosting. Altering entire chunk layers does not guarantee
 * 70% per-block transparency; avoid treating this as a finished X-ray renderer.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    private static boolean rv$modified;
    @Inject(method="renderSectionLayer",at=@At(
        value="INVOKE",target="Lnet/minecraft/client/renderer/RenderType;setupRenderState()V",
        shift=At.Shift.AFTER))
    private void rv$ghostTerrain(RenderType type,double x,double y,double z,
            Matrix4f frustum,Matrix4f projection,CallbackInfo ci) {
        if (!VisionState.active() || !rv$isTerrainLayer(type)) return;
        rv$modified=true;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1F,1F,1F,VisionState.TERRAIN_ALPHA);
    }
    @Inject(method="renderSectionLayer",at=@At("RETURN"))
    private void rv$restore(RenderType type,double x,double y,double z,
            Matrix4f frustum,Matrix4f projection,CallbackInfo ci) {
        if (rv$modified) {
            rv$modified=false;
            RenderSystem.setShaderColor(1F,1F,1F,1F);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }
    private static boolean rv$isTerrainLayer(RenderType type) {
        return type==RenderType.solid()||type==RenderType.cutout()||type==RenderType.cutoutMipped();
    }
}
