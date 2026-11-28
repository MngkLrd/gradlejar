package dev.minemods.worldlogic;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import dev.minemods.worldlogic.api.WorldLogicApi;
/** Bounded local advection from existing persisted direction sectors. No worldgen reads or new blockstates. */
public final class RiverCurrent {
 private RiverCurrent(){}
 private static boolean loaded(Level l,BlockPos p){return !l.isOutsideBuildHeight(p)&&l.hasChunkAt(p);}
 private static boolean water(Level l,BlockPos p){return loaded(l,p)&&l.getFluidState(p).is(FluidTags.WATER);}
 public static Optional<WorldLogicApi.RiverCurrent> sample(Level l,BlockPos p){
  if(!loaded(l,p))return Optional.empty();
  Vec3 direction=DirectedRiverBlock.flow(l,p,Vec3.ZERO);
  if(direction.lengthSqr()<.01)return Optional.empty();
  int depth=1,neighbors=0;
  for(int n=1;n<=3;n++){if(!water(l,p.below(n)))break;depth++;}
  for(Direction side:Direction.Plane.HORIZONTAL)if(water(l,p.relative(side)))neighbors++;
  double speed=speed(depth,neighbors);
  return Optional.of(new WorldLogicApi.RiverCurrent(direction.normalize().scale(speed),speed,depth,neighbors));
 }
 static double speed(int depth,int neighbors){return Math.min(3.8,1.4+.42*(Math.max(1,Math.min(4,depth))-1)+.26*Math.max(0,Math.min(4,neighbors)));}
 /** Velocity is blocks/tick. Preserve active faster downstream swimming and vertical movement. */
 static Vec3 advect(Vec3 motion,WorldLogicApi.RiverCurrent c,double coupling,double strength){
  Vec3 dir=c.velocityMps().normalize();
  double deficit=c.speedMps()/20.0-motion.dot(dir);
  if(deficit<=0)return motion;
  return motion.add(dir.scale(Math.min(.065,deficit*Math.max(0,Math.min(.6,coupling*strength)))));
 }
 static Optional<WorldLogicApi.RiverCurrent> atEntity(Entity e){
  Level l=e.level();BlockPos p=e.blockPosition();
  return sample(l,p).or(()->sample(l,BlockPos.containing(e.getX(),e.getY()+e.getBbHeight()*.5,e.getZ())));
 }
 static boolean push(Player p,WorldLogicApi.RiverCurrent current){
  if(!p.isInWater()||p.isPassenger()||p.getAbilities().flying)return false;
  Vec3 before=p.getDeltaMovement(),after=advect(before,current,.29,Rules.strength("movement"));
  if(after.equals(before))return false;
  p.setDeltaMovement(after);p.hurtMarked=true;return true;
 }
 static boolean pushAquatic(WaterAnimal animal){
  if(!animal.isAlive()||animal.isPassenger()||!animal.isInWater()||!Rules.on("water_pressure"))return false;
  var current=atEntity(animal);if(current.isEmpty())return false;
  Vec3 v=animal.getDeltaMovement(),after=advect(v,current.get(),.35,Rules.strength("movement"));
  if(after.equals(v))return false;
  animal.setDeltaMovement(after);return true;
 }
 public static void aquatic(EntityTickEvent.Post e){
  if(e.getEntity() instanceof WaterAnimal animal&&animal.level() instanceof ServerLevel)pushAquatic(animal);
 }
 static void verify(){
  if(!(speed(4,4)>speed(1,0)&&speed(4,4)<=3.8))throw new IllegalStateException("river velocity range");
  var c=new WorldLogicApi.RiverCurrent(new Vec3(3,0,0),3,3,3);
  Vec3 still=advect(Vec3.ZERO,c,.35,1),against=advect(new Vec3(-.12,.02,0),c,.35,1);
  if(still.x<=.04||against.x<=-.12||against.y!=.02||!advect(new Vec3(.22,.03,0),c,.35,1).equals(new Vec3(.22,.03,0)))throw new IllegalStateException("river advection controls");
 }
}
