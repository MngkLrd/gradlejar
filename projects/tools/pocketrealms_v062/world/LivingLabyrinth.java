package com.kirill.pocketrealms.world;

import com.kirill.pocketrealms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;

/** Opens new shortcuts and reseals only shortcuts created by this controller. */
public final class LivingLabyrinth {
    private LivingLabyrinth() {}

    public static void tick(MinecraftServer server) {
        ServerLevel level=server.getLevel(ModDimensions.POCKET_DIMENSION);
        if (level==null || level.getGameTime()%20!=0 || level.players().isEmpty()) return;
        PocketRunData data=PocketRunData.get(server);
        long now=level.getGameTime();
        for (PocketRunEntry run : data.allRuns()) {
            if (!run.generated || run.completed || run.retiring || now < run.nextShift || (run.collapseStartAt>0?now>=run.collapseStartAt:now-run.startedAt>=20*240)) continue;
            if (level.players().stream().noneMatch(p -> run.contains(p.blockPosition()))) continue;
            List<Long> remain=new ArrayList<>();
            for (long packed : run.phaseOpen) {
                BlockPos pos=BlockPos.of(packed);
                if (occupied(level,pos) || !SafeMazeRouting.canAlter(level,run,pos) || level.players().stream().anyMatch(p -> p.distanceToSqr(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5)<20)) {remain.add(packed);continue;}
                for(int y=1;y<=3;y++) {
                    BlockPos at=pos.above(y);
                    if (level.getBlockState(at).isAir()) level.setBlock(at,ModBlocks.VOID_BRICKS.get().defaultBlockState(),2);
                }
                level.sendParticles(ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+1.4,pos.getZ()+.5,10,.5,.8,.5,.025);
            }
            run.phaseOpen.clear();
            run.phaseOpen.addAll(remain);
            int opened=0;
            List<Long> candidates=new ArrayList<>(run.phaseCandidates);
            java.util.Collections.shuffle(candidates,new java.util.Random(level.random.nextLong()));
            for (long packed:candidates) {
                if(opened>=8)break;
                if(run.phaseOpen.contains(packed))continue;
                BlockPos pos=BlockPos.of(packed);
                if (level.players().stream().noneMatch(p -> p.distanceToSqr(pos.getX(),pos.getY(),pos.getZ())<625))continue;
                if (!level.getBlockState(pos.above()).is(ModBlocks.VOID_BRICKS.get()) &&
                    !level.getBlockState(pos.above()).is(ModBlocks.POCKET_STONE.get()) &&
                    !level.getBlockState(pos.above()).is(ModBlocks.CRACKED_POCKET_STONE.get()) &&
                    !level.getBlockState(pos.above()).is(ModBlocks.CRACKED_VOID_BRICKS.get()) &&
                    !level.getBlockState(pos.above()).is(ModBlocks.RIFT_CARVED_STONE.get()) &&
                    !level.getBlockState(pos.above()).is(ModBlocks.RIBBED_VOID_PILLAR.get())) continue;
                for(int y=1;y<=3;y++) level.setBlock(pos.above(y),Blocks.AIR.defaultBlockState(),2);
                run.phaseOpen.add(packed);
                level.playSound(null,pos,SoundEvents.SCULK_BLOCK_BREAK,SoundSource.BLOCKS,.65F,.7F);
                opened++;
            }
            run.nextShift=now+20*(5+level.random.nextInt(5));
            data.setDirty();
        }
    }

    private static boolean occupied(ServerLevel level,BlockPos floor) {
        // Closing only nonessential passages, but never inside a living entity's hitbox.
        return !level.getEntitiesOfClass(LivingEntity.class,
                new AABB(floor.getX(),floor.getY()+1,floor.getZ(),
                         floor.getX()+1,floor.getY()+4,floor.getZ()+1).inflate(.2)).isEmpty();
    }
}
