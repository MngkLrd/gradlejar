package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
public final class Structure {
 record Work(BlockPos pos,long due,int depth){}
 private static final Map<ServerLevel,ArrayDeque<Work>> QUEUE=new WeakHashMap<>();
 private static void enqueue(ServerLevel l,BlockPos p){enqueue(l,p,0);}
 private static void enqueue(ServerLevel l,BlockPos p,int depth){if(depth>3)return;var q=QUEUE.computeIfAbsent(l,k->new ArrayDeque<>());if(q.size()<512)q.add(new Work(p.immutable(),l.getGameTime()+1,depth));}
 public static void onBreak(BlockEvent.BreakEvent e){if(!Rules.on("loose_gravel")||e.isCanceled()||!(e.getLevel() instanceof ServerLevel l)||e.getPlayer().isCreative())return;
  for(Direction d:Direction.Plane.HORIZONTAL)enqueue(l,e.getPos().relative(d));
  for(Direction d:Direction.Plane.HORIZONTAL)enqueue(l,e.getPos().above().relative(d));
 }
 public static void onTick(TickEvent.LevelTickEvent.Post e){if(!(e.level instanceof ServerLevel l))return;var q=QUEUE.get(l);if(q==null)return;int budget=12;
  while(budget-->0&&!q.isEmpty()&&q.peek().due<=l.getGameTime()){
   Work work=q.remove();BlockPos p=work.pos;if(!l.hasChunkAt(p)||!Rules.on("loose_gravel"))continue;BlockState s=l.getBlockState(p);if(!Material.block(s,"unstable")||!(s.getBlock() instanceof FallingBlock))continue;
   for(Direction d:Direction.Plane.HORIZONTAL){BlockPos t=p.relative(d);if(!l.hasChunkAt(t)||!l.hasChunkAt(t.below()))continue;
    if(l.isEmptyBlock(t)&&l.isEmptyBlock(t.below())&&l.random.nextFloat()<Rules.chance("impact",.28)){l.removeBlock(p,false);l.setBlock(t,s,3);l.scheduleTick(t,s.getBlock(),2);for(Direction next:Direction.Plane.HORIZONTAL)enqueue(l,p.relative(next),work.depth+1);enqueue(l,p.above(),work.depth+1);break;}
   }
  }
 }
 public static void onImpact(ProjectileImpactEvent e){if(!(e.getProjectile().level() instanceof ServerLevel l)||!(e.getRayTraceResult() instanceof BlockHitResult hit)||!(e.getProjectile() instanceof AbstractArrow))return;
  if(!(e.getProjectile().getOwner() instanceof net.minecraft.world.entity.player.Player)&&!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))return;
  if(e.getImpactResult()!=ProjectileImpactEvent.ImpactResult.DEFAULT||e.getProjectile().getDeltaMovement().length()<.45)return;BlockPos p=hit.getBlockPos();if(!l.hasChunkAt(p))return;BlockState s=l.getBlockState(p);
  if(Rules.on("fragile_lanterns")&&s.getBlock() instanceof LanternBlock&&s.getValue(LanternBlock.HANGING)){l.destroyBlock(p,true,e.getProjectile());return;}
  if(Rules.on("fragile_containers")&&Material.block(s,"fragile_container")){l.destroyBlock(p,true,e.getProjectile());return;}
  if(Rules.on("falling_dripstone")&&s.is(Blocks.POINTED_DRIPSTONE)&&s.getValue(PointedDripstoneBlock.TIP_DIRECTION)==Direction.DOWN){FallingBlockEntity.fall(l,p,s);return;}
  if(e.getProjectile() instanceof AbstractArrow a&&(a.getType()==net.minecraft.world.entity.EntityType.ARROW||a.getType()==net.minecraft.world.entity.EntityType.SPECTRAL_ARROW)&&a.getPierceLevel()==0){
   Vec3 normal=Vec3.atLowerCornerOf(hit.getDirection().getNormal());Vec3 v=a.getDeltaMovement();
   boolean metal=Material.block(s,"ricochet_metal"),hard=metal||Material.block(s,"ricochet_stone");
   double angle=Math.abs(v.normalize().dot(normal));int bounces=a.getPersistentData().getInt("worldlogic_bounces");
   if(Rules.on("ricochet")&&Material.surfaceBounce(s)>0&&angle<(metal?.48:hard?.28:.09)&&bounces<2&&v.length()>1.0){
    var clone=a.getType().create(l);
    if(clone instanceof AbstractArrow b){
     var tag=a.saveWithoutId(new net.minecraft.nbt.CompoundTag());tag.remove("UUID");b.load(tag);b.setUUID(java.util.UUID.randomUUID());
     b.getPersistentData().putInt("worldlogic_bounces",bounces+1);
     b.setPos(hit.getLocation().x+normal.x*.25,hit.getLocation().y+normal.y*.25,hit.getLocation().z+normal.z*.25);
     b.setDeltaMovement(new Impulse(v,.2).reflect(normal,Material.surfaceBounce(s)));
     if(l.addFreshEntity(b)){e.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);return;}
    }
   }
   if(Rules.on("arrow_recovery")){
    if(hard&&v.length()>2&&l.random.nextFloat()<Rules.chance("impact",.08))e.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
    else if(Material.block(s,"wood")||Material.block(s,"soft_projectile")||s.is(Blocks.DIRT))a.getPersistentData().putBoolean("worldlogic_recoverable",true);
   }
   if(Rules.on("ricochet")&&Material.block(s,"soft_projectile"))a.setDeltaMovement(v.scale(.55));
  }
 }
 public static void onExplosion(ExplosionEvent.Detonate e){
  if(!(e.getLevel() instanceof ServerLevel l)||e.getAffectedBlocks().isEmpty())return;
  if(!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))return;
  Set<BlockPos> extra=new HashSet<>();int budget=96;
  for(BlockPos p:List.copyOf(e.getAffectedBlocks())){if(budget--<=0)break;for(Direction d:Direction.values()){BlockPos t=p.relative(d);if(!l.hasChunkAt(t))continue;var b=l.getBlockState(t);if(Rules.on("loose_gravel")&&Material.block(b,"unstable"))enqueue(l,t);
   if(Rules.on("falling_dripstone")&&b.is(Blocks.POINTED_DRIPSTONE)&&b.getValue(PointedDripstoneBlock.TIP_DIRECTION)==Direction.DOWN){if(l.random.nextFloat()<Rules.chance("impact",.3))FallingBlockEntity.fall(l,t,b);}
   else if(Rules.on("glass_pressure")&&Material.block(b,"glass")||Rules.on("fragile_containers")&&Material.block(b,"fragile_container")||Rules.on("fragile_lanterns")&&b.getBlock() instanceof LanternBlock&&b.getValue(LanternBlock.HANGING))extra.add(t.immutable());if(extra.size()>=24)break;}if(extra.size()>=24)break;}
  for(BlockPos p:extra)if(!e.getAffectedBlocks().contains(p))e.getAffectedBlocks().add(p);
 }
}
