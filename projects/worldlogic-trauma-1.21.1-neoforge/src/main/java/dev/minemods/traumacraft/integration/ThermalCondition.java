package dev.minemods.traumacraft.integration;

/** Explicit wire IDs; Thermal owns the body-temperature simulation. */
public enum ThermalCondition {
    NONE(0,1f), COMFORTABLE(1,1f), COLD(2,.8f), VERY_COLD(3,.5f), HYPOTHERMIA(4,0f),
    WARM(5,.8f), VERY_HOT(6,.5f), HYPERTHERMIA(7,0f);
    public final int wireId;
    public final float recoveryFactor;
    ThermalCondition(int wireId,float recoveryFactor){this.wireId=wireId;this.recoveryFactor=recoveryFactor;}
    public boolean cold(){return this==COLD||this==VERY_COLD||this==HYPOTHERMIA;}
    public boolean hot(){return this==WARM||this==VERY_HOT||this==HYPERTHERMIA;}
    public boolean stressed(){return cold()||hot();}
    public static ThermalCondition fromWire(int id){
        for(var s:values())if(s.wireId==id)return s;
        return NONE;
    }
    public static ThermalCondition read(boolean installed,boolean active,String stage,double body){
        if(!installed||!active||!Double.isFinite(body)||body<30||body>43||stage==null)return NONE;
        try{return valueOf(stage);}catch(IllegalArgumentException ex){return NONE;}
    }
    public int recoveryTicks(int ticks){return Math.max(0,Math.round(ticks*recoveryFactor));}
}
