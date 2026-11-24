package dev.mngklrd.hearthbound;

/** Pure arithmetic; no world, recipe, or item mutation. -1 means no prior grade. */
public final class QualityMath {
    private QualityMath(){}
    public static int clamp(int value){return Math.max(0,Math.min(100,value));}
    public static int quantize(int value){return clamp(Math.round(clamp(value)/5f)*5);}
    public static int finish(int process,int inherited){
        int p=clamp(process);
        return quantize(inherited<0?p:Math.round(p*.8f+clamp(inherited)*.2f));
    }
    public static int heating(float actual,float target){
        if(!Float.isFinite(actual)||!Float.isFinite(target)||target<=0||actual<target)return -1;
        return clamp(Math.round(100-Math.max(0,actual-target*1.15f)*.18f));
    }
    public static int merged(int oldQuality,int oldCount,int newQuality,int newCount){
        if(oldCount<=0||oldQuality<0)return quantize(newQuality);
        if(newCount<=0)return quantize(oldQuality);
        return quantize((int)Math.round(((double)oldQuality*oldCount+(double)newQuality*newCount)/(oldCount+newCount)));
    }
    public static float saturationMultiplier(int quality){
        int q=clamp(quality);
        return q<=70?.8f+q*(.2f/70f):1f+(q-70)*(.1f/30f);
    }
}
