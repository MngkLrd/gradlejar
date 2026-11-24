package dev.worldfold.fold;

/** Geometry and load calculation are intentionally independent of game state. */
public final class SpatialMath {
    public static final int[] WIDTHS={3,5,9,15}, HEIGHTS={3,5,7,9};
    public static final double[] SCALES={1,0.5,2};
    private SpatialMath() {}
    public static int cost(double area,double distance,boolean dimensions,double ratio,boolean branch) {
        return 12+(int)Math.ceil(area/3.0)+(int)Math.min(48,Math.log1p(distance)*3)
            +(dimensions?28:0)+(ratio==1?0:22)+(branch?14:0);
    }
    public static int capacity(int stabilizers) { return 48+18*Math.max(0,Math.min(stabilizers,12)); }
    public static int nextStrain(int current,int load,int capacity) {
        int change=load>capacity?Math.max(1,(load-capacity)/8):-3;
        return Math.max(0,Math.min(100,current+change));
    }
    public static void main(String[] args) {
        if(cost(9,30,false,1,false)>=capacity(0)) throw new AssertionError("starter seam");
        if(cost(135,10000,true,2,true)<=capacity(0)) throw new AssertionError("large seam");
        if(nextStrain(99,150,48)!=100||nextStrain(1,10,48)!=0) throw new AssertionError("strain clamps");
        if(capacity(100)!=264||capacity(-1)!=48) throw new AssertionError("capacity clamp");
        System.out.println("SpatialMath invariants OK");
    }
}
