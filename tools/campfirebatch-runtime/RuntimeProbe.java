package dev.minemods.campfirebatch;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class RuntimeProbe {
    static final class ProbePlayer extends ServerPlayer {
        boolean sneak = true;
        ProbePlayer(MinecraftServer server, ServerLevel level) {
            super(server, level, new GameProfile(UUID.randomUUID(), "CampfireProbe"), ClientInformation.createDefault());
        }
        @Override public boolean isSecondaryUseActive() { return sneak; }
        @Override public void awardStat(Stat<?> stat, int count) { }
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static int occupied(ServerLevel level, BlockPos pos) {
        var fire = (CampfireBlockEntity) level.getBlockEntity(pos);
        return (int)fire.getItems().stream().filter(s -> !s.isEmpty()).count();
    }
    static void fresh(ServerLevel level, BlockPos pos, Block block) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        check(occupied(level,pos)==0, "fresh campfire is not empty");
    }
    static InteractionResult use(ProbePlayer player, ServerLevel level, BlockPos pos, InteractionHand hand) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        /* CALLBACK */
    }
    static void run(MinecraftServer server) {
        try {
            ServerLevel level = server.overworld();
            BlockPos pos = new BlockPos(0, 100, 0);
            level.getChunkAt(pos);
            ProbePlayer player = new ProbePlayer(server, level);
            player.setPos(0.5,101.0,2.5);
            var main = InteractionHand.MAIN_HAND;
            var off = InteractionHand.OFF_HAND;
            for (Block block : new Block[]{Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE}) {
                fresh(level,pos,block);
                ItemStack beef = new ItemStack(Items.BEEF,64);
                Component name = Component.literal("Probe beef");
                beef.set(DataComponents.CUSTOM_NAME,name);
                player.setItemInHand(main,beef);
                player.setItemInHand(off,new ItemStack(Items.PORKCHOP,64));
                player.sneak = false;
                check(use(player,level,pos,main)==InteractionResult.PASS, "normal click intercepted");
                check(occupied(level,pos)==0 && beef.getCount()==64, "normal click mutated state");
                player.sneak = true;
                check(use(player,level,pos,main).consumesAction(), "batch not consumed");
                check(occupied(level,pos)==4 && beef.getCount()==60, "64 -> 60 / four slots failed");
                check(player.getItemInHand(off).getCount()==64, "offhand depleted");
                var fire = (CampfireBlockEntity)level.getBlockEntity(pos);
                for (ItemStack slot : fire.getItems())
                    check(slot.getCount()==1 && slot.is(Items.BEEF) && name.equals(slot.get(DataComponents.CUSTOM_NAME)), "stack components/count lost");
                check(use(player,level,pos,main)==InteractionResult.PASS && beef.getCount()==60, "full campfire consumed input");
                fresh(level,pos,block);
                player.setItemInHand(main,new ItemStack(Items.BEEF,2));
                check(use(player,level,pos,main).consumesAction() && occupied(level,pos)==2 && player.getItemInHand(main).isEmpty(),"two item load failed");
                var first = ((CampfireBlockEntity)level.getBlockEntity(pos)).getItems().get(0);
                player.setItemInHand(off,new ItemStack(Items.PORKCHOP,64));
                check(use(player,level,pos,off).consumesAction() && occupied(level,pos)==4 && player.getItemInHand(off).getCount()==62,"partial/offhand load failed");
                check(((CampfireBlockEntity)level.getBlockEntity(pos)).getItems().get(0)==first, "existing slot overwritten");
                fresh(level,pos,block);
                player.setItemInHand(main,new ItemStack(Items.DIAMOND,64));
                check(use(player,level,pos,main)==InteractionResult.PASS && occupied(level,pos)==0 && player.getItemInHand(main).getCount()==64,"invalid recipe changed state");
            }
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            System.out.println("CAMPFIRE_BATCH_RUNTIME_PASSED");
        } catch(Throwable error) {
            System.err.println("CAMPFIRE_BATCH_RUNTIME_FAILED");
            error.printStackTrace();
            throw new IllegalStateException("Campfire Batch runtime probe failed", error);
        }
    }
}
