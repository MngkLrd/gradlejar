package dev.minemods.worldlogic;

/** Diagnostic parcel model using physical SI quantities and a parameterized environmental profile.
 * Not a hydrostatic grid solver; column heights are atmospheric metres, not generated blocks.
 * No world state, rendering, or gameplay side effects. */
public final class AtmosphericColumn {
 private AtmosphericColumn(){}
 private static final double G=9.80665,RD=287.05,CP=1004.0,EPS=.622,LV=2.5e6;
 private static final int DZ=100,TOP=4000;
 public record Profile(double airC,double pressureHpa,double vaporMixingRatioGkg,double relativeHumidityPercent,
  double dewPointC,double wetBulbC,double environmentalLapseCPerKm,double condensationLevelM,
  double cloudBaseM,double cloudTopM,double condensedWaterGkg,double buoyancyEnergyJkg,
  double cloudFraction,double precipitationPotential,double fogPotential,boolean convective){}
 private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
 /** Liquid-water Magnus approximation; range deliberately limited for numerical stability. */
 public static double saturationHpa(double c){c=clamp(c,-80,60);return 6.112*Math.exp(17.67*c/(c+243.5));}
 public static double vaporMixingRatio(double vaporHpa,double pressureHpa){
  double e=clamp(vaporHpa,0,pressureHpa*.95);return EPS*e/(pressureHpa-e);
 }
 public static double dewPoint(double e){
  double v=Math.log(Math.max(saturationHpa(-80),e)/6.112);
  return clamp(243.5*v/(17.67-v),-80,60);
 }
 private static double wetBulb(double air,double vapor,double pressure){
  double lo=Math.min(air,dewPoint(vapor)),hi=air;
  for(int i=0;i<32;i++){
   double m=(lo+hi)*.5;
   double estimate=saturationHpa(m)-.00066*(1+.00115*m)*pressure*(air-m);
   if(estimate>vapor)hi=m;else lo=m;
  }
  return (lo+hi)*.5;
 }
 private static double moistLapse(double kelvin,double rs){
  return G*(1+LV*rs/(RD*kelvin))/(CP+LV*LV*rs*EPS/(RD*kelvin*kelvin));
 }
 /** Inputs: local temperature, station pressure, actual RH estimate, environmental lapse and forced ascent.
  * Condensate is cumulative vapor removed along a pseudo-adiabatic parcel path (g/kg dry air).
  * Cloud/precipitation/fog are bounded diagnostic indices, not calibrated rates or probabilities. */
 public static Profile diagnose(double airC,double pressureHpa,double rhPercent,double lapseCPerKm,double forcedLiftM,double surfaceCoolingC,double windMps){
  double air=clamp(airC,-60,55),pressure=clamp(pressureHpa,700,1100),rh=clamp(rhPercent,0,100);
  double lapse=clamp(lapseCPerKm,-2,11),lift=clamp(forcedLiftM,0,TOP);
  double vapor=saturationHpa(air)*rh/100.0;
  double initial=vaporMixingRatio(vapor,pressure),mix=initial;
  double dew=dewPoint(vapor),wb=wetBulb(air,vapor,pressure);
  double parcel=air,levelPressure=pressure,condensation=rh>=99.999?0:-1;
  double base=-1,top=-1,condensed=0,energy=0;
  boolean activeCloud=false;
  for(int h=DZ;h<=TOP;h+=DZ){
   double env=air-lapse*h/1000.0,prevEnv=air-lapse*(h-DZ)/1000.0;
   levelPressure*=Math.exp(-G*DZ/(RD*(273.15+(env+prevEnv)*.5)));
   double dryTrial=parcel-G/CP*DZ;
   double rsDry=vaporMixingRatio(saturationHpa(dryTrial),levelPressure);
   if(mix>rsDry){
    if(condensation<0)condensation=h;
    // Once saturation is reached, latent heating reduces parcel cooling.
    double rs=vaporMixingRatio(saturationHpa(parcel),levelPressure);
    parcel-=moistLapse(parcel+273.15,rs)*DZ;
    double next=Math.min(mix,vaporMixingRatio(saturationHpa(parcel),levelPressure));
    double lost=mix-next;mix=next;
    double buoyancy=G*(parcel-env)/(env+273.15);
    boolean reachable=h<=lift||(activeCloud&&buoyancy>0);
    if(reachable&&lost>1e-10){
     if(base<0)base=h-DZ;
     top=h;activeCloud=true;condensed+=lost;
     energy+=Math.max(0,buoyancy)*DZ;
    }else if(activeCloud&&buoyancy<=0){activeCloud=false;}
   }else{
    parcel=dryTrial;
    if(h>lift)activeCloud=false;
   }
  }
  double depth=base<0?0:top-base;
  double water=clamp(condensed,0,initial)*1000;
  double cloud=clamp((depth/1800.0)*(water/2.0),0,1);
  double precipitation=clamp((water-.25)/3.0,0,1)*clamp(depth/1000.0,0,1);
  // Radiation-fog proxy: surface cooling must reach saturation and excessive mixing suppresses it.
  double fog=clamp((surfaceCoolingC-(air-dew)+.25)/2.0,0,1)*clamp((7-windMps)/6.0,0,1);
  return new Profile(air,pressure,initial*1000,rh,dew,wb,lapse,condensation,base,top,water,energy,
   cloud,precipitation,fog,depth>=1000&&energy>=100&&water>.3);
 }
}
