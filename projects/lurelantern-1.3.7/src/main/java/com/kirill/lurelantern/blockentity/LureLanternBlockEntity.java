package com.kirill.lurelantern.blockentity;

import com.kirill.lurelantern.block.LureLanternBlock;
import com.kirill.lurelantern.registry.ModBlockEntities;
import com.kirill.lurelantern.util.CapturedMobData;
import com.kirill.lurelantern.util.LureLanternLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Faithful 1.3.2 entity NBT and capture APIs, extended with persisted advanced-only lure modes.
 * LureMode lives in BlockEntity NBT rather than blockstate to preserve all existing worlds.
 */
public class LureLanternBlockEntity extends BlockEntity {
    private static final int CHECK_INTERVAL_TICKS = 5;
    private static final String STORED_MOB_KEY = "CapturedMob";
    private static final String MODE_KEY = "LureMode";

    public enum LureMode {
        OFF(0), LURE(1), AUTO_CAPTURE(2);
        private final int id;
        LureMode(int id) { this.id = id; }
        public int id() { return id; }
        public LureMode next() {
            return switch (this) {
                case OFF -> LURE;
                case LURE -> AUTO_CAPTURE;
                case AUTO_CAPTURE -> OFF;
            };
        }
        public static LureMode fromId(int id) {
            return switch (id) {
                case 1 -> LURE;
                case 2 -> AUTO_CAPTURE;
                default -> OFF;
            };
        }
    }

    private int ticksUntilNextCheck = CHECK_INTERVAL_TICKS;
    @Nullable private CompoundTag capturedMobTag;
    private LureMode lureMode = LureMode.OFF;
    private int previewRevision;

    public LureLanternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LURE_LANTERN.get(), pos, state);
    }

    public LureMode getLureMode() { return lureMode; }

    public void setLureMode(LureMode mode) {
        if (lureMode != mode) {
            lureMode = mode;
            ticksUntilNextCheck = CHECK_INTERVAL_TICKS;
            syncBlockEntity();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  LureLanternBlockEntity lantern) {
        if (!state.getValue(LureLanternBlock.ACTIVE)) {
            lantern.ticksUntilNextCheck = CHECK_INTERVAL_TICKS;
            return;
        }
        if (--lantern.ticksUntilNextCheck > 0) return;
        lantern.ticksUntilNextCheck = CHECK_INTERVAL_TICKS;
        Vec3 center = Vec3.atCenterOf(pos);

        if (LureLanternBlock.isAdvancedLantern(state)) {
            if (lantern.lureMode == LureMode.OFF) return;
            if (lantern.lureMode == LureMode.AUTO_CAPTURE && level instanceof ServerLevel server) {
                if (lantern.hasCapturedMob()) return;
                ItemStack temp = new ItemStack(state.getBlock());
                if (LureLanternLogic.captureNearestMob(server, center, temp)) {
                    CapturedMobData.getCapturedMobTag(temp)
                            .ifPresent(tag -> lantern.capturedMobTag = tag.copy());
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                            SoundSource.BLOCKS, 0.85F, 1.55F);
                    level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT,
                            SoundSource.BLOCKS, 0.22F, 1.75F);
                    lantern.previewRevision++;
                    lantern.setLureMode(LureMode.OFF);
                    level.setBlock(pos, state.setValue(LureLanternBlock.ACTIVE, false), 3);
                    return;
                }
            }
        }
        // Original 1.3.2 lure logic and cadence are unchanged.
        LureLanternLogic.lureMobs(level, center);
    }

    /** Releases at a collision-free position beside the lantern, not inside it. */
    public boolean releaseCapturedMob(ServerLevel level, BlockPos pos) {
        if (capturedMobTag == null) return false;
        CompoundTag data = capturedMobTag.copy();
        // Re-use original entity deserializer and attributes (names, age, variants, etc.).
        Entity mob = LureLanternLogic.loadCapturedEntity(level, data,
                new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
        if (mob == null) return false;
        // Try each adjacent block and radius-2 alternatives for large mobs.
        // A failed release NEVER consumes the stored NBT.
        int[][] spots = {
                {0,-1},{1,0},{0,1},{-1,0},
                {1,-1},{1,1},{-1,1},{-1,-1},
                {0,-2},{2,0},{0,2},{-2,0}
        };
        for (int[] offset : spots) {
            for (int dy = 0; dy <= 3; dy++) {
                BlockPos target = pos.offset(offset[0], -dy, offset[1]);
                // Never release into an unsupported void or an unloaded chunk.
                if (!level.hasChunkAt(target)
                        || !level.getBlockState(target.below())
                                .isFaceSturdy(level, target.below(), Direction.UP)) {
                    continue;
                }
                mob.moveTo(target.getX() + 0.5D, target.getY() + 0.01D, target.getZ() + 0.5D,
                        mob.getYRot(), mob.getXRot());
                if (!level.noCollision(mob)) continue;
                if (level.addFreshEntity(mob)) {
                    capturedMobTag = null;
                    previewRevision++;
                    syncBlockEntity();
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public boolean hasCapturedMob() { return capturedMobTag != null; }

    public Component getCapturedMobName() {
        return capturedMobTag == null
                ? Component.translatable("tooltip.lurelantern.no_mob")
                : CapturedMobData.getCapturedMobName(capturedMobTag);
    }

    public void saveCapturedMobToStack(ItemStack stack) {
        if (capturedMobTag != null) CapturedMobData.setCapturedMobTag(stack, capturedMobTag);
    }

    public void loadCapturedMobFromStack(ItemStack stack) {
        capturedMobTag = CapturedMobData.getCapturedMobTag(stack).orElse(null);
        previewRevision++;
        syncBlockEntity();
    }

    /** The renderer copies NBT only when this number changes, not every frame. */
    public int getPreviewRevision() { return previewRevision; }

    @Nullable public CompoundTag getCapturedMobTagForPreview() {
        return capturedMobTag == null ? null : capturedMobTag.copy();
    }

    @Override
    protected void saveAdditional(CompoundTag data, HolderLookup.Provider provider) {
        super.saveAdditional(data, provider);
        if (capturedMobTag != null) data.put(STORED_MOB_KEY, capturedMobTag.copy());
        data.putInt(MODE_KEY, lureMode.id());
    }

    @Override
    protected void loadAdditional(CompoundTag data, HolderLookup.Provider provider) {
        super.loadAdditional(data, provider);
        capturedMobTag = data.contains(STORED_MOB_KEY, 10)
                ? data.getCompound(STORED_MOB_KEY).copy() : null;
        // Old 1.3.2-1.3.5 advanced lanterns had only ACTIVE. When active,
        // preserve the historical auto-capture behavior after upgrading.
        lureMode = data.contains(MODE_KEY, 3)
                ? LureMode.fromId(data.getInt(MODE_KEY))
                : (LureLanternBlock.isAdvancedLantern(getBlockState())
                && getBlockState().getValue(LureLanternBlock.ACTIVE)
                ? LureMode.AUTO_CAPTURE : LureMode.OFF);
        previewRevision++;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag data = super.getUpdateTag(provider);
        saveAdditional(data, provider);
        return data;
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncBlockEntity() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
}
