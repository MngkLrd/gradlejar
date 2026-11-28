package dev.minemods.thermal.content;
/** 8x8 cells, each covering two native texture pixels. Bit set = cleaned. */
public final class FiberMask {
    public static final long COMPLETE=-1L;
    private FiberMask(){}
    public static int index(double x,double z){
        if(!Double.isFinite(x)||!Double.isFinite(z)||x<0||x>1||z<0||z>1)return -1;
        return Math.min(7,(int)(z*8))*8+Math.min(7,(int)(x*8));
    }
    public static boolean clean(long mask,int cell){return cell>=0&&cell<64&&(mask&(1L<<cell))!=0;}
    public static long scrape(long mask,int cell,boolean tool){
        if(cell<0||cell>=64)return mask;
        long out=mask|(1L<<cell);
        if(tool){int x=cell%8,z=cell/8;
            if(x>0)out|=1L<<(cell-1);if(x<7)out|=1L<<(cell+1);
            if(z>0)out|=1L<<(cell-8);if(z<7)out|=1L<<(cell+8);
        }
        return out;
    }
    public static boolean finished(long mask){return mask==COMPLETE;}
    public static int progress(long mask){return Long.bitCount(mask);}
}
