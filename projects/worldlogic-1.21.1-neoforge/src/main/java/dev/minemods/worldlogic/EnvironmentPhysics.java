package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
public final class EnvironmentPhysics {
 private static final Map<Player,Long> STAMPEDE=new WeakHashMap<>(),ICE_NOTICE=new WeakHashMap<>(),COMBAT_NOTICE=new WeakHashMap<>();
 static double currentStrength(double mass,double strength){return Math.min(.045,.028*strength/Math.sqrt(Math.max(.5,mass)));}
 static double crampedFactor(int walls,boolean ceiling){return ceiling&&walls>=2?.80:1;}
 static double swingFactor(int walls){return walls>=2?.72:walls==1?.90:1;}
 private static boolean solid(Level l,Player p,AABB b){return l.getBlockCollisions(p,b).iterator().hasNext();}
 private static boolean loaded(Level l,BlockPos p){return l.hasChunkAt(p)&&l.isInWorldBounds(p);}
 static int sideWalls(Player p){
  Level l=p.level();BlockPos at=p.blockPosition();int walls=0;double y=p.getY()+.85;
  Vec3 look=p.getLookAngle().multiply(1,0,1).normalize();
  if(look.lengthSqr()<.01)look=new Vec3(0,0,1);
  Vec3 side=new Vec3(-look.z,0,look.x).scale(.67);
  for(int sign:new int[]{-1,1}){double x=p.getX()+side.x*sign,z=p.getZ()+side.z*sign;
   if(loaded(l,BlockPos.containing(x,y,z))&&solid(l,p,new AABB(x-.14,y,z-.14,x+.14,y+.55,z+.14)))walls++;
  }
  return walls;
 }
 public static float miningFactor(Player p){
  if(p.isCreative()||p.isSpectator()||p.isInWater()||p.getMainHandItem().isEmpty())return 1;
  Level l=p.level();AABB b=p.getBoundingBox();double top=p.getY()+1.85;
  boolean ceiling=loaded(l,BlockPos.containing(p.getX(),top+.35,p.getZ()))&&solid(l,p,new AABB(b.minX+.03,top,b.minZ+.03,b.maxX-.03,top+.50,b.maxZ-.03));
  return (float)crampedFactor(sideWalls(p),ceiling);
 }
 public static void melee(LivingIncomingDamageEvent e){
  if(e.isCanceled()||!Rules.on("weapon_obstruction")||!(e.getSource().getDirectEntity() instanceof Player p)||!Survival.active(p)||!Material.item(p.getMainHandItem(),"heavy_weapons"))return;
  double factor=swingFactor(sideWalls(p));if(factor>=1)return;e.setAmount(e.getAmount()*(float)factor);Survival.work(p,1.5);
  long now=p.level().getGameTime();if(COMBAT_NOTICE.getOrDefault(p,0L)<=now){COMBAT_NOTICE.put(p,now+60);p.displayClientMessage(Component.translatable("message.worldlogic.cramped_swing"),true);}
 }
 private static Vec3 flow(ServerLevel l,Player p){
  BlockPos at=p.blockPosition();if(!loaded(l,at))return Vec3.ZERO;
  var f=l.getFluidState(at);if(!f.is(FluidTags.WATER)){at=at.above();if(!loaded(l,at))return Vec3.ZERO;f=l.getFluidState(at);}
  if(!f.is(FluidTags.WATER))return Vec3.ZERO;
  for(Direction d:Direction.Plane.HORIZONTAL)if(!loaded(l,at.relative(d))||!loaded(l,at.relative(d).below()))return Vec3.ZERO;
  return f.getFlow(l,at);
 }
 private static void water(ServerLevel l,Player p,long now){
  if(!p.isInWater()||p.isPassenger()||p.getAbilities().flying)return;
  if(Rules.on("water_pressure")&&now%2==0){
   Vec3 flow=flow(l,p);if(flow.lengthSqr()>.04){
    Vec3 v=p.getDeltaMovement(),dir=flow.normalize();
    double opposing=Math.max(0,-v.dot(dir));double force=currentStrength(Material.mass(p),Rules.strength("movement"));
    if(v.dot(dir)<.26){p.setDeltaMovement(v.add(dir.scale(force*(1+Math.min(.5,opposing*2)))));p.hurtMarked=true;}
    if(opposing>.045&&now%20==0)Survival.work(p,1.2);
   }
  }
  if(!Rules.on("ice_entrapment")||!p.isEyeInFluid(FluidTags.WATER)||p.getDeltaMovement().y<.025)return;
  BlockPos above=BlockPos.containing(p.getX(),p.getEyeY()+.25,p.getZ());
  if(!loaded(l,above)||!Material.block(l.getBlockState(above),"ice_ceiling"))return;
  if(now%20==0){Survival.work(p,1.1);if(ICE_NOTICE.getOrDefault(p,0L)<=now){ICE_NOTICE.put(p,now+100);p.displayClientMessage(Component.translatable("message.worldlogic.ice_ceiling"),true);}}
 }
 static void stampede(ServerLevel l,Player p,long now){
  if(!Rules.on("animal_stampede")||!Balance.eligible(p)||!p.onGround()||Balance.down(p)||STAMPEDE.getOrDefault(p,0L)>now||now%4!=0)return;
  double total=0;Vec3 impulse=Vec3.ZERO;int n=0;
  for(Animal a:l.getEntitiesOfClass(Animal.class,p.getBoundingBox().inflate(.35),a->a.isAlive()&&a.onGround()&&Ecology.panicked(a,now))){
   if(n++>=8)break;Vec3 away=p.position().subtract(a.position()).multiply(1,0,1);if(away.lengthSqr()<.01)continue;away=away.normalize();
   double force=ImpactDynamics.collisionRisk(a.getDeltaMovement().subtract(Balance.recentMotion(p)).dot(away),Material.mass(a));
   if(force<.10)continue;total+=force;impulse=impulse.add(away.scale(force));
  }
  if(total>=.48&&impulse.lengthSqr()>.01&&Balance.force(p,impulse,"stampede",Math.min(1.35,total))){STAMPEDE.put(p,now+60);Survival.work(p,3);}
 }
 public static void tick(PlayerTickEvent.Post e){Player p=e.getEntity();if(!Survival.active(p)||!p.isAlive()||!(p.level() instanceof ServerLevel l))return;long now=l.getGameTime();water(l,p,now);stampede(l,p,now);}
 public static void logout(PlayerEvent.PlayerLoggedOutEvent e){Player p=e.getEntity();STAMPEDE.remove(p);ICE_NOTICE.remove(p);COMBAT_NOTICE.remove(p);}
}
