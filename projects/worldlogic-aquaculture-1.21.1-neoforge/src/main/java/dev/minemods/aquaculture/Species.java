package dev.minemods.aquaculture;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;

public record Species(String id,String en,String ru,String region,double minTemp,double maxTemp,double minSalt,double maxSalt,
 int minDepth,int maxGroup,double minKg,double maxKg,double adultLength,double width,double length,double height,int color,int weight,String diet,String nursery,int maturityYears,int incubationDays,int hatchMin,int hatchMax,int reproductionCooldownDays) {
 public static final List<Species> ALL=load();
 public static final Map<String,Species> BY_ID=index();
 private static List<Species> load() {
  try(var stream=Species.class.getResourceAsStream("/assets/aquaculture/species.json")){
   if(stream==null) throw new IllegalStateException("Missing fish registry");
   try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
    return List.of(new Gson().fromJson(reader,Species[].class));
   }
  }catch(Exception e){throw new IllegalStateException("Invalid fish registry",e);}
 }
 private static Map<String,Species> index(){
  Map<String,Species> m=new LinkedHashMap<>();
  for(Species s:ALL){
   if(m.put(s.id,s)!=null||s.minTemp>s.maxTemp||s.minSalt>s.maxSalt||s.weight<=0||s.minKg<=0||s.maxKg<s.minKg||s.minDepth<1||s.adultLength<=0||s.adultLength>3.5||s.maturityYears<1||s.incubationDays<1||s.hatchMin<1||s.hatchMax<s.hatchMin||s.reproductionCooldownDays<1||!Set.of("predator","grazer","invertebrate","plankton").contains(s.diet)||!Set.of("gravel","vegetation","open_water","sheltered_bed").contains(s.nursery))throw new IllegalStateException("Invalid "+s.id);
  }
  return Collections.unmodifiableMap(m);
 }
 public boolean suitable(WaterKind kind,double temp,double salt,double risk,int depth){
  if(!Double.isFinite(temp)||!Double.isFinite(salt)||!Double.isFinite(risk)||salt<0||risk<0||risk>.85||depth<minDepth||temp<minTemp||temp>maxTemp||salt<minSalt||salt>maxSalt)return false;
  boolean marine=kind==WaterKind.SEA||kind==WaterKind.BRACKISH;
  if(region.startsWith("marine"))return marine;
  if(marine)return kind==WaterKind.BRACKISH&&maxSalt>=5;
  return switch(kind){
   case WETLAND,POND -> region.equals("wetland")||region.equals("temperate")||region.equals("tropical");
   case RIVER,LAKE,SPRING -> !region.equals("wetland")||kind==WaterKind.LAKE;
   default -> false;
  };
 }
 public int habitatWeight(WaterKind kind,double temp,double salt,double risk,int depth){
  if(!suitable(kind,temp,salt,risk,depth))return 0;
  double center=(minTemp+maxTemp)/2, half=Math.max(1,(maxTemp-minTemp)/2);
  return Math.max(1,(int)Math.round(weight*(1-.65*Math.abs(temp-center)/half)*(1-.65*risk)));
 }
}

