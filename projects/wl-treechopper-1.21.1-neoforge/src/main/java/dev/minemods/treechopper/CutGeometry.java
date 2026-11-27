package dev.minemods.treechopper;

/** Grid measured in model pixels; the bevel is intentionally stepped at 4px. */
public final class CutGeometry {
    private CutGeometry() {}
    public static int shallow(int depth) { return Math.max(0,depth-1); }
    public static boolean falls(int n,int s,int w,int e) {
        return Math.max(Math.max(n,s),Math.max(w,e))>=4
            || (n>0 && s>0 && n+s>=3) || (w>0 && e>0 && w+e>=3)
            || (n+s+w+e>=5);
    }
    public static int remaining(int n,int s,int w,int e) {
        return Math.max(0,4-n-s)*Math.max(0,4-w-e);
    }
}
