package com.kirill.lurelantern.test;

import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import com.kirill.lurelantern.block.LureLanternBlock;
import com.kirill.lurelantern.registry.ModBlocks;
import com.kirill.lurelantern.registry.ModItems;
import com.kirill.lurelantern.util.CapturedMobData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;

/**
 * Server integration test. It exercises the actual block break / loot pipeline
 * rather than calling SafeLanternDrops directly.
 */
@GameTestHolder("lurelantern")
@PrefixGameTestTemplate(false)
public final class LureLanternPersistenceTests {
    private LureLanternPersistenceTests() {}

    @GameTest(template = "lure_roundtrip", timeoutTicks = 180)
    public static void capturedCowSurvivesBreakAndReplace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockState state = ModBlocks.ADVANCED_LURE_LANTERN.get().defaultBlockState();
        helper.assertTrue(level.setBlock(pos, state, 3), "Failed to place advanced lantern");

        if (!(level.getBlockEntity(pos) instanceof LureLanternBlockEntity lantern)) {
            helper.fail("Placed advanced lantern has no block entity");
            return;
        }
        CompoundTag cow = new CompoundTag();
        cow.putString("id", "minecraft:cow");
        cow.putInt("Age", -100);
        ItemStack filledStack = new ItemStack(ModItems.ADVANCED_LURE_LANTERN.get());
        CapturedMobData.setCapturedMobTag(filledStack, cow);
        // Exercise the real placed-item hook rather than directly calling the Block Entity.
        ((LureLanternBlock) state.getBlock()).setPlacedBy(level, pos, state, null, filledStack);
        helper.assertTrue(lantern.hasCapturedMob(), "Cow NBT not loaded into block entity");
        helper.assertTrue(level.destroyBlock(pos, true), "DestroyBlock returned false");

        helper.runAfterDelay(10, () -> {
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class,
                new AABB(pos).inflate(3.0),
                item -> item.getItem().is(ModItems.ADVANCED_LURE_LANTERN.get()));
            helper.assertTrue(drops.size() == 1,
                "Expected one dropped advanced lantern but found " + drops.size());

            ItemEntity itemEntity = drops.get(0);
            ItemStack droppedStack = itemEntity.getItem().copy();
            CompoundTag saved = CapturedMobData.getCapturedMobTag(droppedStack).orElse(null);
            helper.assertTrue(saved != null, "Dropped lantern lost captured mob NBT");
            if (saved == null) return;
            helper.assertValueEqual(saved.getString("id"), "minecraft:cow", "Mob entity ID");
            helper.assertValueEqual(saved.getInt("Age"), -100, "Mob age");
            itemEntity.discard();

            helper.assertTrue(level.setBlock(pos, state, 3), "Failed to replace lantern");
            if (!(level.getBlockEntity(pos) instanceof LureLanternBlockEntity replaced)) {
                helper.fail("Replaced lantern has no block entity");
                return;
            }
            ((LureLanternBlock) state.getBlock()).setPlacedBy(level, pos, state, null, droppedStack);
            helper.assertTrue(replaced.hasCapturedMob(), "Replacement lantern lost saved cow");
            CompoundTag tile = replaced.getUpdateTag(level.registryAccess());
            helper.assertTrue(tile.contains("CapturedMob", 10), "Block Entity NBT missing cow");
            helper.assertTrue(replaced.releaseCapturedMob(level, pos), "Unable to release cow");
            helper.assertTrue(!replaced.hasCapturedMob(), "Mob tag was not cleared on release");
            List<Cow> cows = level.getEntitiesOfClass(Cow.class, new AABB(pos).inflate(3.0));
            helper.assertTrue(cows.size() == 1, "Expected one released cow but found " + cows.size());
            helper.succeed();
        });
    }
}
