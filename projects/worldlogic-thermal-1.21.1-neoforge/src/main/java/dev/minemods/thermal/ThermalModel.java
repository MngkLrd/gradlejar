package dev.minemods.thermal;
/** Gameplay tuning, not medical predictions. */
public final class ThermalModel {
    private ThermalModel() {}
    public enum Stage { HYPOTHERMIA, VERY_COLD, COLD, COMFORTABLE, WARM, VERY_HOT, HYPERTHERMIA }
    public record Exposure(double airC, double radiantC, double wind, double wetness,
                           double insulation, double activity, boolean submerged, double waterC,
                           double mealWarmth, double coolingC, double ventilation, double coldAdaptation,double heatAdaptation,double nutrition,double hydration,double roomDelta) {
        public Exposure(double airC,double radiantC,double wind,double wetness,double insulation,double activity,boolean submerged,double waterC,double mealWarmth,double coolingC,double ventilation){this(airC,radiantC,wind,wetness,insulation,activity,submerged,waterC,mealWarmth,coolingC,ventilation,0,0,1,1,0);}
        public Exposure(double airC,double radiantC,double wind,double wetness,double insulation,double activity,boolean submerged,double waterC,double mealWarmth){this(airC,radiantC,wind,wetness,insulation,activity,submerged,waterC,mealWarmth,0,0);}
        public Exposure {
            if (!Double.isFinite(airC) || !Double.isFinite(radiantC) || !Double.isFinite(wind)
                || !Double.isFinite(wetness) || !Double.isFinite(insulation) || !Double.isFinite(activity)
                || !Double.isFinite(waterC) || !Double.isFinite(mealWarmth) || !Double.isFinite(coolingC) || !Double.isFinite(ventilation)||!Double.isFinite(coldAdaptation)||!Double.isFinite(heatAdaptation)||!Double.isFinite(nutrition)||!Double.isFinite(hydration)||!Double.isFinite(roomDelta)) throw new IllegalArgumentException("non-finite exposure");
        }
    }
    public record State(double bodyC, double feelsLikeC, double targetC, Stage stage, double trendCPerMinute) {}
    public static State initial() { return new State(37, 22, 37, Stage.COMFORTABLE, 0); }
    public static State tick(State previous, Exposure e, double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0 || !Double.isFinite(previous.bodyC())) throw new IllegalArgumentException("invalid time/state");
        double wet = clamp(e.wetness(),0,1), wind = clamp(e.wind(),0,1), insulation = clamp(e.insulation(),0,1);
        double activity = clamp(e.activity(),0,1), meal = clamp(e.mealWarmth(),0,1);
        double retained = insulation * (1 - 0.75 * wet);
        double felt = e.submerged() ? e.waterC() : e.airC() + clamp(e.roomDelta(),-8,14) + clamp(e.radiantC(),0,55)-clamp(e.coolingC(),0,18)
            - wind * (2 + 5 * wet) - 6 * wet;
        double cold = Math.max(0, 12 - felt - activity * 6 - meal * 2 - retained * 6 - clamp(e.coldAdaptation(),0,1)*3 + (1-clamp(e.nutrition(),0,1))*5) * (1 - retained * .90);
        double hot = Math.max(0, felt - 28 + activity * 7 + meal + retained * 5 - clamp(e.ventilation(),0,1)*7 - clamp(e.heatAdaptation(),0,1)*3 + (1-clamp(e.hydration(),0,1))*4);
        double target = clamp(37 - cold * .14 + hot * .18, 30, 43);
        double tau = e.submerged() ? 80 : (felt>=18&&felt<=28&&previous.bodyC()<36.6 ? 105 : 200);
        // Recover in a heated room faster, without accelerating cooling or dangerous overheating.
        if(!e.submerged()&&e.roomDelta()>0&&previous.bodyC()<37&&target>previous.bodyC()&&target<=37.5)tau/=3;
        double dt = Math.min(seconds, 5);
        double next = previous.bodyC() + (target - previous.bodyC()) * -Math.expm1(-dt / tau);
        next = clamp(next,30,43);
        double trend = (target - next) * 60 / tau;
        return new State(next, felt, target, stage(next), trend);
    }
    public static Stage stage(double bodyC) {
        if (bodyC < 35) return Stage.HYPOTHERMIA;
        if (bodyC < 36) return Stage.VERY_COLD;
        if (bodyC < 36.6) return Stage.COLD;
        if (bodyC <= 37.5) return Stage.COMFORTABLE;
        if (bodyC < 38.3) return Stage.WARM;
        if (bodyC < 39.5) return Stage.VERY_HOT;
        return Stage.HYPERTHERMIA;
    }
    private static double clamp(double value, double low, double high) { return Math.max(low, Math.min(high,value)); }
}
