package com.kirill.simplesorter.client;

/**
 * Sized static chest-front labels. Previous "near flush" text was inside
 * TerraFirmaCraft's extended 3D chest models even when icons were visible.
 * Text now sits safely IN FRONT of their geometry, not inside it.
 */
public final class FaceLabelDimensions {
    private static final float SINGLE_WIDTH = 0.90F;
    private static final float DOUBLE_WIDTH = 1.75F;
    private static final float MAX_TEXT_SCALE = 0.015F;
    public static final double DISPLAY_RADIUS = 10D;
    public static final double DISPLAY_RADIUS_SQ = DISPLAY_RADIUS * DISPLAY_RADIUS;

    private FaceLabelDimensions() {}

    public static float maxWidth(boolean doubleChest) {
        return doubleChest ? DOUBLE_WIDTH : SINGLE_WIDTH;
    }

    public static float textScale(int pixelWidth, boolean doubleChest) {
        return Math.min(MAX_TEXT_SCALE,
                maxWidth(doubleChest) / Math.max(1, pixelWidth));
    }

    public static float textY(boolean withIcon) {
        return withIcon ? -0.22F : 0.0F;
    }

    public static float iconY() { return 0.15F; }
    public static float iconScale() { return 0.28F; }

    /**
     * Use an outward static surface position, not the block voxel shape:
     * TFC/Greg crates commonly have 3D detailing extending past 0.5.
     * At the earlier 0.526-block text depth, text was occluded by their fronts,
     * while the 3D model at about 0.65 blocks rendered correctly.
     */
    public static double labelDistanceFromCenter(boolean vanillaChest) {
        return vanillaChest ? 0.575D : 0.690D;
    }

    public static double textDepth() { return 0.012D; }
    public static double itemDepth() { return 0.055D; }

    public static boolean withinRange(double squaredDistance) {
        return squaredDistance < DISPLAY_RADIUS_SQ;
    }

    public static boolean isFrontVisible(double chestMinusCameraX,
                                         double chestMinusCameraZ,
                                         int normalX, int normalZ) {
        return chestMinusCameraX * normalX
                + chestMinusCameraZ * normalZ < -0.16D;
    }

    public static boolean fitsVertically(boolean withIcon) {
        float textHeight = 9F * MAX_TEXT_SCALE;
        boolean textFits = Math.abs(textY(withIcon)) + textHeight * 0.5F <= 0.44F;
        boolean iconFits = !withIcon || iconY() + iconScale() * 0.5F <= 0.44F;
        return textFits && iconFits;
    }
}
