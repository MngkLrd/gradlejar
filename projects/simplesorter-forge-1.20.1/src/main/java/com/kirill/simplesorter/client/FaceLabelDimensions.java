package com.kirill.simplesorter.client;

/**
 * Pixel-to-world sizing rules for one chest-face decal. Plain Java to test
 * on CI independently of Forge and graphical Minecraft.
 */
public final class FaceLabelDimensions {
    private static final float SINGLE_WIDTH = 0.78F;
    private static final float DOUBLE_WIDTH = 1.53F;
    private static final float MAX_TEXT_SCALE = 0.011F;

    private FaceLabelDimensions() {}

    public static float maxWidth(boolean doubleChest) {
        return doubleChest ? DOUBLE_WIDTH : SINGLE_WIDTH;
    }

    public static float textScale(int pixelWidth, boolean doubleChest) {
        return Math.min(MAX_TEXT_SCALE,
                maxWidth(doubleChest) / Math.max(1, pixelWidth));
    }

    public static float textY(boolean withIcon) {
        return withIcon ? -0.235F : 0.0F;
    }

    public static float iconY() { return 0.145F; }

    public static float iconScale() { return 0.275F; }

    public static boolean fitsVertically(boolean withIcon) {
        float textHeight = 9F * MAX_TEXT_SCALE;
        boolean textFits = Math.abs(textY(withIcon)) + textHeight <= 0.44F;
        boolean iconFits = !withIcon || iconY() + iconScale() * 0.5F <= 0.44F;
        return textFits && iconFits;
    }
}
