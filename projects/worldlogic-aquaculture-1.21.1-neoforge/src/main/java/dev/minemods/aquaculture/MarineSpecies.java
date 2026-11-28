package dev.minemods.aquaculture;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import java.util.*;

public enum MarineSpecies {
 GREAT_WHITE_SHARK("great_white_shark",5.2f,1.3f,1.25f,65,8,7,5,26,6, false, "shark", "hunter"),
 TIGER_SHARK("tiger_shark",4.2f,1.15f,1.05f,55,7,5,19,34,8,false,"shark","hunter"),
 NARWHAL("narwhal",4.8f,1.35f,1.4f,75,5,5,-2,12,5,true,"whale","hunter"),
 HUMPBACK_WHALE("humpback_whale",12f,3.3f,2.7f,150,0,9,0,31,3,true,"whale","filter"),
 BLUE_WHALE("blue_whale",20f,4.6f,3.7f,240,0,12,0,29,1,true,"whale","filter"),
 SEA_SNAKE("sea_snake",1.5f,.34f,.23f,12,2,2,22,36,13,true,"snake","hunter"),
 ELECTRIC_RAY("electric_ray",1.1f,1.05f,.28f,22,0,3,7,32,12,false,"ray","electric"),
 MANTA_RAY("manta_ray",3.9f,3.3f,.6f,50,0,5,17,36,7,false,"ray","filter");
 public final String id,shape,diet;
 public final float length,width,height,hp,damage;
 public final int depth,minTemp,maxTemp,weight;
 public final boolean airBreather;
 MarineSpecies(String id,float length,float width,float height,float hp,float damage,int depth,int minTemp,int maxTemp,int weight,boolean airBreather,String shape,String diet){
  this.id=id;this.length=length;this.width=width;this.height=height;this.hp=hp;this.damage=damage;this.depth=depth;this.minTemp=minTemp;this.maxTemp=maxTemp;this.weight=weight;this.airBreather=airBreather;this.shape=shape;this.diet=diet;
 }
 public boolean suitable(Aquaculture.Habitat h){
  return h!=null&&h.kind()==WaterKind.SEA&&h.salt()>=22&&h.risk()<=.75&&h.depth()>=depth&&h.temp()>=minTemp&&h.temp()<=maxTemp;
 }
 public boolean whale(){return shape.equals("whale");}
 public boolean shark(){return shape.equals("shark");}
 public boolean ray(){return shape.equals("ray");}
 public static MarineSpecies byId(String id){for(var s:values())if(s.id.equals(id))return s;return null;}
}
