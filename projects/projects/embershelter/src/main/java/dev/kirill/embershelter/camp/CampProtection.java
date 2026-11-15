package dev.kirill.embershelter.camp;

import dev.kirill.embershelter.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;

public final class CampProtection {
    private CampProtection() {}

    public static boolean protectedFrom(Mob mob, ServerPlayer player, ServerLevel level) {
        if (!ServerConfig.PROTECTION.get() || level.isDay()) return false;
        if (!ServerConfig.OTHER_DIMENSIONS.get() && level.dimension() != Level.OVERWORLD) return false;
        if (!(mob instanceof Zombie || mob instanceof Skeleton || mob instanceof Spider || mob instanceof Creeper)) return false;
        if (RetaliationTracker.isProvoked(mob,player,level)) return false;
        int radius = ServerConfig.PROTECTION_RADIUS.get();
        for (BlockPos pos : CampSavedData.get(level).positions()) {
            if (!level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (!state.is(Blocks.CAMPFIRE)) {
                CampSavedData.get(level).remove(pos);
                continue;
            }
            if (state.getValue(CampfireBlock.LIT) &&
                    player.blockPosition().distSqr(pos) <= radius * radius &&
                    mob.blockPosition().distSqr(pos) <= (radius + 3) * (radius + 3)) {
                return true;
            }
        }
        return false;
    }
}
