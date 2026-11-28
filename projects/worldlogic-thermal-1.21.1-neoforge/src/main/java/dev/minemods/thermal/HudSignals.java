package dev.minemods.thermal;
/** Pure display thresholds shared with server regression checks. */
public final class HudSignals {
    private HudSignals(){}
    public static double jump(double before,double after){double d=after-before;return Double.isFinite(d)&&Math.abs(d)>=3?d:0;}
    public static String trend(double perMinute){return perMinute>=.5?"↑↑":perMinute<=-.5?"↓↓":perMinute>.02?"↑":perMinute<-.02?"↓":"=";}
    public static double bodyPosition(double c){return ClimateModel.clamp((c-30)/13,0,1);}
}
