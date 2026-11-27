package dev.minemods.traumacraft.climb;

/** Anatomical requirements shared by the authoritative climb and client input. */
public final class ClimbRules {
    public static final int LEFT=1, RIGHT=2, BOTH=3;
    private ClimbRules(){}
    public static boolean needs(boolean lf,boolean rf,float left,float right){
        return lf||rf||left<.70f||right<.70f;
    }
    public static int arms(boolean lf,boolean rf,float left,float right){
        return (!lf&&left>=.40f?LEFT:0)|(!rf&&right>=.40f?RIGHT:0);
    }
    public static int duration(int mask,float left,float right,int shock){
        float power=mask==BOTH?(left+right)*.5f:mask==LEFT?left:right;
        return Math.min(80,Math.max(20,Math.round((mask==BOTH?26:46)/Math.max(.4f,power)*(1+.18f*shock))));
    }
    public static double rise(double progress){return ease((progress-.10)/.52);}
    public static double forward(double progress){return ease((progress-.62)/.38);}
    /** Never rewind or jump phase because a packet arrived early/late. */
    public static float advance(float current,float target,int duration){
        return Math.min(duration,current+Math.max(.75f,Math.min(1.25f,1f+.20f*(target-current-1f))));
    }
    public static double ease(double f){f=Math.max(0,Math.min(1,f));return f*f*(3-2*f);}
}
