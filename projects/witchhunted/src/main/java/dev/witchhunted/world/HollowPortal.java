package dev.witchhunted.world;

import dev.witchhunted.WitchHunted;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class HollowPortal {
    private static final String TAG = "witchhunted_origin";
    private HollowPortal() {}

    public static void use(ServerPlayer player) {
        if (player.level().dimension().equals(WitchHunted.THE_HOLLOW)) {
            leave(player);
        } else {
            enter(player);
        }
    }

    private static void enter(ServerPlayer player) {
        ServerLevel destination = player.getServer().getLevel(WitchHunted.THE_HOLLOW);
        if (destination == null) {
            player.sendSystemMessage(Component.translatable("message.witchhunted.no_dimension"));
            return;
        }

        CompoundTag origin = new CompoundTag();
        origin.putString("dimension", player.level().dimension().location().toString());
        origin.putDouble("x", player.getX());
        origin.putDouble("y", player.getY());
        origin.putDouble("z", player.getZ());
        origin.putFloat("yaw", player.getYRot());
        origin.putFloat("pitch", player.getXRot());
        player.getPersistentData().put(TAG, origin);

        HollowWorld.ensure(destination);
        player.teleportTo(destination, 2.5D, 2.0D, 2.5D, player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.translatable("message.witchhunted.enter"));
    }

    private static void leave(ServerPlayer player) {
        CompoundTag origin = player.getPersistentData().getCompound(TAG);
        ResourceLocation id = ResourceLocation.tryParse(origin.getString("dimension"));
        ResourceKey<Level> key = id == null
                ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel destination = player.getServer().getLevel(key);
        if (destination == null) destination = player.getServer().overworld();
        if (destination == null) return;
        double x = origin.contains("x") ? origin.getDouble("x") : destination.getSharedSpawnPos().getX() + 0.5;
        double y = origin.contains("y") ? origin.getDouble("y") : destination.getSharedSpawnPos().getY() + 1.0;
        double z = origin.contains("z") ? origin.getDouble("z") : destination.getSharedSpawnPos().getZ() + 0.5;
        player.teleportTo(destination, x, y, z,
            origin.getFloat("yaw"), origin.getFloat("pitch"));
        player.getPersistentData().remove(TAG);
        player.sendSystemMessage(Component.translatable("message.witchhunted.leave"));
    }
}
