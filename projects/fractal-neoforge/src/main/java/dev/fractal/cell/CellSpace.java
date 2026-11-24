package dev.fractal.cell;
import dev.fractal.Fractal;
import dev.fractal.block.SocketBlock;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class CellSpace {
 public static final ResourceKey<Level> KEY=ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath("fractal","matter"));
 public static final int SPACING=96,Y=80;
 public static int index(BlockPos p){int x=Math.floorDiv(p.getX(),SPACING),z=Math.floorDiv(p.getZ(),SPACING);return x<0||x>=16||z<0||z>=16?-1:z*16+x;}
 public static BlockPos center(int id){return new BlockPos((id%16)*SPACING+48,Y,(id/16)*SPACING+48);}
 public static BlockPos socket(int id,int n){return center(id).offset((n%3-1)*5,2,n<3?-6:6);}
 public static BlockPos input(int id){return center(id).offset(-8,1,0);}
 public static BlockPos output(int id){return center(id).offset(8,1,0);}
 public static BlockPos exit(int id){return center(id).offset(0,1,-9);}
 public static boolean inside(CellData.Cell c,BlockPos p){BlockPos b=center(c.id);return Math.abs(p.getX()-b.getX())<11&&Math.abs(p.getZ()-b.getZ())<11&&p.getY()>Y&&p.getY()<Y+12;}
 public static int socketIndex(CellData.Cell c,BlockPos p){for(int i=0;i<6;i++)if(socket(c.id,i).equals(p))return i;return -1;}
 public static boolean build(MinecraftServer server,CellData.Cell c){
  ServerLevel l=server.getLevel(KEY);if(l==null)return false;
  BlockPos center=center(c.id);BlockState matrix=c.material.matrix().defaultBlockState(),crystal=Fractal.CRYSTAL.get().defaultBlockState();
  // Shell is non-harvestable, so generating a chamber never generates free ore or building materials.
  for(int x=-11;x<=11;x++)for(int z=-11;z<=11;z++){
   boolean edge=Math.abs(x)==11||Math.abs(z)==11;
   boolean seam=c.material==Material.COPPER?(x+z)%7==0:c.material==Material.WOOD?Math.abs(x)%5==0:Math.abs(x*z)%17==0;
   l.setBlock(center.offset(x,0,z),seam?crystal:matrix,2);
   l.setBlock(center.offset(x,12,z),(x%6==0&&z%6==0)?crystal:matrix,2);
   if(edge)for(int y=1;y<12;y++){
    boolean accent=c.material==Material.WOOD?(x+z)%5==0:c.material==Material.COPPER?(y==4||y==8):(x+z+y)%9==0;
    l.setBlock(center.offset(x,y,z),accent?crystal:matrix,2);
   }
  }
  // Different structural geometry in each material, not just palette swaps.
  if(c.material==Material.STONE){
   for(int x:new int[]{-8,8})for(int z:new int[]{-5,5})for(int y=1;y<=4+(x*z>0?2:0);y++){
    l.setBlock(center.offset(x,y,z),y%3==0?crystal:matrix,2);
    if(y<=2)l.setBlock(center.offset(x/Math.abs(x)+x,y,z),matrix,2);
   }
  }else if(c.material==Material.COPPER){
   for(int z:new int[]{-8,8})for(int x=-9;x<=9;x++)l.setBlock(center.offset(x,8,z),x%4==0?crystal:matrix,2);
   for(int x:new int[]{-9,9})for(int z=-8;z<=8;z++)l.setBlock(center.offset(x,8,z),z%4==0?crystal:matrix,2);
   for(int x:new int[]{-9,9})for(int z:new int[]{-8,8})for(int y=1;y<8;y++)l.setBlock(center.offset(x,y,z),matrix,2);
  }else{
   for(int x:new int[]{-8,8})for(int z:new int[]{-4,4})for(int y=1;y<=9;y++){
    l.setBlock(center.offset(x,y,z),matrix,2);
    if(y>=7)l.setBlock(center.offset(x-Integer.signum(x)*(y-6),y,z),y==9?crystal:matrix,2);
   }
  }
  for(int i=0;i<6;i++){
   BlockPos p=socket(c.id,i);l.setBlock(p.below(),matrix,2);
   l.setBlock(p,Fractal.SOCKET.get().defaultBlockState().setValue(SocketBlock.TYPE,c.material.initial(i)),3);
  }
  for(int k=-8;k<=8;k+=4){
   if(k==0)continue;
   for(int y=1;y<=3;y++)l.setBlock(center.offset(k,y,-10),y==3?crystal:matrix,2);
  }
  l.setBlock(input(c.id),Fractal.INPUT.get().defaultBlockState(),3);
  l.setBlock(output(c.id),Fractal.OUTPUT.get().defaultBlockState(),3);
  l.setBlock(exit(c.id),Fractal.EXIT.get().defaultBlockState(),3);
  return true;
 }
 public static void enter(ServerPlayer p,CellData.Cell c){
  ServerLevel l=p.getServer().getLevel(KEY);if(l==null)return;
  BlockPos b=center(c.id).above();
  BlockPos safe=findSafe(l,b,10,0,10);
  if(safe==null){p.sendSystemMessage(net.minecraft.network.chat.Component.literal("FRACTAL: interior landing blocked."));return;}
  teleport(p,l,safe);
  p.sendSystemMessage(net.minecraft.network.chat.Component.literal("FRACTAL | "+switch(c.depth){case 1->"MESO 1:8";case 2->"MICRO 1:64";default->"CORE 1:512";}+" | "+c.material.name()+" | Cell #"+c.id));
 }
 public static void ascend(ServerPlayer p,CellData data,CellData.Cell c){
  if(c!=null){
   // Skip collapsed ancestors; exit through the highest destroyed container's former address.
   CellData.Cell target=c;
   while(target.parent>=0){CellData.Cell parent=data.cells.get(target.parent);if(parent==null||data.live(parent))break;target=parent;}
   ServerLevel l=p.getServer().getLevel(target.address.dimension());
   if(l!=null){
    BlockPos b=target.address.position().above();
    BlockPos safe=findSafe(l,b,6,-2,4);
    if(l.dimension().equals(KEY)){
     CellData.Cell parent=data.cells.get(target.parent);
     if(parent==null||!data.live(parent)){safe=null;}
     else if(safe==null||!inside(parent,safe))safe=findSafe(l,center(parent.id).above(),9,0,10);
    }
    if(safe!=null){teleport(p,l,safe);return;}
   }
  }
  ServerLevel l=p.getServer().overworld();
  BlockPos b=l.getSharedSpawnPos();BlockPos safe=findSafe(l,b,8,-2,8);teleport(p,l,safe==null?b.above():safe);
 }
 private static BlockPos findSafe(ServerLevel l,BlockPos b,int radius,int minDy,int maxDy){
  for(int r=0;r<=radius;r++)for(int dy=minDy;dy<=maxDy;dy++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
   if(Math.max(Math.abs(x),Math.abs(z))!=r)continue;BlockPos p=b.offset(x,dy,z);
   if(p.getY()<=l.getMinBuildHeight()||p.getY()+2>=l.getMaxBuildHeight()||!l.getWorldBorder().isWithinBounds(p))continue;
   if(l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP)&&l.getFluidState(p).isEmpty()&&l.getFluidState(p.above()).isEmpty()&&!l.getBlockCollisions(null,new AABB(p.getX()+.15,p.getY(),p.getZ()+.15,p.getX()+.85,p.getY()+1.9,p.getZ()+.85)).iterator().hasNext())return p;
  }return null;
 }
 private static void teleport(ServerPlayer p,ServerLevel l,BlockPos b){p.teleportTo(l,b.getX()+.5,b.getY(),b.getZ()+.5,p.getYRot(),p.getXRot());p.resetFallDistance();}
}
