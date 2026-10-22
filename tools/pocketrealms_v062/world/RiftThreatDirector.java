package com.kirill.pocketrealms.world;

import com.kirill.pocketrealms.entity.VoidPursuer;
import com.kirill.pocketrealms.entity.WallGrasper;
import com.kirill.pocketrealms.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Server-only ambush controller: no runaway entity counts and no new dimensions. */
public final class RiftThreatDirector {
    private RiftThreatDirector() {}

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.getLevel(ModDimensions.POCKET_DIMENSION);
        if (level == null || level.getGameTime() % 20 != 0) return;
        PocketRunData data = PocketRunData.get(server);
        long now = level.getGameTime();
        for (PocketRunEntry run : data.allRuns()) {
            if (!run.generated || run.retiring || run.completed || run.slot < 0) continue;
            ServerPlayer player = level.players().stream().filter(p -> p.getUUID().equals(run.owner)
                    && p.isAlive() && !p.isSpectator() && !p.isCreative() && run.contains(p.blockPosition()))
                    .findFirst().orElse(null);
            if (player == null || player.distanceToSqr(run.exitPos.getX()+.5, run.exitPos.getY()+1, run.exitPos.getZ()+.5) < 60) continue;
            if (now - run.startedAt > 20 * 10L && now % 80 == (run.id % 4) * 20L) {
                tryHands(level, run, player);
            }
            if (now - run.startedAt < 20 * 35L) continue;
            AABB area = new AABB(run.minX, run.minY, run.minZ, run.maxX+1, run.maxY+1, run.maxZ+1);
            var stalkers = level.getEntitiesOfClass(VoidPursuer.class, area);
            if (!stalkers.isEmpty()) {
                VoidPursuer stalker = stalkers.getFirst();
                // Maze pathfinding can get stuck on a distant route; move only when out of sight.
                if (stalker.isStranded() && now%80==0 && stalker.distanceToSqr(player)>10*10
                        && !player.hasLineOfSight(stalker)) {
                    BlockPos next = findFloor(level, run, player, 6, 13);
                    if (next != null) {
                        level.sendParticles(ParticleTypes.REVERSE_PORTAL, stalker.getX(), stalker.getY()+1, stalker.getZ(), 20, .4,.8,.4,.08);
                        stalker.moveTo(next.getX()+.5,next.getY(),next.getZ()+.5,stalker.getYRot(),0);
                        stalker.afterHiddenReposition();
                        stalker.beginTracking(player.blockPosition());
                    }
                }
                if (now >= run.nextPursuerAt) {
                    run.nextPursuerAt = now + 20*65L;
                    data.setDirty();
                }
            } else if (now >= run.nextPursuerAt) {
                BlockPos target = findFloor(level, run, player, 7, 16);
                if (target != null) {
                    VoidPursuer stalker = ModEntities.VOID_PURSUER.get().create(level);
                    if (stalker != null) {
                        stalker.moveTo(target.getX()+.5, target.getY(), target.getZ()+.5, 0, 0);
                        stalker.finalizeSpawn(level, level.getCurrentDifficultyAt(target), MobSpawnType.EVENT, null);
                        stalker.beginTracking(player.blockPosition());
                        if (level.addFreshEntity(stalker)) {
                            level.playSound(null, target, com.kirill.pocketrealms.registry.ModSounds.PURSUER_IDLE.get(), SoundSource.HOSTILE, 1.1F, 0.95F);
                            player.displayClientMessage(Component.translatable("message.pocketrealms.pursuer"), true);
                        }
                    }
                }
                run.nextPursuerAt = now + 20 * 32L;
                data.setDirty();
            }
        }
    }

    private static void tryHands(ServerLevel level, PocketRunEntry run, ServerPlayer player) {
        AABB area = new AABB(run.minX, run.minY, run.minZ, run.maxX+1, run.maxY+1, run.maxZ+1);
        if (level.getEntitiesOfClass(WallGrasper.class, area).size() >= 5
                || level.getEntitiesOfClass(WallGrasper.class, player.getBoundingBox().inflate(9)).size() >= 2
                || level.random.nextFloat() >= .62F) return;
        for (int i=0;i<80;i++) {
            int x = player.blockPosition().getX()+level.random.nextInt(21)-10;
            int z = player.blockPosition().getZ()+level.random.nextInt(21)-10;
            BlockPos feet = new BlockPos(x, player.blockPosition().getY(), z);
            if (!validFloor(level,run,feet) || feet.distSqr(player.blockPosition()) < 6
                    || feet.distSqr(run.exitPos) < 70 || feet.distSqr(run.spawnPos) < 20) continue;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos wall = feet.relative(side).above();
                if (!level.getBlockState(wall).isSolid()) continue;
                WallGrasper hands = ModEntities.WALL_GRASPER.get().create(level);
                if (hands == null) return;
                // side points into the wall: outward is the direction from the wall to the corridor.
                Direction normal = side.getOpposite();
                hands.finalizeSpawn(level,level.getCurrentDifficultyAt(feet), MobSpawnType.EVENT,null);
                hands.attach(wall, normal);
                hands.anchorInCorridor();
                if (level.addFreshEntity(hands)) {
                    level.playSound(null, wall, SoundEvents.SCULK_BLOCK_BREAK, SoundSource.HOSTILE, .65F, .6F);
                }
                return;
            }
        }
    }

    private static BlockPos findFloor(ServerLevel level, PocketRunEntry run, ServerPlayer player, int min, int max) {
        for (int i=0;i<260;i++) {
            int x = player.blockPosition().getX()+level.random.nextInt(max*2+1)-max;
            int z = player.blockPosition().getZ()+level.random.nextInt(max*2+1)-max;
            BlockPos feet = new BlockPos(x, player.blockPosition().getY(),z);
            int ds = (x-player.blockPosition().getX())*(x-player.blockPosition().getX())
                    +(z-player.blockPosition().getZ())*(z-player.blockPosition().getZ());
            if (ds < min*min || ds > max*max || feet.distSqr(run.exitPos) < 80 || !validFloor(level,run,feet)) continue;
            // Never materialize in the player's direct view, even during pathfinding recovery.
            var hit=level.clip(new net.minecraft.world.level.ClipContext(
                    player.getEyePosition(),net.minecraft.world.phys.Vec3.atCenterOf(feet),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,player));
            if(hit.getType()==net.minecraft.world.phys.HitResult.Type.MISS)continue;
            return feet;
        }
        return null;
    }
    private static boolean validFloor(ServerLevel level, PocketRunEntry run, BlockPos feet) {
        return run.contains(feet) && run.contains(feet.above())
                && level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                && level.getBlockState(feet.below()).isCollisionShapeFullBlock(level, feet.below())
                && !level.getBlockState(feet.below()).is(Blocks.BARRIER);
    }
}
