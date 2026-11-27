package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.climb.ClimbRules;

/** Three stages: reach, pull, settle. Vanilla rigid limbs, independent anatomical sides. */
public final class ClimbVisual {
    private ClimbVisual(){}
    public static boolean uses(int mask,boolean left){return (mask&(left?ClimbRules.LEFT:ClimbRules.RIGHT))!=0;}
    public static float grip(float f){
        if(f<.18f)return (float)ClimbRules.ease(f/.18);
        if(f<.78f)return 1;
        return (float)(1-ClimbRules.ease((f-.78)/.22));
    }
    public static float arm(int mask,boolean left,float f){
        float g=grip(f);
        if(!uses(mask,left))return .22f*g;
        float pull=(float)ClimbRules.ease((f-.18)/.60);
        return (-2.05f+1.12f*pull)*g;
    }
    public static float lean(float f){return .34f*grip(f);}
    public static float leg(float f,boolean left){return (-.25f-.42f*(float)Math.sin(Math.PI*f))*(left?1:.86f)*grip(f);}
}
