package dev.minemods.thermal;
/** Loader-independent helper functions. Ambient temperature now comes from the core API. */
public final class ClimateModel {
    private ClimateModel() {}
    public static double geographicAir(double climateIndex, double aboveSurface) {
        return 15 + 29 * clamp(climateIndex, -1, 1) - Math.max(0, aboveSurface) * .065;
    }
    public static double biomeAir(double temperature, int y, int seaLevel) {
        return 2 + 23 * temperature - Math.max(0, y-seaLevel) * .065;
    }
    public static double dayOffset(long time, boolean covered, boolean storm) {
        double cycle = Math.cos((Math.floorMod(time, 24000L)-8000)*Math.PI/12000.0);
        return cycle*(covered ? 2 : 5) - (storm && !covered ? 3 : 0);
    }
    public static double water(double air, double base) { return clamp(base*.65+air*.35, 1, 32); }
    /** Natural-water thermal inertia: seasonal regional reference, actual surface air and depth. */
    public static double naturalWater(double air,double base,String kind,int depth) {
        return naturalWater(air,base,kind,depth,1);
    }
    public static double naturalWater(double air,double base,String kind,int depth,double thermalInertia){
        return naturalWater(air,base,kind,depth,thermalInertia,kind.equals("SEA")?-1.8:kind.equals("BRACKISH")?-.6:.1);
    }
    public static double naturalWater(double air,double base,String kind,int depth,double thermalInertia,double freezingC){
        double d=clamp(depth,0,48);
        double inertia=Double.isFinite(thermalInertia)?clamp(thermalInertia,.5,2):1;
        double influence=switch(kind) {
            case "SEA" -> .18; case "LAKE" -> .30; case "RIVER" -> .52;
            case "SPRING","GROUNDWATER" -> .08; case "WETLAND","POND" -> .70;
            case "BRACKISH" -> .36; default -> .48;
        };
        double anchor=base;
        if(kind.equals("GROUNDWATER")||kind.equals("SPRING"))anchor=clamp(8+base*.32,3,22);
        double surface=anchor+(air-anchor)*clamp(influence/inertia,0,1);
        double deep=kind.equals("SEA")?clamp(3+base*.24,1,13):kind.equals("SPRING")||kind.equals("GROUNDWATER")?anchor:clamp(4+base*.22,3,17);
        double mix=1-Math.exp(-d/(kind.equals("SEA")?16:kind.equals("RIVER")?24:9));
        return clamp(surface+(deep-surface)*mix,Double.isFinite(freezingC)?freezingC:0,38);
    }
    public static double skinWetness(double previous, boolean water, boolean rain, double radiant) {
        return clamp(water ? 1 : previous + (rain ? .08 : -.015 - Math.max(0,radiant)*.003),0,1);
    }
    public static double heatContribution(double output, double distance) {
        return output / (1 + distance*distance*.35);
    }
    public static double clamp(double v,double min,double max) { return Math.max(min,Math.min(max,v)); }
}
