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
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityType;
import com.kirill.lurelantern.blockentity.LureLanternBlockEntity.LureMode;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;

@GameTestHolder("lurelantern")
@PrefixGameTestTemplate(false)
public final class LureLanternPersistenceTests {
    private LureLanternPersistenceTests() {}

    @GameTest(template = "lure_roundtrip", timeoutTicks = 180)
    public static void capturedCowSurvivesBreakAndReplace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        prepareFloor(level, pos);
        BlockState state = ModBlocks.ADVANCED_LURE_LANTERN.get().defaultBlockState();
        helper.assertTrue(level.setBlock(pos, state, 3), "Failed to place advanced lantern");
        helper.assertTrue(state.getDestroySpeed(level, pos) >= 0.0F,
                "Advanced lantern must be mineable in survival");
        helper.assertTrue(Block.getDrops(state, level, pos, level.getBlockEntity(pos)).stream()
                        .anyMatch(item -> item.is(ModItems.ADVANCED_LURE_LANTERN.get())),
                "Advanced lantern default loot table must drop itself");

        if (!(level.getBlockEntity(pos) instanceof LureLanternBlockEntity lantern)) {
            helper.fail("Placed advanced lantern has no block entity");
            return;
        }
        CompoundTag cow = new CompoundTag();
        cow.putString("id", "minecraft:cow");
        cow.putInt("Age", -100);
        ItemStack filledStack = new ItemStack(ModItems.ADVANCED_LURE_LANTERN.get());
        CapturedMobData.setCapturedMobTag(filledStack, cow);
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
            if (!cows.isEmpty()) {
                Cow released = cows.get(0);
                helper.assertTrue(released.distanceToSqr(pos.getX() + 0.5D,
                        pos.getY(), pos.getZ() + 0.5D) >= 1.0D,
                        "Cow spawned inside lantern instead of beside it");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "lure_roundtrip", timeoutTicks = 180)
    public static void lureOnlyNeverCapturesAndAutoModeCapturesCow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        prepareFloor(level, pos);
        BlockState state = ModBlocks.ADVANCED_LURE_LANTERN.get().defaultBlockState()
                .setValue(LureLanternBlock.ACTIVE, true);
        helper.assertTrue(level.setBlock(pos, state, 3), "Failed to place advanced lantern");
        if (!(level.getBlockEntity(pos) instanceof LureLanternBlockEntity lantern)) {
            helper.fail("Missing advanced Block Entity");
            return;
        }
        Cow cow = EntityType.COW.create(level);
        helper.assertTrue(cow != null, "Cannot create test cow");
        if (cow == null) return;
        cow.moveTo(pos.getX() + 1.1D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0, 0);
        helper.assertTrue(level.addFreshEntity(cow), "Cannot add test cow");
        lantern.setLureMode(LureMode.LURE);
        for (int i = 0; i < 7; i++) LureLanternBlockEntity.serverTick(level, pos, state, lantern);
        helper.assertTrue(!lantern.hasCapturedMob(), "LURE mode captured a cow unexpectedly");
        helper.assertTrue(cow.isAlive(), "LURE mode removed the test cow");
        helper.assertValueEqual(lantern.getUpdateTag(level.registryAccess()).getInt("LureMode"),
                1, "LureMode saved as LURE");

        lantern.setLureMode(LureMode.AUTO_CAPTURE);
        helper.assertValueEqual(lantern.getUpdateTag(level.registryAccess()).getInt("LureMode"),
                2, "LureMode saved as AUTO_CAPTURE");
        for (int i = 0; i < 7; i++) LureLanternBlockEntity.serverTick(level, pos, state, lantern);
        helper.assertTrue(lantern.hasCapturedMob(), "AUTO_CAPTURE failed to capture a nearby cow");
        helper.assertTrue(lantern.getLureMode() == LureMode.OFF,
                "Auto-capture did not switch back to OFF");
        helper.assertTrue(!level.getBlockState(pos).getValue(LureLanternBlock.ACTIVE),
                "Auto-capture did not switch the lantern light OFF");
        helper.succeed();
    }

    @GameTest(template = "lure_roundtrip", timeoutTicks = 120)
    public static void rabbitSpawnsBesideLanternAndNotInside(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        prepareFloor(level, pos);
        BlockState state = ModBlocks.ADVANCED_LURE_LANTERN.get().defaultBlockState();
        helper.assertTrue(level.setBlock(pos, state, 3), "Could not place advanced lantern");
        LureLanternBlockEntity lantern = (LureLanternBlockEntity) level.getBlockEntity(pos);
        CompoundTag rabbit = new CompoundTag();
        rabbit.putString("id", "minecraft:rabbit");
        ItemStack stack = new ItemStack(ModItems.ADVANCED_LURE_LANTERN.get());
        CapturedMobData.setCapturedMobTag(stack, rabbit);
        ((LureLanternBlock) state.getBlock()).setPlacedBy(level, pos, state, null, stack);
        helper.assertTrue(lantern.hasCapturedMob(), "Rabbit not captured");
        helper.assertTrue(lantern.releaseCapturedMob(level, pos), "Rabbit release failed");
        List<Rabbit> rabbits = level.getEntitiesOfClass(Rabbit.class, new AABB(pos).inflate(3));
        helper.assertTrue(rabbits.size() == 1, "Expected one rabbit after release");
        if (!rabbits.isEmpty()) {
            Rabbit released = rabbits.get(0);
            helper.assertTrue(released.distanceToSqr(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5) >= 1.0,
                    "Rabbit spawned inside lantern");
            helper.assertTrue(level.noCollision(released), "Released rabbit has block collision");
        }
        helper.assertTrue(!lantern.hasCapturedMob(), "Rabbit NBT was not cleared");
        helper.succeed();
    }

    /** The tiny game-test template can be placed above an air cavity; build explicit support. */
    private static void prepareFloor(ServerLevel level, BlockPos center) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(center.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }
}
