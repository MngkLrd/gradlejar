package com.kirill.pocketrealms.world;

import com.kirill.pocketrealms.PocketRealms;
import com.kirill.pocketrealms.registry.ModBlocks;
import com.kirill.pocketrealms.util.PocketRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;

public final class PocketTeleporter {
    private PocketTeleporter() {}

    public static void startRun(ServerPlayer player, BlockPos sourceDoorPos) {
        launch(player, sourceDoorPos, true);
    }

    public static boolean startTotemRun(ServerPlayer player) {
        return launch(player, player.blockPosition(), false);
    }

    private static boolean launch(ServerPlayer player, BlockPos sourceDoorPos, boolean consumeDoor) {
        ServerLevel sourceLevel = player.serverLevel();
        if (PocketRules.isPocket(sourceLevel)) {
            player.displayClientMessage(Component.translatable("message.pocketrealms.no_nested_pockets"), true);
            return false;
        }

        MinecraftServer server = sourceLevel.getServer();
        ServerLevel pocketLevel = server.getLevel(ModDimensions.POCKET_DIMENSION);
        if (pocketLevel == null) {
            player.displayClientMessage(Component.translatable("message.pocketrealms.missing_dimension"), true);
            PocketRealms.LOGGER.error("Missing pocket dimension. Check data/pocketrealms/dimension/pocket_dimension.json");
            return false;
        }

        PocketRunData data = PocketRunData.get(server);
        PocketRunEntry entry = data.createRun();
        if (entry == null) {
            player.displayClientMessage(Component.translatable("message.pocketrealms.busy"), true);
            return false; // Keep the source door/totem on failure.
        }
        entry.owner = player.getUUID();
        entry.originDimension = sourceLevel.dimension().location().toString();
        entry.originPos = sourceDoorPos.immutable();
        entry.startedAt = pocketLevel.getGameTime();
        entry.doorKnockAt = entry.startedAt + 20L * (15 + pocketLevel.random.nextInt(21));

        PocketGenerator.generate(pocketLevel, entry);
        // Physical torch placed exactly at the player's landing tile, on a solid floor.
        BlockPos torch=entry.spawnPos.above();
        pocketLevel.setBlock(torch, Blocks.TORCH.defaultBlockState(), 3);
        data.setDirty();

        if(consumeDoor) {
        // Remove both halves once the player actually enters the opened door.
        BlockPos bottom = sourceDoorPos;
        var doorState = sourceLevel.getBlockState(bottom);
        if (doorState.hasProperty(net.minecraft.world.level.block.DoorBlock.HALF)
                && doorState.getValue(net.minecraft.world.level.block.DoorBlock.HALF)
                    == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
            bottom = bottom.below();
        }
        sourceLevel.setBlockAndUpdate(bottom.above(), Blocks.AIR.defaultBlockState());
        sourceLevel.setBlockAndUpdate(bottom, Blocks.AIR.defaultBlockState());
        sourceLevel.playSound(null, sourceDoorPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.75F, 0.65F);
        }

        player.teleportTo(pocketLevel, entry.spawnPos.getX() + 0.5D, entry.spawnPos.getY() + 1.0D, entry.spawnPos.getZ() + 0.5D, player.getYRot(), player.getXRot());
        pocketLevel.playSound(null, entry.spawnPos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.AMBIENT, 0.85F, 0.55F);
        player.displayClientMessage(Component.translatable("message.pocketrealms.entered"), false);
        // No explicit wayfinding instructions: the last chamber is discovered organically.
        return true;
    }

    /** Optional progression: retire the old sector only AFTER the new sector is built.
     * No item or progress loss; the normal Return Portal still exits the original world.
     */
    public static boolean advanceStage(ServerPlayer player, BlockPos gate) {
        if(!PocketRules.isPocket(player.level()))return false;
        MinecraftServer server=player.getServer();
        if(server==null)return false;
        PocketRunData data=PocketRunData.get(server);
        PocketRunEntry previous=data.findRunAt(player.blockPosition()).orElse(null);
        if(previous==null || previous.stage!=1 || !previous.owner.equals(player.getUUID())
            || previous.exitPos==null || gate.distSqr(previous.exitPos)>160)return false;
        ServerLevel realm=player.serverLevel();
        PocketRunEntry next=data.createRun();
        if(next==null){
            player.displayClientMessage(Component.literal("The Depths are occupied. Return later."),true);
            return false;
        }
        next.stage=previous.stage+1;
        next.owner=player.getUUID();
        next.originDimension=previous.originDimension;
        next.originPos=previous.originPos;
        next.startedAt=realm.getGameTime();
        next.doorKnockAt=next.startedAt+20*18L;
        try{
            PocketGenerator.generate(realm,next);
            realm.setBlock(next.spawnPos.above(),Blocks.TORCH.defaultBlockState(),3);
        }catch(Exception error){
            next.retiring=true;next.completed=true;
            data.setDirty();
            PocketRealms.LOGGER.error("Failed to generate Depths Stage II",error);
            return false;
        }
        // Old sector cleans up separately; no overlap with the new 256-block sector.
        previous.completed=true;
        previous.retiring=true;
        previous.cleanupCursor=0;
        data.setDirty();
        player.teleportTo(realm,next.spawnPos.getX()+.5D,next.spawnPos.getY()+1,
                next.spawnPos.getZ()+.5D,player.getYRot(),player.getXRot());
        realm.playSound(null,next.spawnPos,SoundEvents.WARDEN_HEARTBEAT,SoundSource.AMBIENT,1,.55F);
        player.displayClientMessage(Component.literal("STAGE II - THE HOLLOW DEPTHS"),false);
        return true;
    }

    public static void escape(ServerPlayer player) {
        if (!PocketRules.isPocket(player.level())) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        PocketRunData data = PocketRunData.get(server);
        data.findRunAt(player.blockPosition()).ifPresentOrElse(entry -> {
            ServerLevel target = server.getLevel(entry.originKey());
            if (target == null) {
                target = server.overworld();
            }
            BlockPos safe = findSafeReturnPos(target, entry.originPos);
            entry.completed = true;
            entry.retiring = true;
            entry.cleanupCursor = 0;
            data.setDirty();
            player.teleportTo(target, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D, player.getYRot(), player.getXRot());
            target.playSound(null, safe, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.8F);
            player.displayClientMessage(Component.translatable("message.pocketrealms.escaped"), true);
        }, () -> {
            ServerLevel overworld = server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
        });
    }

    private static BlockPos findSafeReturnPos(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        for (int dy = 0; dy < 8; dy++) {
            cursor.set(origin.getX(), origin.getY() + dy, origin.getZ());
            if (level.getBlockState(cursor).isAir() && level.getBlockState(cursor.above()).isAir()) {
                return cursor.immutable();
            }
        }
        return origin.above();
    }
}
