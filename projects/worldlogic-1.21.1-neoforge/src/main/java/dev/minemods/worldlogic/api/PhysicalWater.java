package dev.minemods.worldlogic.api;

import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Near-atmospheric seawater-like solution model. No chemistry, phase conversion, or water inventory mutation.
 * Correlations: Sharqawy, Lienhard & Zubair (2010), eqs 8, 9, 22-23, 36;
 * https://web.mit.edu/seawater/ . Freezing: UNESCO Technical Papers 44 (1983), p=0.
 * Salinity is g dissolved marine-type salts per kg solution; practical salinity is approximated by g/kg.
 * Mixed arbitrary solutes cannot be modeled by salinity alone. */
public final class PhysicalWater {
 private PhysicalWater(){}
 public static final double MAX_SALINITY_GKG=120, MAX_TEMPERATURE_C=100;
 public record Solution(double salinityGkg,double temperatureC,double densityKgM3,double specificHeatJkgK,
  double viscosityPaS,double freezingC,double boilingCAt1Atm,PhysicalMaterials.Evidence evidence){}
 public static OptionalDouble freezingPointC(double salinityGkg){
  if(!Double.isFinite(salinityGkg)||salinityGkg<0||salinityGkg>40)return OptionalDouble.empty();
  double s=salinityGkg;
  return OptionalDouble.of(s*(-.0575+.001710523*Math.sqrt(s)-.0002154996*s));
 }
 /** Equilibrium boiling estimate at one atmosphere, not a local pressure model. */
 public static OptionalDouble boilingPointC(double salinityGkg){
  if(!Double.isFinite(salinityGkg)||salinityGkg<0||salinityGkg>MAX_SALINITY_GKG)return OptionalDouble.empty();
  double s=salinityGkg/1000;
  double t=100;
  return OptionalDouble.of(t+(17.95+.2823*t-.0004584*t*t)*s*s+(6.56+.05267*t+.0001536*t*t)*s);
 }
 /** Valid liquid temperature range 0..100 C; negative temperatures and unsupported concentrations are unknown.
  * Freezing threshold is separately available for 0..40 g/kg, even when temperature is below zero. */
 public static Optional<Solution> solution(double salinityGkg,double temperatureC){
  if(!Double.isFinite(salinityGkg)||salinityGkg<0||salinityGkg>MAX_SALINITY_GKG||!Double.isFinite(temperatureC)||temperatureC<0||temperatureC>MAX_TEMPERATURE_C)return Optional.empty();
  double t=temperatureC,s=salinityGkg/1000,t2=t*t,t3=t2*t;
  double density=999.9+.02034*t-.006162*t2+.00002261*t3-.00000004657*t2*t2
      +s*(802-2.001*t+.01677*t2-.00003060*t3)-.00001613*s*s*t2;
  double sp=salinityGkg/1.00472, k=273.15+t, k2=k*k;
  double a=5.328-.0976*sp+.000404*sp*sp;
  double b=-.006913+.0007351*sp-.00000315*sp*sp;
  double c=.0000096-.000001927*sp+.00000000823*sp*sp;
  double d=.0000000025+.000000001666*sp-.000000000007125*sp*sp;
  double cp=1000*(a+b*k+c*k2+d*k2*k);
  double pure=.000042844+1/(.157*Math.pow(t+64.993,2)-91.296);
  double visc=pure*(1+(1.541+.01998*t-.0000952*t2)*s+(7.974-.07561*t+.0004724*t2)*s*s);
  if(density<=0||cp<=0||visc<=0||!Double.isFinite(density+cp+visc))return Optional.empty();
  return Optional.of(new Solution(salinityGkg,t,density,cp,visc,freezingPointC(salinityGkg).orElse(Double.NaN),boilingPointC(salinityGkg).orElse(Double.NaN),PhysicalMaterials.Evidence.ESTIMATED));
 }
 public static OptionalDouble nominalSalinityGkg(Fluid fluid){
  if(fluid==Fluids.WATER||fluid==Fluids.FLOWING_WATER)return OptionalDouble.of(0);
  var p=WorldLogicApi.waterProfile(fluid);
  return p.salinityGkg()>=0?OptionalDouble.of(p.salinityGkg()):OptionalDouble.empty();
 }
 public static OptionalDouble freezingPointC(Fluid fluid){
  var s=nominalSalinityGkg(fluid);return s.isEmpty()?OptionalDouble.empty():freezingPointC(s.getAsDouble());
 }
 public static Optional<Solution> solution(Fluid fluid,double temperatureC){
  var s=nominalSalinityGkg(fluid);return s.isEmpty()?Optional.empty():solution(s.getAsDouble(),temperatureC);
 }
 /** Conservative salt mass balance; masses are kg of total solution, not volume. */
 public static OptionalDouble mixedSalinityGkg(double massA,double salinityA,double massB,double salinityB){
  if(!Double.isFinite(massA)||!Double.isFinite(massB)||massA<0||massB<0||massA+massB<=0||!Double.isFinite(massA+massB)||!Double.isFinite(salinityA)||!Double.isFinite(salinityB)||salinityA<0||salinityB<0||salinityA>MAX_SALINITY_GKG||salinityB>MAX_SALINITY_GKG)return OptionalDouble.empty();
  return OptionalDouble.of(massA/(massA+massB)*salinityA+massB/(massA+massB)*salinityB);
 }
}
