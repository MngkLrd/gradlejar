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
 private static final Map<ServerLevel,Long> LAST=new WeakHashMap<>();
 public static void tick(TickEvent.LevelTickEvent.Post e){if(!(e.level instanceof ServerLevel l))return;long now=l.getGameTime();if(now%2!=0||LAST.put(l,now)!=null&&LAST.get(l)!=now)return;
  Set<Integer> processed=new HashSet<>();int budget=384;
  for(Player p:l.players()){
   for(Entity x:l.getEntities(p,p.getBoundingBox().inflate(24),a->a instanceof ItemEntity||a instanceof AbstractArrow)){
    if(!processed.add(x.getId()))continue;if(--budget<0)return;Vec3 v=x.getDeltaMovement();
    if(x instanceof AbstractArrow a){
     if(SEEN.put(a,true)==null&&a.tickCount<5)Body.bow(a);
     if(Rules.on("wind")&&l.isThundering()&&!a.onGround()&&v.lengthSqr()>.25&&l.canSeeSky(a.blockPosition())){double angle=(now/1200L+l.dimension().location().hashCode())*.73;a.setDeltaMovement(v.add(Math.sin(angle)*.0015,0,Math.cos(angle)*.0015));}
    }else if(x instanceof ItemEntity i){
     double mass=Material.mass(i);if(Rules.on("heavy_items")&&mass>1.25&&!i.onGround()&&!i.isInWater())i.setDeltaMovement(v.add(0,-.012*Math.min(2,mass-1),0));
     if(Rules.on("water_drag")&&i.isInWater()){var flow=l.getFluidState(i.blockPosition()).getFlow(l,i.blockPosition());double scale=Math.max(-.012,Math.min(.023,.025*(1/mass-1)));i.setDeltaMovement(i.getDeltaMovement().add(flow.scale(scale)));}
    }
   }
  }
 }
 public static void onInteract(PlayerInteractEvent.RightClickBlock e){if(!Rules.on("doors_have_mass")||!(e.getLevel() instanceof ServerLevel l)||e.isCanceled()||e.getEntity().isShiftKeyDown())return;
  var s=l.getBlockState(e.getPos());if(!(s.getBlock() instanceof DoorBlock)||!s.getValue(BlockStateProperties.OPEN)||s.is(Blocks.IRON_DOOR))return;
  // Apply only after confirming the actual closing state on the next level tick is a future improvement.
  // This interaction does not mutate entities before another mod can cancel the door action.
 }
}
