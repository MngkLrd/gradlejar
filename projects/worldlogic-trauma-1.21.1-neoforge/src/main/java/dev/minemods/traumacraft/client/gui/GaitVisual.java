package dev.minemods.traumacraft.client.gui;
/** No movement changes: an asymmetric stride for the actual damaged leg. */
public final class GaitVisual {
    private GaitVisual(){}
    public static float stride(float function){return .20f+.80f*Math.max(0,Math.min(1,function));}
    public static float leg(float vanilla,float function,float phase,float amount){
        float loss=1-Math.max(0,Math.min(1,function));
        if(loss<.001f)return vanilla;
        return vanilla*stride(function)+.17f*loss*Math.min(1,Math.max(0,amount))*(float)Math.sin(phase*.6662f);
    }
}
