package dev.minemods.traumacraft.client.gui;
import dev.minemods.traumacraft.integration.ThermalCondition;
/** Pure display mapping: bounded opacity and frame-rate-independent easing. */
public final class ThermalVisualProfile {
    private ThermalVisualProfile(){}
    public static float target(ThermalCondition c,boolean cold){return (cold?c.cold():c.hot())?c.severity()/3f:0f;}
    public static float approach(float value,float target,float seconds){
        float a=(float)(1-Math.exp(-3.0*Math.max(0,Math.min(.1f,seconds))));
        float result=value+(target-value)*a;
        return Math.abs(result-target)<.005f?target:result;
    }
    public static int alpha(float strength,int max){return Math.max(0,Math.min(max,Math.round(strength*max)));}
}
