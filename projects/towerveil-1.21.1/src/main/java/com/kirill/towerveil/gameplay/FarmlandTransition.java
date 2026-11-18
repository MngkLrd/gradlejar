package com.kirill.towerveil.gameplay;

import com.kirill.towerveil.TowerVeil;
import com.kirill.towerveil.world.TowerBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FarmlandTransition {
    private static final Map<UUID, Integer> COVERED_TICKS = new HashMap<>();
    private static final String RETURN_TAG = "towerveil_return";
    private static final int REQUIRED_TICKS = 8;
    private static final double TAN_HALF_VERTICAL_FOV = Math.tan(Math.toRadians(35.0));
    private static final double ASSUMED_ASPECT = 16.0 / 9.0;

    private FarmlandTransition() { }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        if (player.level().dimension().equals(TowerVeil.FIELD_DIMENSION) ||
                !player.onGround() || player.getXRot() < 83.0F) {
            COVERED_TICKS.remove(id);
            return;
        }

        int groundY = Mth.floor(player.getY() - 0.05);
        boolean active = isCenteredInFarmlandSquare(player, groundY) && isViewCovered(player, groundY);
        if (!active) {
            COVERED_TICKS.remove(id);
            return;
        }

        int ticks = COVERED_TICKS.merge(id, 1, Integer::sum);
        if (ticks >= REQUIRED_TICKS) {
            COVERED_TICKS.remove(id);
            transfer(player);
        }
    }

    private static boolean isCenteredInFarmlandSquare(ServerPlayer player, int y) {
        int bx = Mth.floor(player.getX());
        int bz = Mth.floor(player.getZ());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x0 = bx - 2; x0 <= bx - 1; x0++) {
            if (Math.abs(player.getX() - (x0 + 2.0)) > 0.72) continue;
            for (int z0 = bz - 2; z0 <= bz - 1; z0++) {
                if (Math.abs(player.getZ() - (z0 + 2.0)) > 0.72) continue;
                boolean square = true;
                for (int x = x0; x < x0 + 4 && square; x++) {
                    for (int z = z0; z < z0 + 4; z++) {
                        if (!player.level().getBlockState(cursor.set(x, y, z)).is(Blocks.FARMLAND)) {
                            square = false;
                            break;
                        }
                    }
                }
                if (square) return true;
            }
        }
        return false;
    }

    private static boolean isViewCovered(ServerPlayer player, int groundY) {
        Vec3 forward = player.getLookAngle().normalize();
        double yaw = Math.toRadians(player.getYRot());
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 up = right.cross(forward).normalize();
        Vec3 eye = player.getEyePosition();
        for (int vy = -1; vy <= 1; vy++) {
            for (int vx = -1; vx <= 1; vx++) {
                Vec3 direction = forward
                        .add(right.scale(vx * TAN_HALF_VERTICAL_FOV * ASSUMED_ASPECT))
                        .add(up.scale(vy * TAN_HALF_VERTICAL_FOV))
                        .normalize();
                BlockHitResult hit = player.level().clip(new ClipContext(
                        eye, eye.add(direction.scale(10.0)),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().getY() != groundY ||
                        !player.level().getBlockState(hit.getBlockPos()).is(Blocks.FARMLAND)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void transfer(ServerPlayer player) {
        ServerLevel destination = player.getServer().getLevel(TowerVeil.FIELD_DIMENSION);
        if (destination == null) return;
        TowerBuilder.ensureBuilt(destination);
        CompoundTag returnInfo = new CompoundTag();
        returnInfo.putString("dimension", player.level().dimension().location().toString());
        returnInfo.putDouble("x", player.getX());
        returnInfo.putDouble("y", player.getY());
        returnInfo.putDouble("z", player.getZ());
        returnInfo.putFloat("yaw", player.getYRot());
        returnInfo.putFloat("pitch", player.getXRot());
        player.getPersistentData().put(RETURN_TAG, returnInfo);
        player.teleportTo(destination, 0.5, 1.01, 0.5, Set.of(), player.getYRot(), player.getXRot());
    }

    public static boolean returnPlayer(ServerPlayer player) {
        if (!player.level().dimension().equals(TowerVeil.FIELD_DIMENSION) ||
                !player.getPersistentData().contains(RETURN_TAG)) return false;

        CompoundTag tag = player.getPersistentData().getCompound(RETURN_TAG);
        net.minecraft.resources.ResourceLocation location =
                net.minecraft.resources.ResourceLocation.tryParse(tag.getString("dimension"));
        if (location == null) return false;

        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location);
        ServerLevel destination = player.getServer().getLevel(key);
        if (destination == null) return false;

        boolean ok = player.teleportTo(destination,
                tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"),
                Set.of(), tag.getFloat("yaw"), 0.0F);
        if (ok) player.getPersistentData().remove(RETURN_TAG);
        return ok;
    }
}
