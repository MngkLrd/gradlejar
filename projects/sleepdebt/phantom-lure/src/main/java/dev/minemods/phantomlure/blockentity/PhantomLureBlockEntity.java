package dev.minemods.phantomlure.blockentity;

import dev.minemods.phantomlure.block.PhantomLureBlock;
import dev.minemods.phantomlure.gameplay.PhantomCleanup;
import dev.minemods.phantomlure.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PhantomLureBlockEntity extends BlockEntity {
    private static final int SUMMON_DELAY = 240; // 12 seconds, allowing the diamond to be inserted.
    private int remainingTicks = SUMMON_DELAY;

    public PhantomLureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LURE.get(), pos, state);
    }

    public static boolean isLureNight(Level level) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        return level.dimension() == Level.OVERWORLD && time >= 13000 && time <= 22000;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhantomLureBlockEntity entity) {
        if (!(level instanceof ServerLevel server)) return;
        if (hasNearbySleeper(server, pos)) {
            server.removeBlock(pos, false); // Sleeping cancels even an unarmed lure. No item drops.
            return;
        }

        if (!isLureNight(level)) {
            // Newly placed daytime lures wait in the world for the next night.
            // A countdown started at night must not roll over to a later night.
            if (entity.remainingTicks < SUMMON_DELAY) server.removeBlock(pos, false);
            return;
        }

        if (--entity.remainingTicks > 0) {
            if (entity.remainingTicks % 20 == 0) {
                entity.setChanged();
                server.sendParticles(ParticleTypes.PORTAL, pos.getX() + .5, pos.getY() + .8,
                    pos.getZ() + .5, 3, .28, .18, .28, .02);
            }
            return;
        }

        boolean charged = state.getValue(PhantomLureBlock.CHARGED);
        int count = charged ? 3 + server.random.nextInt(3) : 1;
        Player nearest = server.getNearestPlayer(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 64, false);
        int spawned = 0;

        // Summon above the exposed ground rather than inside caves or trees.
        for (int i = 0; i < count; i++) {
            int dx = server.random.nextInt(19) - 9;
            int dz = server.random.nextInt(19) - 9;
            double x = pos.getX() + .5 + dx;
            double z = pos.getZ() + .5 + dz;
            int surface = server.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(x), (int) Math.floor(z));
            double y = Math.min(server.getMaxBuildHeight() - 2, Math.max(pos.getY() + 10, surface + 12));
            Phantom phantom = new Phantom(EntityType.PHANTOM, server);
            phantom.setPos(x, y, z);
            if (nearest != null) phantom.setTarget(nearest);
            phantom.addTag(PhantomCleanup.LURE_TAG);
            if (server.addFreshEntity(phantom)) spawned++;
        }

        server.playSound(null, pos, SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE, 1.3F, charged ? .65F : .9F);
        server.sendParticles(ParticleTypes.SOUL, pos.getX() + .5, pos.getY() + .9,
            pos.getZ() + .5, 24, .8, .6, .8, .08);
        server.removeBlock(pos, false); // One-shot trap, no free reuse.
    }

    private static boolean hasNearbySleeper(ServerLevel level, BlockPos pos) {
        for (ServerPlayer player : level.players()) {
            if (player.isSleeping() && player.blockPosition().distSqr(pos) <= 96L * 96L) return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RemainingTicks", remainingTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("RemainingTicks")) remainingTicks = Math.max(1, tag.getInt("RemainingTicks"));
    }
}
