package dev.minemods.treechopper;

/** Grid measured in model pixels; the bevel is intentionally stepped at 2px. */
public final class CutGeometry {
    private CutGeometry() {}
    public static int shallow(int depth) { return Math.max(0,depth-1); }
    public static boolean falls(int n,int s,int w,int e) {
        return Math.max(Math.max(n,s),Math.max(w,e))>=8
            || (n>0 && s>0 && n+s>=6) || (w>0 && e>0 && w+e>=6)
            || (n+s+w+e>=8);
    }
    public static int remaining(int n,int s,int w,int e) {
        return Math.max(0,8-n-s)*Math.max(0,8-w-e);
    }
}
