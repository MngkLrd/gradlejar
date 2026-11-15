package dev.theothercursor.client;

import net.minecraft.client.gui.GuiGraphics;

final class GhostCursorArt {
    // A deliberate 11x15 silhouette modeled after a classic pixel cursor.
    // Painted from code: no missing texture assets, shader dependency, or OS cursor override.
    private static final String[] MASK = {
        "#..........",
        "##.........",
        "#o#........",
        "#oo#.......",
        "#ooo#......",
        "#oooo#.....",
        "#ooooo#....",
        "#oooooo#...",
        "#ooooooo#..",
        "#oooo#####.",
        "#oo#.#oo#..",
        "###..#oo#..",
        ".....#oo#..",
        "......##...",
        "..........."
    };

    private GhostCursorArt() {}

    static void draw(GuiGraphics graphics, int x, int y, boolean clutching, float phase) {
        int black = clutching ? 0xEE211126 : 0xE9000715;
        int white = clutching ? 0xF3E5C5F1 : 0xF2E8E8F5;
        int ghost = 0xA19E80D0;
        boolean afterimage = ((int) (phase * 1.4f) % 17 == 0);

        if (afterimage) {
            paint(graphics, x + 2, y + 1, 0x3C534477, 0x2858448B);
        }
        // Thin dark underlay makes the cursor visible on both light and dark UIs.
        paint(graphics, x + 1, y + 1, 0x73000006, 0x69000010);
        paint(graphics, x, y, black, white);
        // One near-invisible lavender pixel: the "other" pointer isn't quite normal.
        graphics.fill(x + 3, y + 4, x + 4, y + 5, ghost);
    }

    private static void paint(GuiGraphics graphics, int x, int y, int border, int fill) {
        for (int row = 0; row < MASK.length; row++) {
            String pixels = MASK[row];
            for (int col = 0; col < pixels.length(); col++) {
                char pixel = pixels.charAt(col);
                if (pixel != '.') {
                    graphics.fill(x + col, y + row, x + col + 1, y + row + 1,
                        pixel == '#' ? border : fill);
                }
            }
        }
    }
}
