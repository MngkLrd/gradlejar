package dev.minemods.thermal;
/** Bounded gameplay approximation: air and thermal mass in degrees C, vapor in g/m3.
 * No global weather mutation and no player-specific heat in the room state. */
public final class RoomPhysics {
    private RoomPhysics(){}
    public record State(double air,double mass,double vapor){}
    public record Inputs(double outdoor,double humidity,double ventilation,double conductance,double massFactor,int volume,double heat,double cooling,double moisture){}
    private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
    public static double saturation(double temperature){
        double t=clamp(temperature,-60,70);
        return 216.7*(6.112*Math.exp(17.62*t/(243.12+t)))/(273.15+t);
    }
    public static double humidity(State s){return clamp(100*s.vapor()/saturation(s.air()),0,100);}
    public static State initial(double air,double humidity){return new State(air,air,saturation(air)*clamp(humidity,0,100)/100);}
    private static double approach(double a,double b,double dt,double tau){return a+(b-a)*-Math.expm1(-dt/Math.max(.5,tau));}
    public static State step(State s,Inputs in,double seconds){
        double remaining=clamp(seconds,0,3600);
        double outdoor=in.outdoor(),vent=clamp(in.ventilation(),.02,1),conduct=clamp(in.conductance(),.2,3),mass=clamp(in.massFactor(),.3,4);
        double size=Math.max(.65,Math.sqrt(Math.max(4,in.volume())/27.0));
        double retention=clamp(.65/(1+vent*3+conduct*.30),.08,.65);
        double source=(Math.max(0,in.heat())-Math.max(0,in.cooling()))*.35/(size*(.7+conduct*.2+vent*3));
        double exteriorVapor=saturation(outdoor)*clamp(in.humidity(),0,100)/100;
        while(remaining>0){
            double dt=Math.min(10,remaining);remaining-=dt;
            double target=outdoor+clamp(source+retention*(s.mass()-outdoor),-8,14);
            double airTau=(30+40*mass)/(1+vent*4+conduct*.25);
            if(source>0&&target>s.air())airTau/=3;
            double air=approach(s.air(),target,dt,airTau);
            double wall=approach(s.mass(),air,dt,120*mass/(1+conduct*.35));
            // Mixing uses absolute vapor, so heating alone lowers RH without magically removing water.
            double vaporTarget=exteriorVapor+Math.max(0,in.moisture())/(Math.max(4,in.volume())*(.06+vent*.8));
            double vapor=approach(s.vapor(),vaporTarget,dt,65/(.25+vent*3));
            s=new State(clamp(air,outdoor-8,outdoor+14),clamp(wall,-100,100),clamp(vapor,0,saturation(air)));
        }
        return s;
    }
}
