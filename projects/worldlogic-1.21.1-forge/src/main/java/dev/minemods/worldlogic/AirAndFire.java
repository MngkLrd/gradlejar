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
 private static final Map<ServerLevel,List<Pocket>> POCKETS=new WeakHashMap<>();
 private static final Map<ServerLevel,Map<BlockPos,Long>> HEAT=new WeakHashMap<>();
 static boolean nearHeat(ServerLevel l,BlockPos p,int r){for(BlockPos q:BlockPos.betweenClosed(p.offset(-r,-1,-r),p.offset(r,1,r))){if(!l.hasChunkAt(q))continue;var s=l.getBlockState(q);if(s.is(Blocks.FIRE)||s.is(Blocks.LAVA)||s.getBlock() instanceof CampfireBlock&&s.getValue(BlockStateProperties.LIT))return true;}return false;}
 static void breathe(ServerLevel l,Player p,Body.State st,boolean water,long now){
  if(water||p.hasEffect(MobEffects.WATER_BREATHING)){st.pocket=null;st.pocketCheck=0;return;}
  BlockPos head=BlockPos.containing(p.getX(),p.getEyeY(),p.getZ());
  if(now>=st.pocketCheck){st.pocketCheck=now+40;st.pocket=null;
   if(head.getY()<l.getSeaLevel()&&!l.canSeeSky(head)&&l.isEmptyBlock(head)){
    Set<BlockPos> cells=new HashSet<>();ArrayDeque<BlockPos> todo=new ArrayDeque<>();todo.add(head);cells.add(head);
    boolean open=false,wet=false;
    while(!todo.isEmpty()&&!open){BlockPos q=todo.remove();if(l.canSeeSky(q)||cells.size()>128){open=true;break;}
     for(Direction d:Direction.values()){BlockPos t=q.relative(d);if(!l.hasChunkAt(t)||!l.isInWorldBounds(t)){open=true;break;}if(l.getFluidState(t).is(net.minecraft.tags.FluidTags.WATER))wet=true;else if(l.isEmptyBlock(t)&&cells.add(t.immutable()))todo.add(t.immutable());}
    }
    if(!open&&wet){var list=POCKETS.computeIfAbsent(l,k->new ArrayList<>());list.removeIf(a->now-a.last>24000);
     for(Pocket a:list)if(a.cells.contains(head)){st.pocket=a;break;}
     if(st.pocket==null&&list.size()<256){st.pocket=new Pocket(cells,now);list.add(st.pocket);}
    }
   }
  }
  Pocket a=st.pocket;if(a==null||!a.cells.contains(head))return;a.last=now;a.oxygen=Math.max(0,a.oxygen-1);
  if(a.oxygen==0){int air=Math.min(p.getAirSupply(),st.lastAir)-1;if(air<=-20){p.setAirSupply(0);p.hurt(p.damageSources().drown(),2);}else p.setAirSupply(air);}
  else if(a.oxygen<a.cells.size()*60&&p.getAirSupply()>st.lastAir)p.setAirSupply(Math.min(p.getAirSupply(),st.lastAir+1));
 }
 public static void tick(TickEvent.PlayerTickEvent.Post e){var p=e.player;if(!(p.level() instanceof ServerLevel l)||p.isSpectator()||l.getGameTime()%40!=0)return;long now=l.getGameTime();
  var map=HEAT.computeIfAbsent(l,k->new HashMap<>());if(map.size()>512)map.clear();map.entrySet().removeIf(a->a.getValue()<now);
  // Sample bounded local positions instead of scanning every loaded block.
  for(int i=0;i<32;i++){BlockPos q=p.blockPosition().offset(l.random.nextInt(13)-6,l.random.nextInt(7)-3,l.random.nextInt(13)-6);if(!l.hasChunkAt(q))continue;var s=l.getBlockState(q);
   if(s.getBlock() instanceof CampfireBlock&&s.getValue(BlockStateProperties.LIT)){
    if(Rules.on("residual_heat")||Rules.on("thermal_shock"))for(Direction d:Direction.values())map.put(q.relative(d),now+200);
    if(Rules.on("draft_smoke")){for(Direction d:Direction.Plane.HORIZONTAL){BlockPos t=q.above().relative(d);if(l.isEmptyBlock(t)&&l.canSeeSky(t)){l.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,q.getX()+.5+d.getStepX()*.35,q.getY()+1.1,q.getZ()+.5+d.getStepZ()*.35,1,.1,.05,.1,.01);break;}}}
    if(Rules.on("fire_needs_air")){boolean enclosed=true;for(Direction d:Direction.values())if(d!=Direction.DOWN&&l.isEmptyBlock(q.relative(d)))enclosed=false;if(enclosed&&l.random.nextFloat()<.15)l.setBlock(q,s.setValue(BlockStateProperties.LIT,false),3);}
    if(Rules.on("campfire_sparks")&&l.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)&&!l.isRainingAt(q)&&l.random.nextFloat()<.02){BlockPos t=q.relative(Direction.Plane.HORIZONTAL.getRandomDirection(l.random));if(l.isEmptyBlock(t)&&Material.block(l.getBlockState(t.below()),"spark_fuel"))l.setBlock(t,Blocks.FIRE.defaultBlockState(),3);}
   }
   if(Rules.on("wet_torches")&&l.isRainingAt(q)&&(s.is(Blocks.TORCH)||s.is(Blocks.WALL_TORCH)))l.sendParticles(ParticleTypes.SMOKE,q.getX()+.5,q.getY()+.7,q.getZ()+.5,2,.05,.1,.05,.01);
  }
  int budget=20;for(var entry:map.entrySet()){if(budget--<=0)break;BlockPos q=entry.getKey();if(!l.hasChunkAt(q))continue;var s=l.getBlockState(q);boolean wet=l.isRainingAt(q.above())||l.getFluidState(q.above()).is(net.minecraft.tags.FluidTags.WATER);
   if(wet){if(Rules.on("residual_heat"))l.sendParticles(ParticleTypes.CLOUD,q.getX()+.5,q.getY()+1,q.getZ()+.5,1,.15,.05,.15,.01);if(Rules.on("thermal_shock")&&Material.block(s,"glass")&&l.random.nextFloat()<.025)l.destroyBlock(q,false);}
  }
 }
 public static void onExplosion(ExplosionEvent.Detonate e){if(!Rules.on("ashes")||!(e.getLevel() instanceof ServerLevel l))return;int n=0;for(BlockPos p:e.getAffectedBlocks()){if(n++>=24)break;l.sendParticles(ParticleTypes.ASH,p.getX()+.5,p.getY()+.5,p.getZ()+.5,2,.3,.3,.3,.01);}}
}
