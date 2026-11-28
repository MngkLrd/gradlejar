package dev.minemods.worldlogicterrain.growth;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/** Species-specific ecological preferences. Rarity is a per-eligible-chunk divisor, not a global spawn percentage. */
public final class PlantSpecies {
    public record Profile(String id,String habitat,int rarity,int cap,double minTemp,double optimalTemp,double maxTemp,
        double minMoisture,double optimalMoisture,double maxMoisture,String seasons,int tickDivisor){
        public boolean seasonAllowed(String season){return season.equals("none")||Arrays.asList(seasons.split(" ")).contains(season);}
        public boolean sand(){return habitat.equals("desert")||habitat.equals("dry");}
        public double thermal(double t){return fit(t,minTemp,optimalTemp,maxTemp);}
        public double moisture(double m){return fit(m,minMoisture,optimalMoisture,maxMoisture);}
        public double habitatFitness(double t,double m,double fertility){
            if(t<minTemp||t>maxTemp||m<minMoisture||m>maxMoisture)return 0;
            return Math.max(.05,thermal(t)*moisture(m)*(.5+.5*clamp(fertility)));
        }
    }
    private static final Map<String,Profile> ALL=new LinkedHashMap<>();
    private static final List<Profile> NEW=new ArrayList<>();
    private static void add(String id,String habitat,int rarity,int cap,double min,double opt,double max,
                            double lo,double mid,double hi,String seasons,int ticks){
        ALL.put(id,new Profile(id,habitat,rarity,cap,min,opt,max,lo,mid,hi,seasons,ticks));
    }
    static {
        add("plantain","temperate_meadow",5,5,4,17,32,0.18,0.55,0.88,"SPRING SUMMER AUTUMN",5);NEW.add(ALL.get("plantain"));
        add("nettle","temperate_woodland",6,5,5,18,31,0.27,0.68,0.94,"SPRING SUMMER AUTUMN",5);NEW.add(ALL.get("nettle"));
        add("yarrow","temperate_meadow",7,4,3,18,32,0.14,0.42,0.8,"SPRING SUMMER AUTUMN",7);NEW.add(ALL.get("yarrow"));
        add("sage","warm_field",10,3,8,24,38,0.1,0.35,0.7,"SPRING SUMMER AUTUMN",9);NEW.add(ALL.get("sage"));
        add("thyme","dry",8,4,5,22,37,0.08,0.28,0.65,"SPRING SUMMER AUTUMN",8);NEW.add(ALL.get("thyme"));
        add("lavender","dry",13,3,8,24,37,0.1,0.3,0.65,"SPRING SUMMER AUTUMN",10);NEW.add(ALL.get("lavender"));
        add("wormwood","dry",6,5,2,20,36,0.05,0.27,0.7,"SPRING SUMMER AUTUMN",6);NEW.add(ALL.get("wormwood"));
        add("wild_mint","fresh_marsh",8,4,6,19,32,0.42,0.75,1,"SPRING SUMMER AUTUMN",6);NEW.add(ALL.get("wild_mint"));
        add("calendula","warm_field",10,4,8,22,34,.18,.48,.86,"SPRING SUMMER AUTUMN",7);NEW.add(ALL.get("calendula"));
        add("marshmallow","fresh_marsh",12,4,5,20,32,.42,.76,1,"SPRING SUMMER AUTUMN",8);NEW.add(ALL.get("marshmallow"));
        add("flax","temperate_meadow",9,4,4,19,32,.20,.53,.88,"SPRING SUMMER AUTUMN",7);NEW.add(ALL.get("flax"));
        add("horsetail","fresh_marsh",6,5,1,15,28,0.48,0.8,1,"SPRING SUMMER AUTUMN",5);NEW.add(ALL.get("horsetail"));
        add("marsh_cinquefoil","fresh_marsh",13,3,0,14,26,0.55,0.82,1,"SPRING SUMMER",9);NEW.add(ALL.get("marsh_cinquefoil"));
        add("bog_rosemary","season_bog",15,3,-2,12,24,0.55,0.83,1,"SPRING SUMMER",10);NEW.add(ALL.get("bog_rosemary"));
        add("cloudberry","season_bog",12,4,-3,12,23,0.5,0.75,1,"SPRING SUMMER",9);NEW.add(ALL.get("cloudberry"));
        add("lingonberry","cool_woodland",7,5,-4,12,25,0.22,0.52,0.89,"SPRING SUMMER AUTUMN",7);NEW.add(ALL.get("lingonberry"));
        add("arctic_poppy","season_arctic",14,3,-5,8,19,0.12,0.43,0.85,"SPRING SUMMER",9);NEW.add(ALL.get("arctic_poppy"));
        add("edelweiss","season_alpine",18,3,-2,10,23,0.1,0.35,0.72,"SPRING SUMMER",12);NEW.add(ALL.get("edelweiss"));
        add("fireweed","cool_woodland",6,5,1,17,30,0.2,0.58,0.9,"SPRING SUMMER AUTUMN",6);NEW.add(ALL.get("fireweed"));
        add("aloe","desert",12,3,12,29,44,0.03,0.18,0.55,"SPRING SUMMER AUTUMN WINTER",10);NEW.add(ALL.get("aloe"));
        add("agave","desert",17,3,10,30,46,0.02,0.15,0.5,"SPRING SUMMER AUTUMN WINTER",13);NEW.add(ALL.get("agave"));
        add("ginger","tropical",10,4,15,28,39,0.35,0.7,0.95,"SPRING SUMMER AUTUMN WINTER",9);NEW.add(ALL.get("ginger"));
        add("lemongrass","tropical",7,5,14,27,42,0.22,0.6,0.9,"SPRING SUMMER AUTUMN WINTER",6);NEW.add(ALL.get("lemongrass"));
        add("pineapple","tropical",4,5,15,27,38,0.35,0.73,0.95,"SPRING SUMMER AUTUMN WINTER",9);
        add("tomato","warm_field",6,5,9,24,36,0.22,0.55,0.9,"SPRING SUMMER AUTUMN",7);
        add("cucumber","warm_field",6,5,10,23,35,0.3,0.65,0.95,"SPRING SUMMER AUTUMN",7);
        add("radish","temperate_meadow",5,6,2,14,28,0.18,0.48,0.85,"SPRING SUMMER AUTUMN",5);
        add("onion","open_field",5,6,3,17,32,0.15,0.42,0.82,"SPRING SUMMER AUTUMN",7);
        add("cabbage","cool_field",6,5,1,12,27,0.28,0.6,0.91,"SPRING SUMMER AUTUMN",7);
        add("bell_pepper","warm_field",6,5,12,24,37,0.23,0.57,0.9,"SPRING SUMMER AUTUMN",8);
        add("strawberry","temperate_meadow",4,6,3,16,30,0.25,0.55,0.9,"SPRING SUMMER AUTUMN",6);
        add("blueberry","cool_woodland",4,7,-2,12,26,0.3,0.62,0.94,"SPRING SUMMER AUTUMN",6);
        add("raspberry","temperate_woodland",5,6,2,16,31,0.28,0.6,0.94,"SPRING SUMMER AUTUMN",6);
        add("blackberry","temperate_woodland",5,6,4,18,33,0.2,0.55,0.9,"SPRING SUMMER AUTUMN",6);
        add("grape","warm_woodland",5,6,7,23,37,0.16,0.57,0.88,"SPRING SUMMER AUTUMN",7);
        add("wheat","",1,3,2,18,34,0.13,0.5,0.9,"SPRING SUMMER AUTUMN",5);
        add("carrots","",1,3,2,15,29,0.2,0.58,0.93,"SPRING SUMMER AUTUMN",5);
        add("potatoes","",1,3,2,16,30,0.22,0.6,0.93,"SPRING SUMMER AUTUMN",5);
        add("beetroots","",1,3,2,15,29,0.2,0.55,0.92,"SPRING SUMMER AUTUMN",5);
        add("pumpkin_stem","",1,3,9,23,36,0.2,0.58,0.92,"SPRING SUMMER AUTUMN",5);
        add("melon_stem","",1,3,11,26,39,0.2,0.6,0.93,"SPRING SUMMER AUTUMN",5);
        add("sweet_berry_bush","",1,3,-2,13,28,0.25,0.6,0.95,"SPRING SUMMER AUTUMN",5);
    }
    private PlantSpecies(){}
    public static List<Profile> newSpecies(){return List.copyOf(NEW);}
    public static boolean isNew(String id){return NEW.stream().anyMatch(p->p.id().equals(id));}
    public static Profile byId(String id){return ALL.get(id);}
    public static Profile profile(BlockState s){
        var id=BuiltInRegistries.BLOCK.getKey(s.getBlock());
        String name=id.getPath();
        if(id.getNamespace().equals("worldlogicterrain")&&name.startsWith("wild_"))return ALL.get(name.substring(5));
        if(id.getNamespace().equals("minecraft"))return ALL.get(name);
        return null;
    }
    static double clamp(double n){return Math.max(0,Math.min(1,n));}
    static double fit(double n,double lo,double optimum,double hi){
        if(!Double.isFinite(n)||n<lo||n>hi)return 0;
        return clamp(n<optimum?(n-lo)/Math.max(.0001,optimum-lo):(hi-n)/Math.max(.0001,hi-optimum));
    }
}
