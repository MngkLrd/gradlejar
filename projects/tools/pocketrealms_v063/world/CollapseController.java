package com.kirill.pocketrealms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;

/** One permanent dimension. At most 16 reusable sectors, with incremental cleanup. */
public final class CollapseController {
    private CollapseController() {}
    private static final int GRACE_TICKS = 20 * 180;
    private static final int CLEANUP_BUDGET = 950;

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.getLevel(ModDimensions.POCKET_DIMENSION);
        if (level == null || level.getGameTime() % 10 != 0) return;
        PocketRunData data = PocketRunData.get(server);
        long now = level.getGameTime();
        for (PocketRunEntry run : new ArrayList<>(data.allRuns())) {
            if (run.retiring) {
                clean(level, run, data);
                continue;
            }
            if (!run.generated) {
                run.retiring = true;
                data.setDirty();
                continue;
            }
            boolean ownerInside = level.players().stream().anyMatch(player ->
                    player.getUUID().equals(run.owner) && run.contains(player.blockPosition()) && player.isAlive());
            if (!ownerInside) {
                if (run.abandonedAt < 0) run.abandonedAt = now;
                if (now - run.abandonedAt >= 20 * 12L) {
                    run.retiring = true;
                    run.completed = true;
                }
                data.setDirty();
                continue;
            }
            if (run.abandonedAt >= 0) {
                run.abandonedAt = -1;
                data.setDirty();
            }
            long deadline=run.collapseStartAt>0?run.collapseStartAt:run.startedAt+GRACE_TICKS;
            long remaining=deadline-now;
            if (remaining>0) {
                // Three terse atmospheric warnings, not a quest marker or route spoiler.
                if (remaining==20L*90 || remaining==20L*45 || remaining==20L*15) {
                    level.playSound(null,run.spawnPos,SoundEvents.WARDEN_HEARTBEAT,SoundSource.AMBIENT,.8F,.6F);
                    var p=server.getPlayerList().getPlayer(run.owner);
                    if(p!=null && p.serverLevel()==level)
                        p.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                             "message.pocketrealms.collapse_warning"),true);
                }
                continue;
            }
            int size=now>deadline+20L*50?320:110;
            advance(level, run, size);
            if (run.collapseCursor >= run.collapseBlocks.size() && !ownerInside) {
                // Never turn a failed run into a free escape: let the player fall and die.
                run.retiring = true;
                run.completed = true;
            }
            data.setDirty();
        }
    }

    private static void advance(ServerLevel level, PocketRunEntry run, int budget) {
        for (int i=0; i<budget && run.collapseCursor < run.collapseBlocks.size(); i++) {
            BlockPos pos = BlockPos.of(run.collapseBlocks.get(run.collapseCursor++));
            // The entire sanctuary and its access ring survive even after the deadline.
            if (run.exitPos != null
                    && Math.abs(pos.getX()-run.exitPos.getX())<=10
                    && Math.abs(pos.getZ()-run.exitPos.getZ())<=10) continue;
            if (level.getBlockState(pos).isAir()) continue;
            // A player must retain at least one physical route to the sanctuary.
            if (run.exitPos != null && pos.getY()==run.spawnPos.getY()
                    && !SafeMazeRouting.canAlter(level,run,pos)) continue;
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container c)c.clearContent();
            // During the wave, blocks including the final chest disappear into the void.
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            if (level.random.nextFloat() < 0.08F) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5,
                        4, .4,.3,.4,.015);
            }
            if (level.random.nextFloat() < .012F) {
                level.playSound(null,pos,SoundEvents.SCULK_BLOCK_BREAK,SoundSource.BLOCKS,.6F,.5F);
            }
        }
        if (run.collapseCursor % 900 < budget) {
            for (Player player : level.players()) {
                if (run.contains(player.blockPosition())) {
                    level.playSound(null,player.blockPosition(),SoundEvents.WARDEN_HEARTBEAT,SoundSource.AMBIENT,.5F,.55F);
                }
            }
        }
    }

    private static void clean(ServerLevel level, PocketRunEntry run, PocketRunData data) {
        // Never clean while a real player remains inside the sector.
        if (level.players().stream().anyMatch(p -> run.contains(p.blockPosition()))) return;
        if (run.cleanupCursor == 0) {
            var bounds = new AABB(run.minX-2,run.minY-1,run.minZ-2,
                    run.maxX+3,run.maxY+2,run.maxZ+3);
            for (Entity e : level.getEntities((Entity)null,bounds,entity -> !(entity instanceof Player))) e.discard();
        }
        int width = run.maxX - run.minX + 3;
        int depth = run.maxZ - run.minZ + 3;
        int height = run.maxY - run.minY + 3;
        int total = width * depth * height;
        int end = Math.min(total,run.cleanupCursor+CLEANUP_BUDGET);
        for (int i=run.cleanupCursor;i<end;i++) {
            int x=run.minX-1+(i%width);
            int z=run.minZ-1+((i/width)%depth);
            int y=run.minY-1+(i/(width*depth));
            BlockPos pos=new BlockPos(x,y,z);
            if (!level.getBlockState(pos).isAir()) level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
        run.cleanupCursor=end;
        if (end >= total) data.removeRun(run.id);
        else data.setDirty();
    }
}
