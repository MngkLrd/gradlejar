package dev.minemods.traumacraft.client.gui;
/** Bounded, slowly eased effects. No flicker, forced camera movement or shader state. */
public final class InjuryVisualProfile {
    private InjuryVisualProfile(){}
    public static float clamp(float x){return Float.isFinite(x)?Math.max(0,Math.min(1,x)):0;}
    public static float blood(float fraction){return clamp((.90f-fraction)/.70f);}
    public static float red(float pain,int wounds){return clamp(pain/100f*.65f+Math.min(4,Math.max(0,wounds))*.12f);}
    public static float dark(float blood,int shock,boolean concussion){return clamp(Math.max(blood(blood),Math.max(shock*.20f,concussion?.28f:0)));}
    public static float ease(float old,float goal,float dt){return ThermalVisualProfile.approach(old,goal,dt);}
}
