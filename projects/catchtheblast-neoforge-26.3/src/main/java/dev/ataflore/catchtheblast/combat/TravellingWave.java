package dev.ataflore.catchtheblast.combat;

import dev.ataflore.catchtheblast.config.BlastConfig;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class TravellingWave {
    private static final Map<MinecraftServer,List<Wave>> ACTIVE = new IdentityHashMap<>();
    private static final int MAX_WAVES = 24;
    private static final int MAX_BREAK_PER_TICK = 96;
    private TravellingWave() {}
    private static final class Wave {
        final ServerLevel level;
        final UUID owner;
        final Vec3 origin, dir;
        final double range;
        final float strength;
        final Set<BlockPos> visited = new HashSet<>();
        double distance = 0.7;
        Wave(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 dir, double range, float strength) {
            this.level=level; this.owner=player.getUUID(); this.origin=origin; this.dir=dir; this.range=range; this.strength=strength;
        }
    }
    public static void launch(ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 dir, double range, float strength) {
        List<Wave> list=ACTIVE.computeIfAbsent(level.getServer(), k->new ArrayList<>());
        if(list.size()<MAX_WAVES) list.add(new Wave(level,player,origin,dir,range,strength));
    }
    public static void stop(ServerStoppedEvent event) { ACTIVE.remove(event.getServer()); }
    public static void tick(ServerTickEvent.Post event) {
        List<Wave> list=ACTIVE.get(event.getServer());
        if(list==null) return;
        int budget=MAX_BREAK_PER_TICK;
        for(Iterator<Wave> it=list.iterator();it.hasNext();) {
            Wave w=it.next();
            ServerPlayer player=event.getServer().getPlayerList().getPlayer(w.owner);
            if(player==null || player.level()!=w.level || !player.isAlive()) { it.remove(); continue; }
            w.distance=Math.min(w.range,w.distance+0.72);
            Vec3 center=w.origin.add(w.dir.scale(w.distance));
            double radius=.25+w.distance*.53;
            BlastFx.waveRing(w.level,center,w.dir,radius,w.strength);
            if (BlastConfig.terrainDamage() && w.strength>=.2f && w.distance>=1.4 && budget>0)
                budget-=carve(w,player,center,Math.min(2.5, .65+radius*.48),Math.min(24,budget));
            if(w.distance>=w.range) it.remove();
        }
        if(list.isEmpty()) ACTIVE.remove(event.getServer());
    }
    private static int carve(Wave w,ServerPlayer player,Vec3 center,double radius,int budget) {
        // A continuous expanding lower cross-section: carve the ground and any wall
        // inside the moving shockwave, not a spherical explosion around the player.
        Vec3 lower=center.add(0,-.85,0);
        BlockPos middle=BlockPos.containing(lower);
        int extent=(int)Math.ceil(radius+1);
        List<BlockPos> candidates=new ArrayList<>();
        for(BlockPos pos:BlockPos.betweenClosed(middle.offset(-extent,-extent,-extent),middle.offset(extent,extent,extent))) {
            Vec3 delta=Vec3.atCenterOf(pos).subtract(lower);
            double along=delta.dot(w.dir);
            if(Math.abs(along)> .75 || delta.subtract(w.dir.scale(along)).lengthSqr()>radius*radius) continue;
            if(Vec3.atCenterOf(pos).distanceToSqr(w.origin)<2.0) continue;
            candidates.add(pos.immutable());
        }
        candidates.sort(Comparator.comparingDouble(p->Vec3.atCenterOf(p).distanceToSqr(lower)));
        int used=0;
        for(BlockPos pos:candidates) {
            if(used>=budget) break;
            if(!w.visited.add(pos) || !w.level.isLoaded(pos) || !w.level.getWorldBorder().isWithinBounds(pos)) continue;
            BlockState state=w.level.getBlockState(pos);
            if(state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty() ||
               state.getDestroySpeed(w.level,pos)<0 || state.getBlock().getExplosionResistance()>BlastConfig.maxResistance() ||
               !w.level.mayInteract(player,pos)) continue;
            used++;
            // Standard player block break path fires NeoForge's protection event.
            if(player.gameMode.destroyBlock(pos)) {
                Vec3 p=Vec3.atCenterOf(pos);
                w.level.sendParticles(ParticleTypes.FLAME,p.x,p.y,p.z,4,.22,.22,.22,.06);
                w.level.sendParticles(ParticleTypes.SMOKE,p.x,p.y,p.z,2,.2,.2,.2,.03);
            }
        }
        return used;
    }
}
