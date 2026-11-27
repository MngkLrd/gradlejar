package dev.minemods.thermal.client;
public final class ThermalColors {
    private static final int[] STOPS={0xff5483c4,0xff75b1e2,0xffa4cfe5,0xff83c17b,0xffe6ce78,0xffe99151,0xffd64b40};
    private ThermalColors(){}
    public static int gradient(double v){
        double t=Math.max(0,Math.min(1,v))*6;int i=Math.min(5,(int)t);double f=t-i;int a=STOPS[i],b=STOPS[i+1];
        return 0xff000000|mix(a,b,16,f)<<16|mix(a,b,8,f)<<8|mix(a,b,0,f);
    }
    private static int mix(int a,int b,int shift,double f){return (int)Math.round(((a>>shift)&255)*(1-f)+((b>>shift)&255)*f);}
    public static int trend(double v){return v>.12?0xffe99151:v<-.12?0xff75b1e2:0xffb6b9bc;}
    public static int darkTrend(double v){return v>.12?0xffa54a18:v<-.12?0xff255f99:0xff555555;}
    public static String arrow(double v){return v>.12?"↑":v<-.12?"↓":"=";}
    public static int air(double c){return c<10?0xff245e96:c>32?0xff9f3522:c>25?0xff99601c:0xff385e39;}
    public static int body(double c){return gradient(Math.max(0,Math.min(1,(c-30)/13)));}
    public static int marker(int stage){return STOPS[Math.max(0,Math.min(6,stage))];}
}
