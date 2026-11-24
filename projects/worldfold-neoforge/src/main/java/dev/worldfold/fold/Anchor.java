package dev.worldfold.fold;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
public record Anchor(ResourceKey<Level> dimension, BlockPos floor, Direction normal, double scale){
 public double width(){return 2*scale;}
 public double height(){return 3*scale;}
 public Vec3 center(){return new Vec3(floor.getX()+.5,floor.getY()+1+height()/2,floor.getZ()+.5);}
 public Vec3 axisW(){return new Vec3(0,1,0).cross(Vec3.atLowerCornerOf(normal.getNormal()));}
 public AABB clearance(){Vec3 c=center();double x=normal.getAxis()==Direction.Axis.X?1:width()/2,z=normal.getAxis()==Direction.Axis.Z?1:width()/2;return new AABB(c.x-x,c.y-height()/2+.01,c.z-z,c.x+x,c.y+height()/2-.01,c.z+z);}
 public ServerLevel level(MinecraftServer s){return s.getLevel(dimension);}
 public boolean clear(ServerLevel l){if(l==null||!l.getWorldBorder().isWithinBounds(floor))return false;AABB b=clearance();return b.minY>=l.getMinBuildHeight()&&b.maxY<l.getMaxBuildHeight()&&l.getBlockState(floor).isFaceSturdy(l,floor,Direction.UP)&&!l.getBlockCollisions(null,b).iterator().hasNext();}
 public CompoundTag save(){CompoundTag t=new CompoundTag();t.putString("dim",dimension.location().toString());t.putLong("floor",floor.asLong());t.putString("normal",normal.getName());t.putDouble("scale",scale);return t;}
 public static Anchor load(CompoundTag t){Direction d=Direction.byName(t.getString("normal"));double s=t.getDouble("scale");if(d==null||d.getAxis()==Direction.Axis.Y||(s!=.5&&s!=1&&s!=2))throw new IllegalArgumentException("Invalid anchor");return new Anchor(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(t.getString("dim"))),BlockPos.of(t.getLong("floor")),d,s);}
}