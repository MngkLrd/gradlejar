package dev.minemods.worldlogic;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
public record Impulse(Vec3 velocity,double mass) {
 public double energy(){return .5*mass*velocity.lengthSqr();}
 public Vec3 reflect(Vec3 normal,double retained){return velocity.subtract(normal.scale(2*velocity.dot(normal))).scale(retained);}
 public double transfer(Entity target){return Math.min(.45,velocity.length()*mass/Math.max(.25,Material.mass(target)));}
}
