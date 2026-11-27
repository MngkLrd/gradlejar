package dev.minemods.traumacraft.integration;

/** Explicit wire IDs; Thermal owns the body-temperature simulation. */
public enum ThermalCondition {
    NONE(0,1f), COMFORTABLE(1,1f), COLD(2,.8f), VERY_COLD(3,.5f), HYPOTHERMIA(4,0f),
    WARM(5,.8f), VERY_HOT(6,.5f), HYPERTHERMIA(7,0f), HEATSTROKE(8,0f), SUNSTROKE(9,0f), CONFIRMED_HYPOTHERMIA(10,0f);
    public final int wireId;
    public final float recoveryFactor;
    ThermalCondition(int wireId,float recoveryFactor){this.wireId=wireId;this.recoveryFactor=recoveryFactor;}
    public boolean cold(){return this==COLD||this==VERY_COLD||this==HYPOTHERMIA||this==CONFIRMED_HYPOTHERMIA;}
    public boolean hot(){return this==WARM||this==VERY_HOT||this==HYPERTHERMIA||this==HEATSTROKE||this==SUNSTROKE;}
    public boolean stressed(){return cold()||hot();}
    public static ThermalCondition fromWire(int id){
        for(var s:values())if(s.wireId==id)return s;
        return NONE;
    }
    public static ThermalCondition read(boolean installed,boolean active,String stage,double body){
        if(!installed||!active||!Double.isFinite(body)||body<30||body>43||stage==null)return NONE;
        try{return valueOf(stage);}catch(IllegalArgumentException ex){return NONE;}
    }
    /** Display severity, not a second temperature simulation. */
    public int severity(){return switch(this){
        case COLD,WARM -> 1;
        case VERY_COLD,VERY_HOT -> 2;
        case HYPOTHERMIA,HYPERTHERMIA,HEATSTROKE,SUNSTROKE,CONFIRMED_HYPOTHERMIA -> 3;
        default -> 0;
    };}
    public static ThermalCondition actual(String stage,String clinical,double body){
        var base=read(true,true,stage,body);
        if(base==HYPOTHERMIA&&"HYPOTHERMIA".equals(clinical))return CONFIRMED_HYPOTHERMIA;
        if(base==HYPERTHERMIA){
            if("HEATSTROKE".equals(clinical))return HEATSTROKE;
            if("SUNSTROKE".equals(clinical))return SUNSTROKE;
        }
        return base;
    }
    public int recoveryTicks(int ticks){return Math.max(0,Math.round(ticks*recoveryFactor));}
}
