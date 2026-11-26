package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
public final class ObjectsPhysics {
 private static final Map<Entity,Boolean> SEEN=new WeakHashMap<>();
 private record DoorJob(BlockPos pos,long due){}
 private static final Map<ServerLevel,ArrayDeque<DoorJob>> DOORS=new WeakHashMap<>();
 private static final Map<Entity,Long> FALL_HITS=new WeakHashMap<>();
 public static void tick(TickEvent.LevelTickEvent.Post e){if(!(e.level instanceof ServerLevel l))return;long now=l.getGameTime();var jobs=DOORS.get(l);
  if(jobs!=null){int left=16;while(left-->0&&!jobs.isEmpty()&&jobs.peek().due<=now){
   BlockPos q=jobs.remove().pos;if(!l.hasChunkAt(q))continue;var s=l.getBlockState(q);if(!(s.getBlock() instanceof DoorBlock)||s.getValue(BlockStateProperties.OPEN))continue;
   Vec3 away=Vec3.atLowerCornerOf(s.getValue(BlockStateProperties.HORIZONTAL_FACING).getNormal());
   for(Entity x:l.getEntities((Entity)null,new AABB(q).inflate(.06).expandTowards(0,1,0),a->!a.isSpectator()&&(a instanceof LivingEntity||a instanceof ItemEntity))){
    double doorMass=s.is(Blocks.IRON_DOOR)?1.8:1.0;double side=x.position().subtract(Vec3.atCenterOf(q)).dot(away)>=0?1:-1;
    x.push(away.x*side*.16*doorMass/Math.max(.5,Material.mass(x)),.015,away.z*side*.16/Math.max(.5,Material.mass(x)));x.hurtMarked=true;
   }
  }}
  if(now%2!=0)return;
  Set<Integer> processed=new HashSet<>();int budget=384;
  for(Player p:l.players()){
   for(Entity x:l.getEntities(p,p.getBoundingBox().inflate(24),a->a instanceof ItemEntity||a instanceof AbstractArrow||a instanceof net.minecraft.world.entity.item.FallingBlockEntity||a instanceof net.minecraft.world.entity.vehicle.Boat||a instanceof LivingEntity&&a.isInWater())){
    if(!processed.add(x.getId()))continue;if(--budget<0)return;Vec3 v=x.getDeltaMovement();
    if(x instanceof AbstractArrow a){
     if(SEEN.put(a,true)==null&&a.tickCount<5&&a.getPersistentData().getInt("worldlogic_bounces")==0)Body.bow(a);
     if(Rules.on("wind")&&l.isThundering()&&!a.onGround()&&v.lengthSqr()>.25&&l.canSeeSky(a.blockPosition())){double angle=(now/1200L+l.dimension().location().hashCode())*.73;a.setDeltaMovement(v.add(Math.sin(angle)*.0015,0,Math.cos(angle)*.0015));}
    }else if(x instanceof net.minecraft.world.entity.item.FallingBlockEntity fb){
     if(v.y<-.5&&Rules.on("fragile_containers")){BlockPos q=fb.blockPosition().below();if(l.hasChunkAt(q)&&Material.block(l.getBlockState(q),"fragile_container"))l.destroyBlock(q,true,fb);}
     if(Rules.on("falling_objects")&&v.y<-.5&&Material.block(fb.getBlockState(),"falling_hazard")){
      for(LivingEntity target:l.getEntitiesOfClass(LivingEntity.class,fb.getBoundingBox().expandTowards(0,v.y,0),a->!a.isSpectator())){
       if(FALL_HITS.getOrDefault(target,0L)>now)continue;
       FALL_HITS.put(target,now+20);target.hurt(l.damageSources().generic(),(float)Math.min(5,Math.abs(v.y)*2));
      }
     }
    }else if(x instanceof LivingEntity||x instanceof net.minecraft.world.entity.vehicle.Boat){
     if(Rules.on("water_drag")&&x.isInWater()){var flow=l.getFluidState(x.blockPosition()).getFlow(l,x.blockPosition());double m=Material.mass(x);x.setDeltaMovement(v.add(flow.scale(Math.max(-.009,Math.min(.018,.018*(1/m-1))))));}
    }else if(x instanceof ItemEntity i){
     if(Rules.on("spoilage")&&i.getItem().has(net.minecraft.core.component.DataComponents.FOOD)&&!i.getItem().is(Content.SPOILED.get())&&(i.isInWater()||l.isRainingAt(i.blockPosition()))){
      int wet=i.getPersistentData().getInt("worldlogic_food_wet")+2;
      if(wet>=2400){i.setItem(new net.minecraft.world.item.ItemStack(Content.SPOILED.get(),i.getItem().getCount()));i.getPersistentData().remove("worldlogic_food_wet");}
      else i.getPersistentData().putInt("worldlogic_food_wet",wet);
     }
     double mass=Material.mass(i);if(Rules.on("heavy_items")&&mass>1.25&&!i.onGround()&&!i.isInWater())i.setDeltaMovement(v.add(0,-.012*Math.min(2,mass-1),0));
     if(Rules.on("water_drag")&&i.isInWater()){var flow=l.getFluidState(i.blockPosition()).getFlow(l,i.blockPosition());double scale=Math.max(-.012,Math.min(.023,.025*(1/mass-1)));i.setDeltaMovement(i.getDeltaMovement().add(flow.scale(scale)));}
    }
   }
  }
 }
 public static void onNeighbor(net.minecraftforge.event.level.BlockEvent.NeighborNotifyEvent e){
  if(!Rules.on("doors_have_mass")||!(e.getLevel() instanceof ServerLevel l)||e.isCanceled())return;BlockPos q=e.getPos();var s=e.getState();if(!(s.getBlock() instanceof DoorBlock)||s.getValue(BlockStateProperties.OPEN)||!s.is(Blocks.IRON_DOOR))return;
  if(s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)q=q.below();
  var jobs=DOORS.computeIfAbsent(l,k->new ArrayDeque<>());if(jobs.size()<128)jobs.add(new DoorJob(q.immutable(),l.getGameTime()+1));
 }
 public static void onInteract(PlayerInteractEvent.RightClickBlock e){if(!Rules.on("doors_have_mass")||!(e.getLevel() instanceof ServerLevel l)||e.isCanceled()||e.getEntity().isShiftKeyDown())return;
  var s=l.getBlockState(e.getPos());if(!(s.getBlock() instanceof DoorBlock)||!s.getValue(BlockStateProperties.OPEN))return;
  BlockPos q=e.getPos();if(s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)q=q.below();
  var jobs=DOORS.computeIfAbsent(l,k->new ArrayDeque<>());if(jobs.size()<128)jobs.add(new DoorJob(q.immutable(),l.getGameTime()+1));
 }
}
