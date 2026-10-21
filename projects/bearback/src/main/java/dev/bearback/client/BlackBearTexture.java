package dev.bearback.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;

public final class BlackBearTexture {
    private static final ResourceLocation VANILLA =
            ResourceLocation.withDefaultNamespace("textures/entity/bear/polarbear.png");
    private static ResourceLocation cached;

    private BlackBearTexture() {}

    public static ResourceLocation texture() {
        if (cached != null) return cached;
        Minecraft mc = Minecraft.getInstance();
        try (InputStream stream = mc.getResourceManager().getResourceOrThrow(VANILLA).open()) {
            NativeImage image = NativeImage.read(stream);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    int alpha = (abgr >>> 24) & 255;
                    if (alpha == 0) continue;
                    int r = abgr & 255, g = (abgr >>> 8) & 255, b = (abgr >>> 16) & 255;
                    int shade = Math.max(19, Math.min(78, 22 + ((r + g + b) / 3 - 100) / 4));
                    int rr = shade + 7, gg = shade + 4, bb = shade + 1;
                    image.setPixelRGBA(x, y, (alpha << 24) | (bb << 16) | (gg << 8) | rr);
                }
            }
            cached = mc.getTextureManager().register("bearback_black_bear", new DynamicTexture(image));
        } catch (Exception ex) {
            cached = VANILLA;
        }
        return cached;
    }
}
