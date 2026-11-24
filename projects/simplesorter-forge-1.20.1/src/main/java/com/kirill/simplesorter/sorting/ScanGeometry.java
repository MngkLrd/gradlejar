package com.kirill.simplesorter.sorting;

/** Square horizontal preview footprint, with equal vertical reach. */
public final class ScanGeometry {
    private ScanGeometry() {}
    public static boolean contains(int dx,int dy,int dz,int radius) {
        return Math.abs((long)dx)<=radius && Math.abs((long)dy)<=radius && Math.abs((long)dz)<=radius;
    }
}
