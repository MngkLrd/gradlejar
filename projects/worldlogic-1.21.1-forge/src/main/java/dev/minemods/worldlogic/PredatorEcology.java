package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import org.joml.Vector3f;
public final class PredatorEcology {
 private static final String WOUND="worldlogic_blood_until",FOOD="worldlogic_food_pos",FOOD_TIME="worldlogic_food_until";
 private record Trace(UUID owner,Vec3 pos,long until){}
 private static final Map<ServerLevel,ArrayDeque<Trace>> TRAILS=new WeakHashMap<>();
 private static final Map<Mob,Long> NEXT=new WeakHashMap<>();
 private static final Map<Player,Vec3> LAST=new WeakHashMap<>();
 static boolean predator(Mob m){return (m instanceof Wolf||m instanceof Fox)&&(!(m instanceof TamableAnimal t)||!t.isTame());}
 public static void wound(LivingDamageEvent e){
  if(!Rules.on("blood_trail")||e.isCanceled()||e.getAmount()<3||!(e.getEntity() instanceof Player p)||!Survival.active(p))return;
  if(p.getHealth()-e.getAmount()>p.getMaxHealth()*.65)return;
  long now=p.level().getGameTime();p.getPersistentData().putLong(WOUND,now+400);
 }
 public static void tick(TickEvent.PlayerTickEvent.Post e){
  Player p=e.player;if(!Rules.on("blood_trail")||!Survival.active(p)||!(p.level() instanceof ServerLevel l)||l.getGameTime()%20!=0)return;long now=l.getGameTime();
  if(p.isInWater()||l.isRainingAt(p.blockPosition())||p.getHealth()>p.getMaxHealth()*.8){p.getPersistentData().remove(WOUND);LAST.remove(p);return;}
  if(p.getPersistentData().getLong(WOUND)<=now||!p.onGround())return;
  Vec3 point=p.position();Vec3 prev=LAST.get(p);if(prev!=null&&prev.distanceToSqr(point)<1)return;LAST.put(p,point);
  var trails=TRAILS.computeIfAbsent(l,k->new ArrayDeque<>());while(!trails.isEmpty()&&(trails.peek().until<=now||trails.size()>=192))trails.remove();trails.add(new Trace(p.getUUID(),point,now+500));
  l.sendParticles(new DustParticleOptions(new Vector3f(.46f,.055f,.045f),.65f),point.x,point.y+.06,point.z,3,.13,.025,.13,0);
 }
 public static void remember(Mob m,Vec3 pos){
  if(!Rules.on("predator_memory")||!predator(m)||!(m.level() instanceof ServerLevel l)||m.distanceToSqr(pos)>9)return;
  m.getPersistentData().putLong(FOOD,BlockPos.containing(pos).asLong());m.getPersistentData().putLong(FOOD_TIME,l.getGameTime()+2400);
 }
 public static void hunt(LivingEvent.LivingTickEvent e){
  if(!(e.getEntity() instanceof Mob m)||!(m.level() instanceof ServerLevel l)||m.tickCount%40!=0||m.getTarget()!=null||!m.isAlive()||!m.getNavigation().isDone())return;
  boolean wild=predator(m);if(!wild&&!(m instanceof Zombie)&&!(m instanceof Spider))return;
  long now=l.getGameTime();if(NEXT.getOrDefault(m,0L)>now)return;
  if(Rules.on("blood_trail")){
   var trails=TRAILS.get(l);Trace best=null;double dist=144;
   if(trails!=null){while(!trails.isEmpty()&&trails.peek().until<=now)trails.remove();
    for(Trace trace:trails){double d=m.distanceToSqr(trace.pos);if(d>2.25&&d<dist&&l.hasChunkAt(BlockPos.containing(trace.pos))){dist=d;best=trace;}}
   }
   if(best!=null){Vec3 p=best.pos;if(m.getNavigation().moveTo(p.x,p.y,p.z,1.05)){NEXT.put(m,now+80);return;}}
  }
  if(!Rules.on("predator_memory")||!wild||m.getPersistentData().getLong(FOOD_TIME)<=now)return;
  BlockPos pos=BlockPos.of(m.getPersistentData().getLong(FOOD));double d=m.distanceToSqr(Vec3.atCenterOf(pos));
  if(d<4){m.getPersistentData().remove(FOOD_TIME);return;}
  if(d<=576&&l.hasChunkAt(pos)){m.getNavigation().moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,1);NEXT.put(m,now+160);}
 }
}
