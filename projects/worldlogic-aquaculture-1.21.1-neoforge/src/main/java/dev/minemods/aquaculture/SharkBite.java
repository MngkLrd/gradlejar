package dev.minemods.aquaculture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;

/** Rest-pose mouth offset uses the actual mesh hierarchy and renderer scale. Local -Z is forward. */
final class SharkBite {
 private SharkBite(){}
 private static final java.util.Map<MarineSpecies,Double> OFFSETS=new java.util.EnumMap<>(MarineSpecies.class);
 static double reach(MarineSpecies s){
  return OFFSETS.computeIfAbsent(s,key->{
   var mesh=MarineGeometry.ALL.get(key.id);var pivots=new HashMap<String,Double>();pivots.put("root",0.0);
   for(var part:mesh.parts()){
    double z=pivots.get(part.parent())+part.oz();pivots.put(part.name(),z);
    if(part.name().equals("snout"))return -(z+part.z())*key.length/mesh.pixels();
   }
   throw new IllegalStateException("missing shark snout "+key.id);
  });
 }
 static Vec3 forward(float yaw){double a=Math.toRadians(yaw);return new Vec3(-Math.sin(a),0,Math.cos(a));}
 static Vec3 mouth(MarineSpecies s,Vec3 center,float yaw){return center.add(forward(yaw).scale(reach(s)));}
 static Vec3 closest(AABB box,Vec3 point){return new Vec3(clamp(point.x,box.minX,box.maxX),clamp(point.y,box.minY,box.maxY),clamp(point.z,box.minZ,box.maxZ));}
 private static double clamp(double n,double a,double b){return Math.max(a,Math.min(b,n));}
 static boolean contact(MarineSpecies s,Vec3 center,float yaw,AABB target){
  if(!s.shark())return false;
  Vec3 delta=target.getCenter().subtract(center);
  if(delta.dot(forward(yaw))<=0)return false;
  Vec3 mouth=mouth(s,center,yaw);double tolerance=Math.max(.35,s.width*.42);
  return closest(target,mouth).distanceToSqr(mouth)<=tolerance*tolerance;
 }
}
