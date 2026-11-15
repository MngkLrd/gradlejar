package dev.bearback.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;

/**
 * Reuses the Minecraft bear skin's pixel shapes without shipping Mojang texture assets.
 * Neutral charcoal fur, with a centered beige chest patch on the front torso UV.
 */
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
            int scaleX = Math.max(1, image.getWidth() / 128);
            int scaleY = Math.max(1, image.getHeight() / 64);

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    int alpha = (abgr >>> 24) & 255;
                    if (alpha == 0) continue;

                    int r = abgr & 255;
                    int g = (abgr >>> 8) & 255;
                    int b = (abgr >>> 16) & 255;
                    int luminance = (r * 30 + g * 59 + b * 11) / 100;
                    // Charcoal, never sepia: subtle neutral-grey highlights preserve fur details.
                    int shade = Math.min(47, Math.max(12, 12 + luminance / 9));
                    int rr = shade;
                    int gg = Math.min(255, shade + 1);
                    int bb = Math.min(255, shade + 2);

                    // Main body front / upper-face UV: soft cream marking under the neck.
                    // Scale coordinates for high-resolution replacement resource packs.
                    float u = (x + .5F) / scaleX;
                    float v = (y + .5F) / scaleY;
                    float dx = (u - 27.0F) / 4.4F;
                    float dy = (v - 29.6F) / 8.8F;
                    float distance = dx * dx + dy * dy;
                    if (u >= 22.0F && u < 32.0F && v >= 19.0F && v < 41.0F
                            && distance < 1.0F) {
                        // Hand-shaped cream/tan chest island, without smooth antialiasing.
                        int variation = ((x * 17 + y * 13) % 7) - 3;
                        boolean edge = distance > .64F;
                        rr = clamp((edge ? 157 : 202) + variation);
                        gg = clamp((edge ? 136 : 178) + variation);
                        bb = clamp((edge ? 109 : 140) + variation);
                    }
                    image.setPixelRGBA(x, y, (alpha << 24) | (bb << 16) | (gg << 8) | rr);
                }
            }

            cached = mc.getTextureManager().register("bearback_black_bear", new DynamicTexture(image));
        } catch (Exception ex) {
            // Do not fail entity rendering if another resource pack removes the vanilla skin.
            cached = VANILLA;
        }
        return cached;
    }

    private static int clamp(int color) {
        return Math.max(0, Math.min(255, color));
    }
}
