package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ExplosionEvent;
public final class AirAndFire {
 static final class Pocket {final Set<BlockPos> cells;int oxygen;long last;Pocket(Set<BlockPos> cells,long now){this.cells=cells;oxygen=cells.size()*240;last=now;}}

 private static final Map<ServerLevel,Map<BlockPos,Long>> HEAT=new WeakHashMap<>();
 // Local ventilation path: bounded air-cell BFS, no forced chunk loads.
 static BlockPos vent(ServerLevel l,BlockPos start){
  if(!l.hasChunkAt(start)||!l.isEmptyBlock(start))return null;
  Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> todo=new ArrayDeque<>();seen.add(start);todo.add(start);
  while(!todo.isEmpty()&&seen.size()<=128){BlockPos q=todo.remove();if(l.canSeeSky(q))return q;
   for(Direction d:Direction.values()){BlockPos t=q.relative(d);if(!l.hasChunkAt(t)||!l.isInWorldBounds(t))return start;if(l.isEmptyBlock(t)&&seen.add(t.immutable()))todo.add(t);}
  }
  return seen.size()>128?start:null;
 }
 static boolean nearHeat(ServerLevel l,BlockPos p,int r){for(BlockPos q:BlockPos.betweenClosed(p.offset(-r,-1,-r),p.offset(r,1,r))){if(!l.hasChunkAt(q))continue;var s=l.getBlockState(q);if(s.is(Blocks.FIRE)||s.is(Blocks.SOUL_FIRE)||s.is(Blocks.LAVA)||s.is(Blocks.MAGMA_BLOCK)||s.hasProperty(BlockStateProperties.LIT)&&s.getValue(BlockStateProperties.LIT)&&(s.is(Blocks.FURNACE)||s.is(Blocks.SMOKER)||s.is(Blocks.BLAST_FURNACE))||s.getBlock() instanceof CampfireBlock&&s.getValue(BlockStateProperties.LIT))return true;}return false;}
 static void breathe(ServerLevel l,Player p,Body.State st,boolean water,long now){
  if(water||p.hasEffect(MobEffects.WATER_BREATHING)){st.pocket=null;st.pocketCheck=0;return;}
  BlockPos head=BlockPos.containing(p.getX(),p.getEyeY(),p.getZ());
  if(now>=st.pocketCheck){st.pocketCheck=now+40;st.pocket=null;
   if(l.isEmptyBlock(head)&&(!l.canSeeSky(head)||st.water)){
    Set<BlockPos> cells=new HashSet<>();ArrayDeque<BlockPos> todo=new ArrayDeque<>();todo.add(head);cells.add(head);
    boolean open=false,wet=false;
    while(!todo.isEmpty()&&!open){BlockPos q=todo.remove();if(cells.size()>128||l.canSeeSky(q)&&q.getY()>=l.getSeaLevel()){open=true;break;}
     for(Direction d:Direction.values()){BlockPos t=q.relative(d);if(!l.hasChunkAt(t)||!l.isInWorldBounds(t)){open=true;break;}if(l.getFluidState(t).is(net.minecraft.tags.FluidTags.WATER))wet=true;else if(l.isEmptyBlock(t)&&cells.add(t.immutable()))todo.add(t.immutable());}
    }
    if(open||!wet){var data=PocketData.get(l);if(data.pockets.removeIf(a->a.cells.contains(head)))data.setDirty();}
    if(!open&&wet){var data=PocketData.get(l);var list=data.pockets;
     int overlapOxygen=0,overlap=0;for(Pocket old:list){if(old.cells.stream().anyMatch(cells::contains)){overlapOxygen+=old.oxygen;overlap+=old.cells.size();}}
     if(overlap>0)list.removeIf(a->a.cells.stream().anyMatch(cells::contains)&&!a.cells.equals(cells));data.setDirty();
     for(Pocket a:list)if(a.cells.equals(cells)){st.pocket=a;break;}
     if(st.pocket==null&&list.size()<256){st.pocket=new Pocket(cells,now);if(overlap>0)st.pocket.oxygen=Math.min(st.pocket.oxygen,overlapOxygen*cells.size()/overlap);list.add(st.pocket);}
    }
   }
  }
  Pocket a=st.pocket;if(a==null||!a.cells.contains(head))return;a.last=now;a.oxygen=Math.max(0,a.oxygen-1);PocketData.get(l).setDirty();
  if(a.oxygen==0){int air=Math.min(p.getAirSupply(),st.lastAir)-1;if(air<=-20){p.setAirSupply(0);p.hurt(p.damageSources().drown(),2);}else p.setAirSupply(air);}
  else if(a.oxygen<a.cells.size()*60&&p.getAirSupply()>st.lastAir)p.setAirSupply(Math.min(p.getAirSupply(),st.lastAir+1));
 }
 public static void tick(TickEvent.PlayerTickEvent.Post e){var p=e.player;if(!(p.level() instanceof ServerLevel l)||p.isSpectator()||l.getGameTime()%40!=0)return;long now=l.getGameTime();
  var map=HEAT.computeIfAbsent(l,k->new HashMap<>());if(map.size()>512)map.clear();map.entrySet().removeIf(a->a.getValue()<now);
  // Sample bounded local positions instead of scanning every loaded block.
  for(int i=0;i<32;i++){BlockPos q=p.blockPosition().offset(l.random.nextInt(13)-6,l.random.nextInt(7)-3,l.random.nextInt(13)-6);if(!l.hasChunkAt(q))continue;var s=l.getBlockState(q);
   if((Rules.on("residual_heat")||Rules.on("thermal_shock"))&&(s.is(Blocks.LAVA)||s.is(Blocks.FIRE)||s.is(Blocks.SOUL_FIRE)||s.is(Blocks.MAGMA_BLOCK)||s.hasProperty(BlockStateProperties.LIT)&&s.getValue(BlockStateProperties.LIT)&&(s.is(Blocks.FURNACE)||s.is(Blocks.SMOKER)||s.is(Blocks.BLAST_FURNACE))))for(Direction d:Direction.values())map.put(q.relative(d),now+200);
   if(s.getBlock() instanceof CampfireBlock&&s.getValue(BlockStateProperties.LIT)){
    if(Rules.on("residual_heat")||Rules.on("thermal_shock"))for(Direction d:Direction.values())map.put(q.relative(d),now+200);
    BlockPos exit=(Rules.on("draft_smoke")||Rules.on("fire_needs_air"))?vent(l,q.above()):q.above();
    if(Rules.on("draft_smoke")&&exit!=null&&!exit.equals(q.above())){var v=net.minecraft.world.phys.Vec3.atCenterOf(exit).subtract(net.minecraft.world.phys.Vec3.atCenterOf(q)).normalize();l.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,q.getX()+.5+v.x*.5,q.getY()+1.0,q.getZ()+.5+v.z*.5,1,.05,.05,.05,.01);}
    if(Rules.on("fire_needs_air")&&exit==null&&l.random.nextFloat()<.12)l.setBlock(q,s.setValue(BlockStateProperties.LIT,false),3);
    if(Rules.on("campfire_sparks")&&l.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)&&!l.isRainingAt(q)&&l.random.nextFloat()<.02){BlockPos t=q.relative(Direction.Plane.HORIZONTAL.getRandomDirection(l.random));if(l.isEmptyBlock(t)&&Material.block(l.getBlockState(t.below()),"spark_fuel"))l.setBlock(t,Blocks.FIRE.defaultBlockState(),3);}
   }
  }
  int budget=20;for(var entry:map.entrySet()){if(budget--<=0)break;BlockPos q=entry.getKey();if(!l.hasChunkAt(q))continue;var s=l.getBlockState(q);boolean wet=l.isRainingAt(q.above())||l.getFluidState(q.above()).is(net.minecraft.tags.FluidTags.WATER);
   if(wet){if(Rules.on("residual_heat"))l.sendParticles(ParticleTypes.CLOUD,q.getX()+.5,q.getY()+1,q.getZ()+.5,1,.15,.05,.15,.01);if(Rules.on("thermal_shock")&&l.random.nextFloat()<.025){if(Material.block(s,"glass"))l.destroyBlock(q,false);else if(s.is(Blocks.STONE))l.setBlock(q,Blocks.COBBLESTONE.defaultBlockState(),3);else if(s.is(Blocks.STONE_BRICKS))l.setBlock(q,Blocks.CRACKED_STONE_BRICKS.defaultBlockState(),3);else if(s.is(Blocks.DEEPSLATE_BRICKS))l.setBlock(q,Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(),3);}}
  }
 }
 public static void onExplosion(ExplosionEvent.Detonate e){if(!Rules.on("ashes")||!(e.getLevel() instanceof ServerLevel l))return;int n=0;for(BlockPos p:e.getAffectedBlocks()){if(n++>=24)break;l.sendParticles(ParticleTypes.ASH,p.getX()+.5,p.getY()+.5,p.getZ()+.5,2,.3,.3,.3,.01);}}
}
