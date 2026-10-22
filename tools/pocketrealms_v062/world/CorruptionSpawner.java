package com.kirill.pocketrealms.world;

import com.kirill.pocketrealms.registry.ModBlocks;
import com.kirill.pocketrealms.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;

/** Infinite lifetime spawning while a pocket is active. Concurrent density is bounded for TPS stability. */
public final class CorruptionSpawner {
    private CorruptionSpawner() {}

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.getLevel(ModDimensions.POCKET_DIMENSION);
        if (level == null || level.players().isEmpty() || level.getGameTime() % 60 != 0) return;

        for (PocketRunEntry run : PocketRunData.get(server).allRuns()) {
            if (!run.generated || run.completed) continue;
            for (long packed : run.spawnerPositions) {
                if ((level.getGameTime()/60L + (packed & 3L))%3L != 0L) continue;
                BlockPos nest = BlockPos.of(packed);
                if (!level.getBlockState(nest).is(ModBlocks.CORRUPTION_NEST.get())) continue;
                if (level.players().stream().noneMatch(player -> run.contains(player.blockPosition())
                        && player.distanceToSqr(nest.getX() + .5, nest.getY() - 1, nest.getZ() + .5) < 256)) continue;
                // No lifetime spawn counter. Only cap simultaneous hostile entities around each node.
                if (level.getEntitiesOfClass(Monster.class, new AABB(nest).inflate(9.0D)).size() >= 9) continue;
                for (int attempt=0;attempt<25;attempt++) {
                    int x=nest.getX()+level.random.nextInt(7)-3;
                    int z=nest.getZ()+level.random.nextInt(7)-3;
                    BlockPos feet = new BlockPos(x, nest.getY()-4,z);
                    if (!run.contains(feet) || !level.getBlockState(feet).isAir()
                            || !level.getBlockState(feet.above()).isAir()
                            || !level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),Direction.UP)) continue;
                    EntityType<? extends Mob> type = level.random.nextFloat() < 0.52F
                            ? ModEntities.CORRUPTED_ZOMBIE.get() : ModEntities.CORRUPTED_SKELETON.get();
                    Mob mob=type.create(level);
                    if (mob==null) return;
                    mob.moveTo(feet.getX()+0.5D,feet.getY(),feet.getZ()+0.5D,level.random.nextFloat()*360.0F,0.0F);
                    mob.finalizeSpawn(level,level.getCurrentDifficultyAt(feet),MobSpawnType.SPAWNER,null);
                    if (!level.noCollision(mob)) continue;
                    if (level.addFreshEntity(mob)) {
                        level.sendParticles(ParticleTypes.SMOKE,feet.getX()+.5D,feet.getY()+1.0D,feet.getZ()+.5D,
                                12,.25D,.6D,.25D,.035D);
                    }
                    break;
                }
            }
        }
    }
}
