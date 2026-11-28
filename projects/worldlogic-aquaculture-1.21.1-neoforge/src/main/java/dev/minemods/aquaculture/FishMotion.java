package dev.minemods.aquaculture;
import java.util.Set;

/** Pure animation math; does not change navigation, movement or survival. */
public final class FishMotion {
 private FishMotion(){}
 public static final Set<String> MODES=Set.of("ambush","spiny","deep","school","salmonid","eel","fast","flat");
 public record Pose(float body,float head,float rear,float tail,float fins,float roll,float pitch,float lift){}
 public static Pose sample(String mode,float age,float stride,float activity,float vertical,boolean wet,boolean panic,float phaseOffset){
  float speed=Math.max(0,Math.min(1,activity));
  if(!wet){
   float p=age*.92f+phaseOffset;
   return new Pose(0,0,.18f*sin(p),.45f*sin(p-.8f),.2f*sin(p),1.38f+.14f*sin(p),0,-.8f*Math.abs(sin(p)));
  }
  float frequency,amplitude,body,head,rear,tail,fin;
  switch(mode){
   case "eel"->{frequency=.33f;amplitude=.47f;body=.23f;head=-.18f;rear=.66f;tail=1;fin=.25f;}
   case "flat"->{frequency=.28f;amplitude=.24f;body=.04f;head=.02f;rear=.35f;tail=.8f;fin=.85f;}
   case "fast"->{frequency=.48f;amplitude=.35f;body=.035f;head=-.025f;rear=.38f;tail=1;fin=.12f;}
   case "ambush"->{frequency=.23f;amplitude=.33f;body=.03f;head=.025f;rear=.38f;tail=1;fin=.25f;}
   case "deep"->{frequency=.27f;amplitude=.34f;body=.10f;head=-.06f;rear=.55f;tail=1;fin=.55f;}
   case "spiny"->{frequency=.34f;amplitude=.33f;body=.07f;head=-.03f;rear=.48f;tail=1;fin=.5f;}
   case "salmonid"->{frequency=.39f;amplitude=.38f;body=.09f;head=-.06f;rear=.55f;tail=1;fin=.32f;}
   default->{frequency=.38f;amplitude=.31f;body=.08f;head=-.05f;rear=.5f;tail=1;fin=.35f;}
  }
  float p=age*frequency+stride*.7f+phaseOffset;
  float a=amplitude*(.22f+.78f*speed)*(panic?1.5f:1f);
  return new Pose(body*a*sin(p+.7f),head*a*sin(p+1.1f),rear*a*sin(p-.7f),tail*a*sin(p-1.4f),
    fin*(.12f+.5f*speed)*sin(p+.5f),0,Math.max(-.25f,Math.min(.25f,-vertical*1.6f)),0);
 }
 private static float sin(float v){return (float)Math.sin(v);}
}
