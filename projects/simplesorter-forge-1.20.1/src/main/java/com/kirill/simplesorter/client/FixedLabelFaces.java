package com.kirill.simplesorter.client;

/**
 * Deterministic geometry for placing a label on an actual cardinal block face.
 *
 * This is NOT a billboard: the selected surfaces are existing N/S/E/W faces
 * of the block, never a plane continuously rotating toward the camera.
 * An adjacent chest or a solid wall may close one or more sides. The display
 * uses the visible exposed face, including the reverse face when a player
 * walks behind a row of modded crates.
 *
 * 0=NORTH, 1=SOUTH, 2=EAST, 3=WEST, -1=not visible.
 */
public final class FixedLabelFaces {
    public static final int NORTH = 0;
    public static final int SOUTH = 1;
    public static final int EAST = 2;
    public static final int WEST = 3;

    private FixedLabelFaces() {}

    public static int bestFace(double viewerX, double viewerZ,
                               boolean northOpen, boolean southOpen,
                               boolean eastOpen, boolean westOpen) {
        double best = 0.12D;
        int selected = -1;
        if (northOpen && -viewerZ > best) { best = -viewerZ; selected = NORTH; }
        if (southOpen && viewerZ > best) { best = viewerZ; selected = SOUTH; }
        if (eastOpen && viewerX > best) { best = viewerX; selected = EAST; }
        if (westOpen && -viewerX > best) { selected = WEST; }
        return selected;
    }

    public static boolean isCardinal(int side) {
        return side >= NORTH && side <= WEST;
    }
}
