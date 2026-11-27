package dev.minemods.thermal;
/** One sample per second. Counts are gameplay tuning, not medical timing. */
public final class ThermalCondition {
    public enum Kind { NONE, HYPOTHERMIA, HEATSTROKE, SUNSTROKE }
    public record Progress(int coldSeconds,int hotSeconds,int sunSeconds,Kind kind) {
        public static Progress initial(){return new Progress(0,0,0,Kind.NONE);}
    }
    private ThermalCondition(){}
    private static int increment(int x){return Math.min(3600,Math.max(0,x)+1);}
    public static Progress advance(Progress old,ThermalModel.Stage stage,double sun,boolean immersed){
        boolean cold=stage==ThermalModel.Stage.HYPOTHERMIA, hot=stage==ThermalModel.Stage.HYPERTHERMIA;
        if(cold){
            int c=increment(old.coldSeconds());
            return new Progress(c,0,0,c>=30?Kind.HYPOTHERMIA:Kind.NONE);
        }
        if(hot){
            int h=increment(old.hotSeconds());
            int s=!immersed&&sun>=.55?increment(old.sunSeconds()):Math.max(0,old.sunSeconds()-2);
            Kind k=h<30?Kind.NONE:old.kind()==Kind.SUNSTROKE||s>=20?Kind.SUNSTROKE:Kind.HEATSTROKE;
            return new Progress(0,h,s,k);
        }
        return Progress.initial();
    }
    public static boolean pulse(Progress p){
        int n=p.kind()==Kind.HYPOTHERMIA?p.coldSeconds():p.hotSeconds();
        return p.kind()!=Kind.NONE && n>=30 && (n==30||n%10==0);
    }
    public static Kind parse(String value){
        try{return Kind.valueOf(value);}catch(IllegalArgumentException ex){return Kind.NONE;}
    }
}
