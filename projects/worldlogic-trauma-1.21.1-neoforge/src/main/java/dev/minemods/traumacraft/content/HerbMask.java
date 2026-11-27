package dev.minemods.traumacraft.content;
/** A 4x4 workpiece, each cell covers four native texture pixels. Set bit = crushed. */
public final class HerbMask {
    public static final int COMPLETE=0xffff;
    private HerbMask(){}
    public static int cell(double x,double z){
        if(!Double.isFinite(x)||!Double.isFinite(z)||x<0||x>1||z<0||z>1)return -1;
        return Math.min(3,(int)(z*4))*4+Math.min(3,(int)(x*4));
    }
    public static boolean done(int mask,int i){return i>=0&&i<16&&(mask&(1<<i))!=0;}
    public static int crush(int mask,int i,boolean tool){
        mask&=COMPLETE;
        if(i<0||i>=16||done(mask,i))return mask;
        int next=mask|(1<<i);
        if(tool){int x=i%4,z=i/4;if(x>0)next|=1<<(i-1);if(x<3)next|=1<<(i+1);if(z>0)next|=1<<(i-4);if(z<3)next|=1<<(i+4);}
        return next;
    }
    public static boolean finished(int mask){return (mask&COMPLETE)==COMPLETE;}
    public static int progress(int mask){return Integer.bitCount(mask&COMPLETE);}
}
