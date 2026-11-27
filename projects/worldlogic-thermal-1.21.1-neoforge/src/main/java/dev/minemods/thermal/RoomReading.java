package dev.minemods.thermal;
/** A modeled local room, not an authoritative outdoor Core weather measurement. */
public record RoomReading(boolean indoor,double airC,double humidity,double ventilation,double massC,int volume) {
    public static RoomReading outside(double air,double humidity){return new RoomReading(false,air,humidity,1,air,0);}
    public static RoomReading unavailable(){return outside(0,0);}
}
