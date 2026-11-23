package com.kirill.simplesorter.client;

/** Pure Java test: the full caption + model never exceeds one chest front. */
public final class FaceLabelDimensionsSelfTest {
    private FaceLabelDimensionsSelfTest() {}

    private static void require(boolean correct, String error) {
        if (!correct) throw new AssertionError(error);
    }

    public static void main(String[] args) {
        int assertions = 0;
        for (int width = 1; width <= 1000; width++) {
            for (boolean doubleChest : new boolean[]{false, true}) {
                float scale = FaceLabelDimensions.textScale(width, doubleChest);
                float max = FaceLabelDimensions.maxWidth(doubleChest);
                require(Float.isFinite(scale) && scale > 0F && scale <= 0.011F,
                        "Invalid caption scale " + scale);
                require(scale * width <= max + 0.0001F,
                        "Caption exceeds chest face: " + width + " px -> " + scale * width);
                require(max <= (doubleChest ? 1.60F : 0.82F),
                        "Caption may spill past neighbouring chest");
                assertions += 3;
            }
        }
        require(FaceLabelDimensions.fitsVertically(false),
                "Text-only label must fit vertically within chest");
        require(FaceLabelDimensions.fitsVertically(true),
                "Icon+text label must fit vertically within chest");
        require(FaceLabelDimensions.iconScale() < 0.30F,
                "Icon must be markedly smaller than the old oversized model");
        require(FaceLabelDimensions.textY(true) < FaceLabelDimensions.iconY(),
                "Icon must sit ABOVE text in one block-front region");
        require(Math.abs(FaceLabelDimensions.faceOffset(true)
                        + FaceLabelDimensions.textDepth() - 0.4375D) < 0.04D,
                "Vanilla chest text must sit almost flush with its inset face");
        require(Math.abs(FaceLabelDimensions.faceOffset(false)
                        + FaceLabelDimensions.textDepth() - 0.5D) < 0.04D,
                "TFC crate text must sit almost flush with its full-block face");
        require(FaceLabelDimensions.itemDepth() > FaceLabelDimensions.iconScale() / 2D,
                "Icon back should not clip the underlying chest texture");
        System.out.println("Face-mounted chest labels: " + (assertions + 7)
                + " sizing and vertical-extent checks passed.");
    }
}
