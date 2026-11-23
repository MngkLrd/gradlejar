package com.kirill.simplesorter.client;

/** Deterministic label size and visibility regression tests without Forge. */
public final class FaceLabelDimensionsSelfTest {
    private FaceLabelDimensionsSelfTest() {}

    private static void require(boolean condition, String error) {
        if (!condition) throw new AssertionError(error);
    }

    public static void main(String[] args) {
        int assertions = 0;
        for (int pixels = 1; pixels <= 1000; pixels++) {
            for (boolean doubleChest : new boolean[]{false, true}) {
                float scale = FaceLabelDimensions.textScale(pixels, doubleChest);
                require(Float.isFinite(scale) && scale > 0F && scale <= 0.015F,
                        "Text scale must be smaller than the old 0.020 floating captions");
                require(scale * pixels <= FaceLabelDimensions.maxWidth(doubleChest) + 0.0001F,
                        "A long label cannot extend beyond the chest face");
                assertions += 2;
            }
        }
        require(FaceLabelDimensions.fitsVertically(false),
                "Text alone must fit within a chest front");
        require(FaceLabelDimensions.fitsVertically(true),
                "Text plus model must fit within a chest front");
        require(FaceLabelDimensions.labelDistanceFromCenter(false) > 0.65D,
                "TFC chest text must be clearly IN FRONT of protruding 3D models");
        require(FaceLabelDimensions.labelDistanceFromCenter(true) > 0.53D,
                "Vanilla chest text must sit outside its block front");
        require(FaceLabelDimensions.iconScale() < 0.30F,
                "Icon should remain a compact chest decoration");
        require(FaceLabelDimensions.withinRange(9.99D * 9.99D),
                "Inside 10 blocks the label must be eligible");
        require(!FaceLabelDimensions.withinRange(10D * 10D),
                "At 10 blocks the label must disappear");
        require(!FaceLabelDimensions.withinRange(10.01D * 10.01D),
                "Beyond 10 blocks the label must remain hidden");
        require(!FaceLabelDimensions.withinRange(1000D),
                "No long-distance label rendering");
        assertions += 9;

        for (int[] face : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            require(FaceLabelDimensions.isFrontVisible(-5D * face[0],
                            -5D * face[1], face[0], face[1]),
                    "A physical front face must be visible");
            require(!FaceLabelDimensions.isFrontVisible(5D * face[0],
                            5D * face[1], face[0], face[1]),
                    "Rear face must not bleed through the chest");
            assertions += 2;
        }
        System.out.println("FaceLabelDimensions: " + assertions +
                " caption scale, world-space depth, 10-block culling and face tests passed.");
    }
}
