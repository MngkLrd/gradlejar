package com.kirill.pocketrealms.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** Players can WATCH their old tracks fall away before the terminal collapse. */
public final class PressureErosion {
    private PressureErosion() {}

    public static void tick(MinecraftServer server) {
        ServerLevel level=server.getLevel(ModDimensions.POCKET_DIMENSION);
        if(level==null || level.getGameTime()%20!=0)return;
        PocketRunData data=PocketRunData.get(server);
        long now=level.getGameTime();
        for(PocketRunEntry run:data.allRuns()) {
            if(!run.generated||run.retiring||run.completed||run.spawnPos==null)continue;
            ServerPlayer player=level.players().stream()
                    .filter(p->p.getUUID().equals(run.owner) && p.isAlive()
                            && run.contains(p.blockPosition())).findFirst().orElse(null);
            if(player==null)continue;
            BlockPos walked=player.blockPosition().below();
            List<Long> track=run.footsteps;
            if(track.isEmpty() || BlockPos.of(track.getLast()).distSqr(walked)>=5){
                track.add(walked.asLong());
                if(track.size()>220)track.removeFirst();
                data.setDirty();
            }
            // Three-quarter minute warmup: the environment starts failing BEHIND the player.
            if(now-run.startedAt<20*42L || now<run.nextErosionAt || track.size()<8)continue;
            run.nextErosionAt=now+20*(2+level.random.nextInt(2));
            int tries=Math.min(track.size()-4,18);
            for(int i=0;i<tries;i++){
                BlockPos floor=BlockPos.of(track.removeFirst());
                if(!level.getBlockState(floor).isFaceSturdy(level,floor,net.minecraft.core.Direction.UP))continue;
                if(floor.distSqr(player.blockPosition())<121 || floor.distSqr(run.exitPos)<196
                        || floor.distSqr(run.spawnPos)<36)continue;
                // Do not remove any floor that would strand a player or seal the goal.
                if(!SafeMazeRouting.canAlter(level,run,floor))continue;
                level.setBlock(floor,Blocks.AIR.defaultBlockState(),2);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,floor.getX()+.5,floor.getY()+.1,
                        floor.getZ()+.5,18,.5,.04,.5,.035);
                level.playSound(null,floor,SoundEvents.SCULK_BLOCK_BREAK,SoundSource.BLOCKS,.7F,.38F);
                break;
            }
            data.setDirty();
        }
    }
}
