package dev.minemods.worldlogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
public final class PlatePhysics {
 public static double mass(Level level,BlockPos pos) {
  double total=0;
  AABB box=new AABB(.125,0,.125,.875,.25,.875).move(pos.getX(),pos.getY(),pos.getZ());
  for(Entity e:level.getEntities((Entity)null,box,e->!e.isSpectator()&&!e.isIgnoringBlockTriggers())){
   double m=Material.mass(e);
   if(e instanceof Player p){double added=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(!s.isEmpty())added+=Material.weight(s)*s.getCount()*.0015;}m+=Math.min(1.5,added);}
   total+=m;if(total>24)return 24;
  }return total;
 }
}
