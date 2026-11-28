package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
public final class TreeDescent {
 private TreeDescent(){}
 private record Grip(int x,int z,Direction face,ServerLevel level,long start){}
 private static final Map<Player,Grip> GRIPS=new WeakHashMap<>();
 public static boolean trunk(BlockState s){return s.is(BlockTags.LOGS)&&s.hasProperty(BlockStateProperties.AXIS)&&s.getValue(BlockStateProperties.AXIS)==Direction.Axis.Y;}
 private static boolean eligible(Player p){return Survival.active(p)&&p.isAlive()&&!p.isPassenger()&&!p.isFallFlying()&&!p.isInWater()&&!p.isInLava()&&!Balance.down(p)&&p.isShiftKeyDown()&&p.getMainHandItem().isEmpty()&&Rules.on("tree_descent");}
 private static boolean near(Player p,int x,int z,Direction face){
  double dx=p.getX()-(x+.5),dz=p.getZ()-(z+.5);
  double normal=dx*face.getStepX()+dz*face.getStepZ(),side=dx*face.getStepZ()-dz*face.getStepX();
  return normal>=.45&&normal<=1.05&&Math.abs(side)<=.78;
 }
 private static boolean support(ServerLevel l,Player p,Grip g){
  if(g.level()!=l||!near(p,g.x(),g.z(),g.face()))return false;
  int y=p.blockPosition().getY();
  for(int dy=0;dy<=1;dy++){var pos=new BlockPos(g.x(),y+dy,g.z());if(!l.hasChunkAt(pos)||!trunk(l.getBlockState(pos)))return false;}
  return true;
 }
 private static Grip nearbyGrip(ServerLevel l,Player p){
  if(p.onGround()||p.getDeltaMovement().y>0||p.getDeltaMovement().y<-.8)return null;
  var eye=p.getLookAngle();
  int bx=p.blockPosition().getX(),bz=p.blockPosition().getZ();
  Grip best=null;double nearest=Double.POSITIVE_INFINITY;
  for(int x=bx-1;x<=bx+1;x++)for(int z=bz-1;z<=bz+1;z++)for(Direction face:Direction.Plane.HORIZONTAL){
   if(!near(p,x,z,face)||eye.x*face.getStepX()+eye.z*face.getStepZ()>-.35)continue;
   var grip=new Grip(x,z,face,l,l.getGameTime());
   double distance=Math.hypot(p.getX()-x-.5,p.getZ()-z-.5);
   if(distance<nearest&&support(l,p,grip)){nearest=distance;best=grip;}
  }
  return best;
 }
 private static void release(Player p){if(GRIPS.remove(p)!=null)TreeDescentSync.send(p,false);}
 private static void grip(Player p,Grip g){GRIPS.put(p,g);TreeDescentSync.send(p,true);}
 public static boolean gripping(Player p){return GRIPS.containsKey(p);}
 public static void interact(PlayerInteractEvent.RightClickBlock e){
  if(e.isCanceled()||e.getHand()!=InteractionHand.MAIN_HAND||!(e.getLevel() instanceof ServerLevel l)||!eligible(e.getEntity()))return;
  var p=e.getEntity();var pos=e.getPos();var face=e.getFace();
  if(face==null||!face.getAxis().isHorizontal()||!trunk(l.getBlockState(pos))||p.getEyePosition().distanceToSqr(e.getHitVec().getLocation())>9)return;
  var grip=new Grip(pos.getX(),pos.getZ(),face,l,l.getGameTime());
  if(!support(l,p,grip))return;
  grip(p,grip);e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
 }
 public static void tick(PlayerTickEvent.Post e){
  var p=e.getEntity();
  if(!(p.level() instanceof ServerLevel l)||!eligible(p)||Survival.fatigue(p)>=98||ImpactDynamics.hanging(p)){release(p);return;}
  var grip=GRIPS.get(p);
  if(grip==null){grip=nearbyGrip(l,p);if(grip==null)return;grip(p,grip);}
  if(!support(l,p,grip)){release(p);return;}
  if(p.onGround()||!l.noCollision(p,p.getBoundingBox().move(0,-.12,0))){release(p);return;}
  var v=p.getDeltaMovement();p.setDeltaMovement(v.x*.55,-.12,v.z*.55);p.fallDistance=0;p.hurtMarked=true;
  if(p.tickCount%10==0)TreeDescentSync.send(p,true);
  if(l.getGameTime()%20==0)Survival.work(p,.8);
 }
 public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){release(e.getEntity());}
 static void verify(){
  if(!trunk(net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState())||trunk(net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X))||trunk(net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState())||trunk(net.minecraft.world.level.block.Blocks.OAK_STAIRS.defaultBlockState())||trunk(net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState()))throw new IllegalStateException("tree descent trunk filter");
 }
}
