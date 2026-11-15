package dev.kirill.embershelter.camp;

import dev.kirill.embershelter.config.ServerConfig;
import dev.kirill.embershelter.util.BlockReplaceRules;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public final class CampBuilder {
    private static final List<CampBuildTask> ACTIVE = new ArrayList<>();

    private CampBuilder() {}

    public static boolean start(ServerLevel level, ServerPlayer player, BlockPos center, ItemStack kit) {
        long alreadyBuilding = ACTIVE.stream().filter(t -> t.owner.equals(player.getUUID())).count();
        if (alreadyBuilding >= ServerConfig.MAX_ACTIVE.get()) {
            player.displayClientMessage(Component.translatable("message.embershelter.busy"), true);
            return false;
        }
        List<CampBuildStep> steps = CampLayouts.create(center, player.getDirection().getOpposite());
        CampAreaValidator.Result status=CampAreaValidator.check(level,player,center,steps);
        if(status != CampAreaValidator.Result.OK) {
            player.displayClientMessage(Component.translatable(
                status==CampAreaValidator.Result.WATER ? "message.embershelter.water" : "message.embershelter.blocked"),true);
            return false;
        }
        CampAreaValidator.clearSoftBlocks(level,center);
        if (!player.getAbilities().instabuild) kit.shrink(1);
        CampBuildTask task = new CampBuildTask(player.getUUID(), level.dimension(), center,
                steps, level.getGameTime());
        ACTIVE.add(task);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.getX() + .5,
                center.getY() + .5, center.getZ() + .5, 10, .5, .2, .5, .01);
        player.displayClientMessage(Component.translatable("message.embershelter.started"), true);
        return true;
    }

    public static void tick(MinecraftServer server) {
        Iterator<CampBuildTask> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            CampBuildTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension);
            if (level == null) {
                iterator.remove();
                continue;
            }
            if (level.getGameTime() < task.nextTick) continue;
            if (task.index >= task.steps.size()) {
                complete(level, task);
                iterator.remove();
                continue;
            }
            CampBuildStep step = task.steps.get(task.index);
            if (!level.hasChunkAt(step.pos()) || !BlockReplaceRules.replaceable(level.getBlockState(step.pos()))) {
                interrupted(server, task);
                iterator.remove();
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(task.owner);
            if (owner != null && !level.mayInteract(owner, step.pos())) {
                interrupted(server, task);
                iterator.remove();
                continue;
            }
            boolean placed = level.setBlock(step.pos(), step.state(), 3);
            if (!placed) {
                interrupted(server, task);
                iterator.remove();
                continue;
            }
            level.sendParticles(ParticleTypes.SMOKE, step.pos().getX() + .5, step.pos().getY() + .6,
                    step.pos().getZ() + .5, 4, .25, .2, .25, .02);
            level.playSound(null, step.pos(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, .75f,
                    .9f + level.random.nextFloat() * .25f);
            task.index++;
            task.nextTick = level.getGameTime() + ServerConfig.BUILD_DELAY.get();
        }
    }

    private static void complete(ServerLevel level, CampBuildTask task) {
        if (level.getBlockState(task.center).is(Blocks.CAMPFIRE)) {
            CampSavedData.get(level).add(task.center);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                task.center.getX() + .5, task.center.getY() + 1,
                task.center.getZ() + .5, 18, 1.1, .3, 1.1, .04);
        level.playSound(null, task.center, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.BLOCKS, .55f, 1.1f);
    }

    private static void interrupted(MinecraftServer server, CampBuildTask task) {
        ServerPlayer player=server.getPlayerList().getPlayer(task.owner);
        if(player != null) player.displayClientMessage(Component.translatable("message.embershelter.interrupted"),false);
    }

    public static void clear() { ACTIVE.clear(); }
}
