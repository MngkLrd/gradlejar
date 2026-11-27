package dev.minemods.thermal;
/** Live authoritative core values with clearly distinguished local model estimate. */
public record WeatherReading(String biome,String dimension,int elevation,boolean atmosphere,
 double humidity,double windSpeed,double windX,double windZ,int precipitation,double intensity,
 boolean thunder,double solar,double reference,double latitude,double moisture) {
    public static WeatherReading unavailable(){return new WeatherReading("","",0,false,0,0,0,0,0,0,false,0,Double.NaN,Double.NaN,Double.NaN);}
    public static double normalizedWind(double mps,boolean covered){return covered?.06:Math.max(0,Math.min(1,mps/12.0));}
    public static double dryingFactor(double humidity){return Math.max(.15,Math.min(1,1-humidity/100.0));}
}
