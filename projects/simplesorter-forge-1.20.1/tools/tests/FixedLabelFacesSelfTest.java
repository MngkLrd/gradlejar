package com.kirill.simplesorter.client;

/**
 * Regression for TFC 14-column warehouses: labels cannot point into chest,
 * at sideways edges, or through a blocked neighbor.
 * Pure Java 17, no client graphics context required.
 */
public final class FixedLabelFacesSelfTest {
    private FixedLabelFacesSelfTest() {}

    private static void check(boolean correct, String text) {
        if (!correct) throw new AssertionError(text);
    }

    public static void main(String[] args) {
        int tests = 0;
        int[][] data = {
                {FixedLabelFaces.NORTH, 0, -1},
                {FixedLabelFaces.SOUTH, 0, 1},
                {FixedLabelFaces.EAST, 1, 0},
                {FixedLabelFaces.WEST, -1, 0}
        };
        for (int[] each : data) {
            int side = each[0];
            int nx = each[1], nz = each[2];
            int picked = FixedLabelFaces.bestFace(nx * 4D, nz * 4D,
                    true, true, true, true);
            check(picked == side, "Wrong visible face for cardinal viewer " + side);
            int rejected = FixedLabelFaces.bestFace(nx * 4D, nz * 4D,
                    side != FixedLabelFaces.NORTH,
                    side != FixedLabelFaces.SOUTH,
                    side != FixedLabelFaces.EAST,
                    side != FixedLabelFaces.WEST);
            check(rejected == -1,
                    "A closed chest face must not display through an adjacent solid wall");
            double radians = Math.toRadians(FixedLabelFaces.glyphRotationDegrees(side));
            // After negative X/Y scaling, font text's front points +Z.
            // Rotation about world Y sends +Z to (sin(rotation), cos(rotation)).
            check(Math.abs(Math.sin(radians) - nx) < 0.00001D
                    && Math.abs(Math.cos(radians) - nz) < 0.00001D,
                    "Font glyph normal turned inward for cardinal face " + side);
            tests += 3;
        }

        // Many chests at X=-20..20, with the user standing at (0,-3).
        // Even far lateral columns must put their names on NORTH (the
        // exposed front), not EAST/WEST based on an individual chest offset.
        for (int col = -20; col <= 20; col++) {
            int front = FixedLabelFaces.bestFace(-col, -3D,
                    true, false, false, false);
            check(front == FixedLabelFaces.NORTH,
                    "Lateral chest " + col + " lost front-facing text");
            tests++;
        }
        check(FixedLabelFaces.bestFace(0D, -2D, false, false, false, false) == -1,
                "Fully surrounded crate should produce no floating text");
        check(FixedLabelFaces.isCardinal(FixedLabelFaces.EAST)
                && !FixedLabelFaces.isCardinal(-1),
                "Invalid face IDs must be ignored");
        tests += 2;

        System.out.println("FixedLabelFaces: " + tests +
                " static-surface, 14-column and font-normal cases passed.");
    }
}
