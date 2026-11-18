#!/usr/bin/env python3
"""Generate the Bone Meal Everything project from proven Gradle templates in this repo."""
from __future__ import annotations
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODID = "bonemealeverything"
VERSION = "1.0.0"

CORE = r'''package com.kirill.bonemealeverything;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server-authoritative bone meal mechanics. Unrecognized blocks PASS to vanilla;
 * this deliberately does not replace the built-in fertilization of ordinary crops.
 * Chance is rolled only for an attempted action, never on each game tick.
 */
public final class GrowthLogic {
    private GrowthLogic() {}

    private static final Direction[] HORIZONTAL = {
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    public static boolean supports(BlockState state) {
        return state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS)
            || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
            || state.is(Blocks.NETHER_WART) || state.is(Blocks.CHORUS_FLOWER)
            || state.is(Blocks.MELON_STEM) || state.is(Blocks.PUMPKIN_STEM)
            || state.is(Blocks.VINE) || state.is(Blocks.MYCELIUM)
            || state.is(Blocks.PODZOL) || state.is(Blocks.DEAD_BUSH)
            || state.is(Blocks.LILY_PAD) || state.is(BlockTags.FLOWERS)
            || state.is(Blocks.TERRACOTTA)
            || state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE_BRICKS)
            || isCoral(state) || isDeadCoral(state);
    }

    /**
     * true = the click was handled and one bone meal must be consumed.
     * On a failed percentage roll it is still consumed, like vanilla bone meal.
     */
    public static boolean apply(Level level, BlockPos pos, Player player) {
        BlockState st = level.getBlockState(pos);
        if (!supports(st)) return false;
        // Mossification is guaranteed and never accelerates a crop.
        if (st.is(Blocks.COBBLESTONE)) return set(level, pos, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        if (st.is(Blocks.STONE_BRICKS)) return set(level, pos, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());

        if (st.is(Blocks.SUGAR_CANE) || st.is(Blocks.CACTUS)) {
            BlockPos bottom = pos;
            while (level.getBlockState(bottom.below()).is(st.getBlock()))
                bottom = bottom.below();
            BlockPos top = bottom;
            int height = 0;
            while (level.getBlockState(top).is(st.getBlock()) && height < 4) {
                top = top.above(); height++;
            }
            if (height >= 3 || !level.getBlockState(top).isAir()) return false;
            BlockState placed = st.getBlock().defaultBlockState();
            if (!placed.canSurvive(level, top)) return false;
            return roll(level, 85) ? set(level, top, placed) : true;
        }
        if (st.is(Blocks.SAND) || st.is(Blocks.RED_SAND)) {
            BlockPos top = pos.above();
            BlockState cactus = Blocks.CACTUS.defaultBlockState();
            if (!level.getBlockState(top).isAir() || !cactus.canSurvive(level, top)) return false;
            if (roll(level, 85)) return set(level, top, cactus);
            // A failed cactus roll can instead generate a dead bush (old mod behavior).
            BlockState bush = Blocks.DEAD_BUSH.defaultBlockState();
            if (bush.canSurvive(level, top) && roll(level, 70)) set(level, top, bush);
            return true;
        }
        if (st.is(Blocks.NETHER_WART)) {
            int age = st.getValue(NetherWartBlock.AGE);
            if (age >= 3) return false;
            return roll(level, 85) ? set(level, pos, st.setValue(NetherWartBlock.AGE, Math.min(age + 1, 3))) : true;
        }
        if (st.is(Blocks.CHORUS_FLOWER)) {
            int age = st.getValue(ChorusFlowerBlock.AGE);
            BlockPos next = pos.above();
            if (age >= 5 || !level.getBlockState(next).isAir()) return false;
            BlockState flower = Blocks.CHORUS_FLOWER.defaultBlockState()
                .setValue(ChorusFlowerBlock.AGE, Math.min(age + 1, 5));
            if (!roll(level, 80)) return true;
            // The support check must happen AFTER the old flower becomes a plant.
            if (!level.setBlock(pos, Blocks.CHORUS_PLANT.defaultBlockState(), 3)) return false;
            if (!flower.canSurvive(level, next)) {
                level.setBlock(pos, st, 3);
                return false;
            }
            return set(level, next, flower);
        }
        if (st.is(Blocks.MELON_STEM) || st.is(Blocks.PUMPKIN_STEM)) {
            if (st.getValue(StemBlock.AGE) < 7) return false;
            boolean melon = st.is(Blocks.MELON_STEM);
            BlockState crop = (melon ? Blocks.MELON : Blocks.PUMPKIN).defaultBlockState();
            for (Direction dir : shuffled(level)) {
                BlockPos dest = pos.relative(dir);
                BlockState ground = level.getBlockState(dest.below());
                if (!level.getBlockState(dest).isAir()) continue;
                if (!(ground.is(Blocks.DIRT) || ground.is(Blocks.GRASS_BLOCK)
                    || ground.is(Blocks.FARMLAND) || ground.is(Blocks.COARSE_DIRT))) continue;
                if (!roll(level, 80)) return true;
                level.setBlock(dest, crop, 3);
                BlockState attached = (melon ? Blocks.ATTACHED_MELON_STEM : Blocks.ATTACHED_PUMPKIN_STEM)
                    .defaultBlockState().setValue(AttachedStemBlock.FACING, dir);
                level.setBlock(pos, attached, 3);
                return true;
            }
            return false;
        }
        if (st.is(Blocks.VINE)) {
            BlockPos down = pos.below();
            if (level.getBlockState(down).isAir() && st.canSurvive(level, down)) {
                return roll(level, 85) ? set(level, down, st) : true;
            }
            for (Direction dir : shuffled(level)) {
                BlockPos side = pos.relative(dir);
                if (!level.getBlockState(side).isAir()) continue;
                BlockState vine = Blocks.VINE.defaultBlockState().setValue(
                    VineBlock.PROPERTY_BY_DIRECTION.get(dir.getOpposite()), true);
                if (vine.canSurvive(level, side))
                    return roll(level, 85) ? set(level, side, vine) : true;
            }
            return false;
        }
        if (st.is(Blocks.MYCELIUM)) {
            BlockPos top = pos.above();
            BlockState mushroom = (level.getRandom().nextBoolean() ? Blocks.RED_MUSHROOM
                : Blocks.BROWN_MUSHROOM).defaultBlockState();
            boolean canMushroom = level.getBlockState(top).isAir() && mushroom.canSurvive(level, top);
            if (!roll(level, 85)) return true;
            boolean spreadDone = spread(level, pos, Blocks.MYCELIUM.defaultBlockState(), 100);
            boolean mushroomDone = canMushroom && set(level, top, mushroom);
            return spreadDone || mushroomDone;
        }
        if (st.is(Blocks.PODZOL)) {
            return spread(level, pos, Blocks.PODZOL.defaultBlockState(), 85);
        }
        if (st.is(Blocks.DEAD_BUSH)) {
            for (Direction dir : shuffled(level)) {
                BlockPos at = pos.relative(dir);
                BlockState bush = Blocks.DEAD_BUSH.defaultBlockState();
                if (level.getBlockState(at).isAir() && bush.canSurvive(level, at))
                    return roll(level, 70) ? set(level, at, bush) : true;
            }
            return roll(level, 70) ? drop(level, pos, new ItemStack(Blocks.DEAD_BUSH)) : true;
        }
        if (st.is(Blocks.TERRACOTTA)) {
            BlockPos at = pos.above();
            BlockState bush = Blocks.DEAD_BUSH.defaultBlockState();
            if (!level.getBlockState(at).isAir() || !bush.canSurvive(level, at)) return false;
            return roll(level, 70) ? set(level, at, bush) : true;
        }
        if (st.is(Blocks.LILY_PAD)) {
            return roll(level, 75) ? drop(level, pos, new ItemStack(Blocks.LILY_PAD)) : true;
        }
        if (isDeadCoral(st)) {
            if (!nearWater(level, pos)) return false;
            if (!roll(level, 90)) return true;
            return restoreCoral(level, pos, st);
        }
        if (isCoral(st)) {
            BlockPos top = pos.above();
            if (!level.getBlockState(top).is(Blocks.WATER)) return false;
            BlockState coral = (st.is(Blocks.BRAIN_CORAL_BLOCK) ? Blocks.BRAIN_CORAL :
                st.is(Blocks.BUBBLE_CORAL_BLOCK) ? Blocks.BUBBLE_CORAL :
                st.is(Blocks.FIRE_CORAL_BLOCK) ? Blocks.FIRE_CORAL :
                st.is(Blocks.HORN_CORAL_BLOCK) ? Blocks.HORN_CORAL :
                Blocks.TUBE_CORAL).defaultBlockState();
            if (!coral.canSurvive(level, top)) return false;
            return roll(level, 80) ? set(level, top, coral) : true;
        }
        if (st.is(BlockTags.FLOWERS)) {
            return roll(level, 70) ? drop(level, pos, new ItemStack(st.getBlock())) : true;
        }
        return false;
    }

    private static boolean set(Level level, BlockPos pos, BlockState state) {
        return level.setBlock(pos, state, 3);
    }

    private static boolean roll(Level level, int percent) {
        return level.getRandom().nextInt(100) < percent;
    }

    private static boolean drop(Level level, BlockPos pos, ItemStack stack) {
        Block.popResource(level, pos, stack);
        return true;
    }

    private static Direction[] shuffled(Level level) {
        Direction[] arr = HORIZONTAL.clone();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = level.getRandom().nextInt(i + 1);
            Direction tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
        return arr;
    }

    private static boolean spread(Level level, BlockPos origin, BlockState result, int chance) {
        for (Direction dir : shuffled(level)) {
            BlockPos p = origin.relative(dir);
            BlockState s = level.getBlockState(p);
            if (!(s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.COARSE_DIRT))) continue;
            if (!level.getBlockState(p.above()).isAir()) continue;
            return roll(level, chance) ? set(level, p, result) : true;
        }
        return false;
    }

    private static boolean nearWater(Level level, BlockPos pos) {
        for (Direction dir : Direction.values())
            if (level.getBlockState(pos.relative(dir)).is(Blocks.WATER)) return true;
        return false;
    }

    private static boolean isCoral(BlockState st) {
        return st.is(Blocks.TUBE_CORAL_BLOCK) || st.is(Blocks.BRAIN_CORAL_BLOCK)
            || st.is(Blocks.BUBBLE_CORAL_BLOCK) || st.is(Blocks.FIRE_CORAL_BLOCK)
            || st.is(Blocks.HORN_CORAL_BLOCK);
    }

    private static boolean isDeadCoral(BlockState st) {
        return st.is(Blocks.DEAD_TUBE_CORAL_BLOCK) || st.is(Blocks.DEAD_BRAIN_CORAL_BLOCK)
            || st.is(Blocks.DEAD_BUBBLE_CORAL_BLOCK) || st.is(Blocks.DEAD_FIRE_CORAL_BLOCK)
            || st.is(Blocks.DEAD_HORN_CORAL_BLOCK);
    }

    private static boolean restoreCoral(Level level, BlockPos pos, BlockState st) {
        Block target = st.is(Blocks.DEAD_TUBE_CORAL_BLOCK) ? Blocks.TUBE_CORAL_BLOCK :
            st.is(Blocks.DEAD_BRAIN_CORAL_BLOCK) ? Blocks.BRAIN_CORAL_BLOCK :
            st.is(Blocks.DEAD_BUBBLE_CORAL_BLOCK) ? Blocks.BUBBLE_CORAL_BLOCK :
            st.is(Blocks.DEAD_FIRE_CORAL_BLOCK) ? Blocks.FIRE_CORAL_BLOCK :
            Blocks.HORN_CORAL_BLOCK;
        return set(level, pos, target.defaultBlockState());
    }
}
'''

FABRIC = r'''package com.kirill.bonemealeverything;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;

public final class BoneMealEverything implements ModInitializer {
    @Override public void onInitialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator() || !player.getItemInHand(hand).is(Items.BONE_MEAL))
                return InteractionResult.PASS;
            if (!GrowthLogic.supports(level.getBlockState(hit.getBlockPos())))
                return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!GrowthLogic.apply(level, hit.getBlockPos(), player))
                return InteractionResult.PASS;
            if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
            level.levelEvent(1505, hit.getBlockPos(), 0);
            return InteractionResult.SUCCESS;
        });
    }
}
'''

NEOFORGE = r'''package com.kirill.bonemealeverything;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod("bonemealeverything")
public final class BoneMealEverything {
    public BoneMealEverything(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(BoneMealEverything::onUse);
    }

    private static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isSpectator() || !event.getItemStack().is(Items.BONE_MEAL))
            return;
        if (!GrowthLogic.supports(event.getLevel().getBlockState(event.getPos()))) return;
        if (event.getLevel().isClientSide()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (!GrowthLogic.apply(event.getLevel(), event.getPos(), event.getEntity())) return;
        if (!event.getEntity().getAbilities().instabuild) event.getItemStack().shrink(1);
        event.getLevel().levelEvent(1505, event.getPos(), 0);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
'''

LEGACY = r'''package com.kirill.bonemealeverything;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("bonemealeverything")
public final class BoneMealEverything {
    @Mod.EventBusSubscriber(modid = "bonemealeverything", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Interactions {
        @SubscribeEvent public static void onUse(PlayerInteractEvent.RightClickBlock event) {
            if (event.getEntity().isSpectator() || !event.getItemStack().is(Items.BONE_MEAL))
                return;
            if (!GrowthLogic.supports(event.getLevel().getBlockState(event.getPos()))) return;
            if (event.getLevel().isClientSide) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
            if (!GrowthLogic.apply(event.getLevel(), event.getPos(), event.getEntity())) return;
            if (!event.getEntity().getAbilities().instabuild) event.getItemStack().shrink(1);
            event.getLevel().levelEvent(1505, event.getPos(), 0);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
'''

def make(version: str, loader: str) -> Path:
    source = ROOT / 'projects' / f'harvestglow-{loader}-{version}'
    if not (source / 'build.gradle').is_file():
        raise SystemExit(f'Missing Gradle template: {source}')
    target = ROOT / 'build' / f'bonemealeverything-{loader}-{version}'
    if target.exists():
        shutil.rmtree(target)
    shutil.copytree(source, target, ignore=shutil.ignore_patterns('.gradle', 'build', '.idea'))
    for name in ('build.gradle', 'settings.gradle', 'gradle.properties', 'build-profile.json'):
        file = target / name
        if not file.is_file(): continue
        file.write_text(file.read_text().replace('harvestglow', 'bonemealeverything')
                        .replace('1.1.0', VERSION), encoding='utf8')
    java_dir = target / 'src' / 'main' / 'java'
    if java_dir.exists(): shutil.rmtree(java_dir)
    java_dir = java_dir / 'com' / 'kirill' / MODID
    java_dir.mkdir(parents=True)
    (java_dir / 'GrowthLogic.java').write_text(CORE, encoding='utf8')
    main = FABRIC if loader == 'fabric' else NEOFORGE if version != '1.20.1' else LEGACY
    if version == '1.20.1' and loader == 'fabric':
        main = FABRIC.replace('level.isClientSide()', 'level.isClientSide')
    if version == '1.20.1' and loader == 'neoforge':
        main = LEGACY
    (java_dir / 'BoneMealEverything.java').write_text(main, encoding='utf8')
    resources = target / 'src' / 'main' / 'resources'
    if resources.exists(): shutil.rmtree(resources)
    resources.mkdir(parents=True)
    if loader == 'fabric':
        # Depend on Fabric API for UseBlockCallback.
        meta = {
            'schemaVersion': 1, 'id': MODID, 'version': VERSION,
            'name': 'Bone Meal Everything', 'description':
                'Extend bone meal to plants, mossy stones, coral, vines and more.',
            'authors': ['MineMods'], 'environment': '*',
            'entrypoints': {'main':['com.kirill.bonemealeverything.BoneMealEverything']},
            'depends': {
                'fabricloader': '>=0.16.0',
                'minecraft': version, 'java': '>=25' if version.startswith('26.') else
                ('>=17' if version == '1.20.1' else '>=21'),
                'fabric-api': '*'
            }
        }
        (resources / 'fabric.mod.json').write_text(json.dumps(meta, indent=2)+'\n', encoding='utf8')
    else:
        islegacy = version == '1.20.1'
        api = 'forge' if loader == 'forge' else 'neoforge'
        if islegacy:
            text = ('modLoader="javafml"\nloaderVersion="[47,)"\nlicense="MIT"\n\n'
                    '[[mods]]\nmodId="bonemealeverything"\nversion="1.0.0"\n'
                    'displayName="Bone Meal Everything"\nauthors="MineMods"\n'
                    'description="Bone meal can fertilize many more blocks."\n')
        else:
            text = ('modLoader="javafml"\nloaderVersion="[1,)"\nlicense="MIT"\n\n[[mods]]\nmodId="bonemealeverything"\n'
                    'version="1.0.0"\ndisplayName="Bone Meal Everything"\nauthors="MineMods"\n'
                    'description="Bone meal can fertilize many more blocks."\n')
        text += '\n'
        dep = '[[dependencies.bonemealeverything]]\n'
        text += dep + f'modId="{api}"\n'
        if islegacy:
            text += 'mandatory=true\n'
        else:
            text += 'type="required"\n'
        text += ('versionRange="[47,)"' if islegacy else
                 'versionRange="[21.1,)"' if version == '1.21.1' else
                 f'versionRange="[{version.split(".")[0]}.{version.split(".")[1]},)"') + '\n'
        text += 'ordering="NONE"\nside="BOTH"\n\n'
        text += dep + 'modId="minecraft"\n'
        text += 'mandatory=true\n' if islegacy else 'type="required"\n'
        text += f'versionRange="[{version}]"\nordering="NONE"\nside="BOTH"\n'
        meta = resources / 'META-INF' / ('mods.toml' if islegacy else 'neoforge.mods.toml')
        meta.parent.mkdir(parents=True)
        meta.write_text(text, encoding='utf8')
    # Reuse the template's exact pack format instead of guessing format changes.
    original = source / 'src' / 'main' / 'resources' / 'pack.mcmeta'
    if original.exists():
        shutil.copy2(original, resources / 'pack.mcmeta')
    return target

if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise SystemExit('usage: build_bonemeal_everything.py MC_VERSION LOADER')
    print(make(sys.argv[1], sys.argv[2]))
