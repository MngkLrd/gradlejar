package dev.minemods.metabolism;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
/** Approximate gameplay composition per 100 g raw edible muscle, not whole catch. Rare species use related-fish priors. */
final class FishNutrition {
 private record Raw(double protein,double fat,double b12,double iron,double calcium,double magnesium){}
 private static final Map<String,Raw> RAW=Map.ofEntries(
  Map.entry("pike",new Raw(19.3,0.8,2.1,0.5,20,28)),
  Map.entry("perch",new Raw(19.4,1.3,1.9,0.9,28,30)),
  Map.entry("crucian_carp",new Raw(18,2.6,1.5,1.1,45,26)),
  Map.entry("carp",new Raw(17.8,5.6,1.5,1.2,41,29)),
  Map.entry("roach",new Raw(18.1,2.4,2,0.8,30,26)),
  Map.entry("bream",new Raw(18.3,3.8,2,1,32,28)),
  Map.entry("trout",new Raw(20.5,6.2,4.3,0.6,43,27)),
  Map.entry("grayling",new Raw(19.8,3.6,2.8,0.7,24,29)),
  Map.entry("char",new Raw(20,8.5,4,0.5,20,30)),
  Map.entry("taimen",new Raw(20,5.2,3,0.8,20,28)),
  Map.entry("whitefish",new Raw(19.1,5.9,3.2,0.6,25,30)),
  Map.entry("burbot",new Raw(18.5,0.9,1.6,0.6,20,28)),
  Map.entry("catfish",new Raw(17.7,5.5,2.3,0.7,13,23)),
  Map.entry("tench",new Raw(18.2,2.3,1.7,0.9,35,27)),
  Map.entry("loach",new Raw(17.5,3.7,2.1,1.4,40,28)),
  Map.entry("rotan",new Raw(18,2.7,1.8,0.9,32,26)),
  Map.entry("rudd",new Raw(18.2,2.8,1.9,0.9,30,27)),
  Map.entry("bowfin",new Raw(18.5,3.9,2.4,0.8,22,25)),
  Map.entry("piranha",new Raw(19.5,3.2,2.1,1.1,24,28)),
  Map.entry("arapaima",new Raw(19.8,2.1,2,0.8,18,26)),
  Map.entry("tilapia",new Raw(20.1,1.7,1.6,0.6,10,27)),
  Map.entry("pacu",new Raw(18.4,9.3,2.6,0.8,20,25)),
  Map.entry("snakehead",new Raw(20.4,2.5,2.2,0.9,18,27)),
  Map.entry("electric_eel",new Raw(18.2,10.5,3.1,0.9,21,24)),
  Map.entry("atlantic_cod",new Raw(17.8,0.7,0.9,0.4,16,32)),
  Map.entry("herring",new Raw(18,9,13.7,1.1,57,32)),
  Map.entry("mackerel",new Raw(18.6,13.9,8.7,1.6,12,76)),
  Map.entry("halibut",new Raw(19.4,2.3,1.2,0.3,7,23)),
  Map.entry("haddock",new Raw(18.9,0.6,1.8,0.5,11,21)),
  Map.entry("flounder",new Raw(16.5,1.7,1.1,0.4,21,18)),
  Map.entry("tuna",new Raw(24.4,0.5,2.1,0.8,4,35)),
  Map.entry("barracuda",new Raw(20.5,2.1,2.3,0.6,18,29)),
  Map.entry("grouper",new Raw(19.4,1,1.1,0.9,27,31)),
  Map.entry("snapper",new Raw(20.5,1.3,3,0.2,32,32)),
  Map.entry("mahi_mahi",new Raw(18.5,0.7,0.6,1.1,15,30)),
  Map.entry("parrotfish",new Raw(19.4,1.7,1.4,0.7,25,28))
 );
 static Set<String> species(){return RAW.keySet();}
 static Optional<FoodProfiles.Entry> portion(String id,int grams,boolean cooked){
  if(id==null||grams<1||grams>(cooked?80:100))return Optional.empty();
  Raw r=RAW.get(id);if(r==null)return Optional.empty();
  // Aqua cooks 100 g raw to 80 g cooked; concentration accounts for water loss, with retention losses.
  double scale=grams/100.0*(cooked?1.25:1);
  double p=r.protein*scale*(cooked?.98:1),fat=r.fat*scale*(cooked?.90:1);
  double b12=r.b12*scale*(cooked?.8:1),iron=r.iron*scale*(cooked?.95:1);
  double[] n={p,fat,0,0,0,0,b12,iron,r.calcium*scale*(cooked?.95:1),r.magnesium*scale*(cooked?.95:1)};
  // Gameplay raw food risk, dose-dependent. Cooking reduces ordinary contamination; no pathogen diagnosis here.
  double risk=(cooked?.002:.045)*grams/100.0;
  if(id.equals("barracuda")||id.equals("parrotfish")||id.equals("grouper"))risk+=.015*grams/100.0;
  return Optional.of(new FoodProfiles.Entry(new MetabolicModel.Meal(4*p+9*fat,n,risk,cooked?.97:.79),true));
 }
 private FishNutrition(){}
}
