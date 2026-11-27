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
    public static double skinWetness(double previous, boolean water, boolean rain, double radiant) {
        return clamp(water ? 1 : previous + (rain ? .08 : -.015 - Math.max(0,radiant)*.003),0,1);
    }
    public static double heatContribution(double output, double distance) {
        return output / (1 + distance*distance*.35);
    }
    public static double clamp(double v,double min,double max) { return Math.max(min,Math.min(max,v)); }
}
