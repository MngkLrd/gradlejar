package dev.christmasui.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LogoRenderer.class)
public abstract class LogoRendererMixin {
    private static final ResourceLocation CHRISTMAS_LOGO = ResourceLocation.fromNamespaceAndPath(
            "christmasui", "textures/gui/christmas_logo.png");

    @Inject(method = "renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IFI)V", at = @At("HEAD"), cancellable = true)
    private void christmasui$renderLogo(GuiGraphics graphics, int screenWidth, float alpha, int y, CallbackInfo ci) {
        if (!(Minecraft.getInstance().screen instanceof TitleScreen title)) return;
        int top = Math.max(4, y - 10);
        int maxHeight = Math.max(16, title.height / 4 + 48 - top - 10);
        int w = Math.max(1, Math.min(272, Math.min(screenWidth - 12, maxHeight * 512 / 147)));
        int h = Math.max(1, Math.round(w * 147f / 512f));
        RenderSystem.enableBlend();
        graphics.setColor(1f, 1f, 1f, alpha);
        graphics.blit(CHRISTMAS_LOGO, (screenWidth - w) / 2, top, w, h, 0f, 0f, 512, 147, 512, 147);
        graphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        ci.cancel();
    }
}
