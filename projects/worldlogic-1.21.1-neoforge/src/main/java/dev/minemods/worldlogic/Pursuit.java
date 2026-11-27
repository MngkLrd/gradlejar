package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
public final class Pursuit {
 private static final TagKey<EntityType<?>> HUNTERS=TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("worldlogic","persistent_hunters"));
 private record Memory(Vec3 position,long until){}
 private static final Map<Mob,Memory> MEMORIES=new WeakHashMap<>();
 public static void tick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post e){
  if(!Rules.on("persistent_pursuit")||!(e.getEntity() instanceof Mob mob)||!(mob.level() instanceof ServerLevel l)||!mob.getType().is(HUNTERS)||mob.tickCount%20!=0||!mob.isAlive())return;
  long now=l.getGameTime();var target=mob.getTarget();
  if(target!=null){
   if(target instanceof Player p&&p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&mob.distanceToSqr(p)<=400&&mob.getSensing().hasLineOfSight(p)){
    MEMORIES.put(mob,new Memory(p.position(),now+(long)(200*Rules.strength("ecology"))));
   }else if(!(target instanceof Player))MEMORIES.remove(mob);
   return;
  }
  Memory memory=MEMORIES.get(mob);if(memory==null)return;Vec3 pos=memory.position;
  if(now>=memory.until||mob.distanceToSqr(pos)>625||mob.distanceToSqr(pos)<2.25){MEMORIES.remove(mob);return;}
  if(!l.hasChunkAt(net.minecraft.core.BlockPos.containing(pos)))return;
  // Only revisit the last seen location, never the player's hidden real position.
  if(mob.getNavigation().isDone())mob.getNavigation().moveTo(pos.x,pos.y,pos.z,1.05);
 }
}
