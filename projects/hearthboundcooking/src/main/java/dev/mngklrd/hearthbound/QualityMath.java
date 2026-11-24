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
        if(oldCount<=0)return quantize(newQuality);
        if(oldQuality<0)oldQuality=70;
        if(newCount<=0)return quantize(oldQuality);
        return quantize((int)Math.round(((double)oldQuality*oldCount+(double)newQuality*newCount)/(oldCount+newCount)));
    }
    public static int batches(int max,int water,int[] counts){
        if(max<=0||water<1000)return 0;
        int groups=0,n=Math.min(max,water/1000);
        for(int count:counts)if(count>0){groups++;n=Math.min(n,count);}
        return groups>=1?n:0;
    }
    public static float advanceHeat(float current,float target,float coefficient,int ticks){
        if(!Float.isFinite(current)||current<0)current=0;
        if(!Float.isFinite(target)||target<0)target=0;
        return (float)(target+(current-target)*Math.pow(1-coefficient,Math.max(0,ticks)));
    }
    public static float soupUnder(int exposure,int ideal){return Math.max(0,1-exposure/(float)Math.max(1,ideal))*80;}
    public static float soupOver(int exposure,int ideal){return Math.max(0,exposure/(float)Math.max(1,ideal)-1.25f)*55;}
    public static float soupHeat(int excess,int exposure){return Math.min(15,15f*Math.max(0,excess)/Math.max(1,exposure));}
    public static int stars(int q){return q>=95?5:q>=80?4:q>=60?3:q>=40?2:q>=20?1:0;}
    public static float saturationMultiplier(int quality){
        int q=clamp(quality);
        return q<=70?.8f+q*(.2f/70f):1f+(q-70)*(.1f/30f);
    }
}
