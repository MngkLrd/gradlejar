package com.kirill.pocketrealms.world;

import com.kirill.pocketrealms.compat.CompatLoot;
import com.kirill.pocketrealms.registry.ModBlocks;
import com.kirill.pocketrealms.registry.ModItems;
import com.kirill.pocketrealms.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PocketGenerator {
    private PocketGenerator() {}

    private static final int GRID = 39;
    private static final int HEIGHT = 5;
    private static final int BASE_Y = 80;
    private static final int SPACING = 256;
    private static final int SECTOR_X = 8192;
    private static final int SECTOR_Z = 16384;

    public static void generate(ServerLevel level, PocketRunEntry entry) {
        RandomSource random = RandomSource.create(level.getSeed() ^ (entry.id * 341873128712L));
        int type = entry.id % 3;
        int centerX = SECTOR_X + entry.slot * SPACING;
        int centerZ = SECTOR_Z;
        int minX = centerX - GRID / 2 - 3;
        int minZ = centerZ - GRID / 2 - 3;
        int maxX = centerX + GRID / 2 + 3;
        int maxZ = centerZ + GRID / 2 + 3;

        entry.minX = minX;
        entry.minY = BASE_Y - 2;
        entry.minZ = minZ;
        entry.maxX = maxX;
        entry.maxY = BASE_Y + HEIGHT + 2;
        entry.maxZ = maxZ;

        boolean[][] open = carveMaze(random, type);
        // Build actual enclosed architectural rooms around existing corridors, not 3x3 corridor blobs.
        PocketRooms rooms = PocketRooms.plan(open, random, type);
        Map<Long, Integer> distances = computeDistances(open, 1, 1);
        BlockPos exitCell = findFarthestOutsideRooms(open, distances, rooms, centerX, centerZ);
        // Reserve the final arena in the logical mask before floor and AI generation.
        reserveArena(open, centerX, centerZ, exitCell);
        distances = computeDistances(open, 1, 1);
        BlockPos startCell = gridToWorld(centerX, centerZ, 1, 1);
        entry.spawnPos = startCell;
        entry.exitPos = exitCell;

        // Old chests must never spill loose items while a reused arena is rebuilt.
        clearOldContainers(level,minX,minZ,maxX,maxZ);
        buildShell(level, minX, minZ, maxX, maxZ, type, random);
        carveOpenCells(level, open, centerX, centerZ, random, rooms);
        decorate(level, open, centerX, centerZ, random, type, rooms);
        rooms.build(level, centerX - GRID / 2, centerZ - GRID / 2, BASE_Y, HEIGHT, random);
        weatherExitNeighborhood(level, open, centerX, centerZ, exitCell, random);
        placeExit(level, entry, exitCell, random);
        spawnMobs(level, open, centerX, centerZ, random, type, exitCell);
        placeCeilingNests(level, entry, open, centerX, centerZ, random, exitCell, rooms);

        // Four minutes minimum plus time proportional to the ACTUAL shortest-path length.
        // A run with 150 turns should not have the same deadline as a 70-step run.
        int pathSteps = distances.values().stream().mapToInt(Integer::intValue).max().orElse(90);
        int bonusSeconds = Math.min(65, Math.max(0, (pathSteps-60)));
        entry.collapseStartAt = level.getGameTime()+20L*(180+bonusSeconds);
        entry.generated = true;
        buildCollapseQueue(entry, open, centerX, centerZ, distances);
        // No guide lamps: the maze does not reveal its route.
        buildLivingPassages(entry, open, centerX, centerZ, random, rooms);
        entry.nextShift = level.getGameTime() + 20 * 15;
        entry.nextPursuerAt = level.getGameTime() + 20 * 35;
        entry.nextDarknessAt=level.getGameTime()+20L*(30+random.nextInt(26));
    }

    /** Physical cyan lamps on the guaranteed route every ten steps, no waypoints or shaders needed. */
    private static void markCriticalRoute(ServerLevel level, PocketRunEntry entry, boolean[][] open,
                                          Map<Long, Integer> distances, int centerX, int centerZ) {
        int gx = entry.exitPos.getX() - (centerX - GRID / 2);
        int gz = entry.exitPos.getZ() - (centerZ - GRID / 2);
        int step = 0;
        while (gx != 1 || gz != 1) {
            if (++step % 10 == 0) {
                BlockPos pos = gridToWorld(centerX, centerZ, gx, gz).above(HEIGHT-1);
                if (level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.SOUL_LANTERN.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 3);
                }
            }
            int distance = distances.getOrDefault(key(gx, gz), 0);
            if (distance == 0) break;
            boolean moved = false;
            for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int nx = gx + dir[0], nz = gz + dir[1];
                if (distances.getOrDefault(key(nx,nz), Integer.MAX_VALUE) == distance - 1) {
                    gx = nx; gz = nz; moved = true; break;
                }
            }
            if (!moved) break;
        }
    }

    private static boolean[][] carveMaze(RandomSource random, int type) {
        boolean[][] open = new boolean[GRID][GRID];
        boolean[][] visited = new boolean[GRID][GRID];
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{1, 1});
        visited[1][1] = true;
        open[1][1] = true;

        int[][] dirs = {{2,0},{-2,0},{0,2},{0,-2}};
        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            List<int[]> options = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = current[0] + d[0];
                int nz = current[1] + d[1];
                if (nx > 0 && nz > 0 && nx < GRID - 1 && nz < GRID - 1 && !visited[nx][nz]) {
                    options.add(d);
                }
            }
            if (options.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] d = options.get(random.nextInt(options.size()));
            int mx = current[0] + d[0] / 2;
            int mz = current[1] + d[1] / 2;
            int nx = current[0] + d[0];
            int nz = current[1] + d[1];
            open[mx][mz] = true;
            open[nx][nz] = true;
            visited[nx][nz] = true;
            stack.push(new int[]{nx, nz});
        }

        int extraBreaks = switch (type) {
            case 1 -> 18;
            case 2 -> 10;
            default -> 6;
        };
        for (int i = 0; i < extraBreaks; i++) {
            int x = 2 + random.nextInt(GRID - 4);
            int z = 2 + random.nextInt(GRID - 4);
            if (random.nextFloat() < 0.65F) {
                open[x][z] = true;
            }
        }
        return open;
    }

    private static void clearOldContainers(ServerLevel level,int minX,int minZ,int maxX,int maxZ) {
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
            for(int y=BASE_Y+1;y<BASE_Y+HEIGHT;y++){
                if(level.getBlockEntity(new BlockPos(x,y,z)) instanceof Container c)c.clearContent();
            }
        }
        // A sector can be reused; never carry over dropped loot from another run.
        var bounds=new net.minecraft.world.phys.AABB(minX,BASE_Y-2,minZ,maxX+1,BASE_Y+HEIGHT+2,maxZ+1);
        for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,bounds))
            item.discard();
    }

    private static void buildShell(ServerLevel level, int minX, int minZ, int maxX, int maxZ, int type, RandomSource random) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                level.setBlock(new BlockPos(x, BASE_Y, z), floorBlock(type, random), 2);
                level.setBlock(new BlockPos(x, BASE_Y + HEIGHT, z), roofBlock(type, random), 2);
                for (int y = BASE_Y + 1; y < BASE_Y + HEIGHT; y++) {
                    level.setBlock(new BlockPos(x, y, z), wallBlock(type, random), 2);
                }
            }
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = BASE_Y - 1; y <= BASE_Y + HEIGHT + 1; y++) {
                level.setBlock(new BlockPos(x, y, minZ - 1), Blocks.BARRIER.defaultBlockState(), 2);
                level.setBlock(new BlockPos(x, y, maxZ + 1), Blocks.BARRIER.defaultBlockState(), 2);
            }
        }
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = BASE_Y - 1; y <= BASE_Y + HEIGHT + 1; y++) {
                level.setBlock(new BlockPos(minX - 1, y, z), Blocks.BARRIER.defaultBlockState(), 2);
                level.setBlock(new BlockPos(maxX + 1, y, z), Blocks.BARRIER.defaultBlockState(), 2);
            }
        }
    }

    private static void carveOpenCells(ServerLevel level, boolean[][] open, int centerX, int centerZ, RandomSource random, PocketRooms rooms) {
        for (int gx = 0; gx < GRID; gx++) {
            for (int gz = 0; gz < GRID; gz++) {
                if (!open[gx][gz]) continue;
                BlockPos pos = gridToWorld(centerX, centerZ, gx, gz);
                for (int y = BASE_Y + 1; y < BASE_Y + HEIGHT; y++) {
                    level.setBlock(new BlockPos(pos.getX(), y, pos.getZ()), Blocks.AIR.defaultBlockState(), 2);
                }
                // No invisible floor holes on walkable cells, especially in room entrances.
            }
        }
    }

    private static void decorate(ServerLevel level, boolean[][] open, int centerX, int centerZ, RandomSource random, int type, PocketRooms rooms) {
        for (int gx = 1; gx < GRID - 1; gx++) {
            for (int gz = 1; gz < GRID - 1; gz++) {
                if (!open[gx][gz] || rooms.inRoom(gx, gz)) continue;
                BlockPos pos = gridToWorld(centerX, centerZ, gx, gz);
                if (random.nextFloat() < 0.035F) {
                    level.setBlock(pos.above(HEIGHT-1), Blocks.SOUL_LANTERN.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 3);
                } else if (random.nextFloat() < 0.023F) {
                    level.setBlock(pos.above(), Blocks.SOUL_TORCH.defaultBlockState(), 3);
                }
                // Color language communicates districts, never the correct direction.
                // Change floor material only; keep all occupied walking cells navigable.
                float detail = random.nextFloat();
                if (type==0 && detail<.14F)
                    level.setBlock(pos,Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),2);
                else if(type==1 && detail<.21F)
                    level.setBlock(pos,ModBlocks.CRACKED_VOID_BRICKS.get().defaultBlockState(),2);
                else if(type==2 && detail<.17F)
                    level.setBlock(pos,Blocks.SCULK.defaultBlockState(),2);
                if (type==2 && random.nextFloat()<.04F &&
                     level.getBlockState(pos.above(HEIGHT-1)).isAir())
                    level.setBlock(pos.above(HEIGHT-1),Blocks.CHAIN.defaultBlockState(),2);
            }
        }
    }

    /** Final district uses distinct stone and occasional crying obsidian; not a breadcrumb line. */
    private static void weatherExitNeighborhood(ServerLevel level, boolean[][] open, int cx, int cz,
                                                BlockPos exit, RandomSource random) {
        for(int gx=1;gx<GRID-1;gx++) for(int gz=1;gz<GRID-1;gz++) {
            if(!open[gx][gz])continue;
            BlockPos cell=gridToWorld(cx,cz,gx,gz);
            if(cell.distSqr(exit)<100 && random.nextFloat()<.36F) {
                level.setBlock(cell,Blocks.CRYING_OBSIDIAN.defaultBlockState(),2);
            }
        }
    }

    /** A boss-scale 13x13 terminus. No boss entity is spawned in 0.6.2. */
    private static void reserveArena(boolean[][] open, int cx, int cz, BlockPos centre) {
        int x=centre.getX()-(cx-GRID/2), z=centre.getZ()-(cz-GRID/2);
        for(int dx=-6;dx<=6;dx++) for(int dz=-6;dz<=6;dz++) {
            int gx=x+dx,gz=z+dz;
            if(gx>0&&gz>0&&gx<GRID-1&&gz<GRID-1)open[gx][gz]=true;
        }
    }

    private static void placeExit(ServerLevel level, PocketRunEntry entry, BlockPos exitCell, RandomSource random) {
        // Clear all original narrow partitions before installing a single cohesive arena.
        for (int dx=-6; dx<=6; dx++) for (int dz=-6; dz<=6; dz++) {
            BlockPos floor=exitCell.offset(dx,0,dz);
            level.setBlock(floor, (Math.abs(dx)==6||Math.abs(dz)==6)
                    ? ModBlocks.RIFT_CARVED_STONE.get().defaultBlockState()
                    : (Math.abs(dx)%4==0||Math.abs(dz)%4==0)
                    ? ModBlocks.RITUAL_MOSAIC.get().defaultBlockState()
                    : ModBlocks.ASHEN_TILES.get().defaultBlockState(), 2);
            for(int dy=1; dy<HEIGHT; dy++)
                level.setBlock(floor.above(dy),Blocks.AIR.defaultBlockState(),2);
            level.setBlock(floor.above(HEIGHT),ModBlocks.RIFT_CARVED_STONE.get().defaultBlockState(),2);
        }
        // Four corner pillars, set back from the routes. No perimeter wall seals a
        // pre-existing maze connection, so the arena always remains reachable.
        for(int dx:new int[]{-5,5})for(int dz:new int[]{-5,5}){
            BlockPos pillar=exitCell.offset(dx,0,dz);
            for(int dy=1;dy<HEIGHT;dy++)
                level.setBlock(pillar.above(dy),ModBlocks.RIBBED_VOID_PILLAR.get().defaultBlockState(),2);
        }
        // Portal is a proper far-wall focus, with a clear 3-block approach.
        BlockPos portalPos=exitCell.offset(0,1,5);
        for (int dy=1;dy<=3;dy++) for(int dx=-2;dx<=2;dx++){
            BlockPos at=exitCell.offset(dx,dy,5);
            if (Math.abs(dx)==2 || dy==3)
                level.setBlock(at,ModBlocks.VOID_BRICKS.get().defaultBlockState(),2);
        }
        level.setBlock(portalPos,ModBlocks.RETURN_PORTAL.get().defaultBlockState(),3);
        level.setBlock(portalPos.above(),ModBlocks.RETURN_PORTAL.get().defaultBlockState(),3);
        // Place the reward container LAST. No subsequent room decoration overwrites it.
        BlockPos chestPos=exitCell.offset(-4,1,3);
        level.setBlock(chestPos,Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING,Direction.SOUTH),3);
        if(level.getBlockEntity(chestPos) instanceof Container chest)fillFinalChest(chest,random);
        for(int dx:new int[]{-4,0,4}){
            BlockPos lamp=exitCell.offset(dx,HEIGHT-1,-4);
            level.setBlock(lamp,Blocks.SOUL_LANTERN.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LanternBlock.HANGING,true),3);
        }
        level.playSound(null,portalPos,SoundEvents.END_PORTAL_FRAME_FILL,SoundSource.BLOCKS,1.0F,.55F);
    }

    private static void fillFinalChest(Container container, RandomSource random) {
        List<ItemStack> rewards = new ArrayList<>();
        rewards.add(new ItemStack(Items.DIAMOND, 2 + random.nextInt(5)));
        rewards.add(new ItemStack(Items.ENDER_PEARL, 2 + random.nextInt(4)));
        rewards.add(new ItemStack(Items.EXPERIENCE_BOTTLE, 8 + random.nextInt(18)));
        rewards.add(new ItemStack(Items.ECHO_SHARD, random.nextInt(3)));
        rewards.add(new ItemStack(Items.CRYING_OBSIDIAN, 4 + random.nextInt(8)));
        if (random.nextFloat() < 0.30F) rewards.add(new ItemStack(Items.NETHERITE_SCRAP, 1));
        if (random.nextFloat() < 0.18F) rewards.add(new ItemStack(ModItems.VOIDCLEAVER.get()));
        if (random.nextFloat() < 0.20F) rewards.add(new ItemStack(ModItems.RIFT_FANG.get()));
        if (random.nextFloat() < 0.16F) rewards.add(new ItemStack(ModItems.OBSIDIAN_MAW.get()));
        if (random.nextFloat() < 0.14F) rewards.add(new ItemStack(ModItems.HOLLOW_PICKAXE.get()));
        if (random.nextFloat() < 0.14F) rewards.add(new ItemStack(ModItems.GRAVE_SPADE.get()));
        // Every successful raid pays at least one signature artifact.
        var guaranteed = java.util.List.of(ModItems.VOIDCLEAVER.get(), ModItems.RIFT_FANG.get(),
                ModItems.OBSIDIAN_MAW.get(), ModItems.HOLLOW_PICKAXE.get(), ModItems.GRAVE_SPADE.get());
        rewards.add(new ItemStack(guaranteed.get(random.nextInt(guaranteed.size()))));
        CompatLoot.appendOptionalRewards(rewards, random);

        for (ItemStack stack : rewards) {
            if (!stack.isEmpty()) {
                int slot = random.nextInt(container.getContainerSize());
                for (int attempts = 0; attempts < 20 && !container.getItem(slot).isEmpty(); attempts++) {
                    slot = random.nextInt(container.getContainerSize());
                }
                if (container.getItem(slot).isEmpty()) {
                    container.setItem(slot, stack);
                }
            }
        }
    }

    /** Place physical nests embedded in the roof, rather than fixed wave spawning. */
    private static void placeCeilingNests(ServerLevel level, PocketRunEntry entry, boolean[][] open,
                                          int centerX, int centerZ, RandomSource random, BlockPos exitCell, PocketRooms rooms) {
        List<BlockPos> cells = new ArrayList<>();
        for (int gx = 3; gx < GRID-3; gx++) {
            for (int gz = 3; gz < GRID-3; gz++) {
                if (!open[gx][gz] || rooms.inRoom(gx, gz)) continue;
                BlockPos base=gridToWorld(centerX,centerZ,gx,gz);
                if (base.distSqr(entry.spawnPos == null ? gridToWorld(centerX,centerZ,1,1) : entry.spawnPos) < 56
                        || base.distSqr(exitCell)<196) continue;
                cells.add(base);
            }
        }
        java.util.Collections.shuffle(cells,new java.util.Random(random.nextLong()));
        int amount = Math.min(cells.size(), 7 + random.nextInt(4));
        for (int i=0;i<amount;i++) {
            BlockPos roof = cells.get(i).above(HEIGHT);
            level.setBlock(roof,ModBlocks.CORRUPTION_NEST.get().defaultBlockState(),3);
            entry.spawnerPositions.add(roof.asLong());
        }
    }

    /**
     * Spawn only in clear two-wide pockets, with separation between enemies.
     * No random Wither Skeleton mini-boss: the empty final arena is reserved for
     * a future unique boss.
     */
    private static void spawnMobs(ServerLevel level, boolean[][] open, int centerX, int centerZ,
                                  RandomSource random, int type, BlockPos exitCell) {
        List<BlockPos> candidates=new ArrayList<>();
        BlockPos start=gridToWorld(centerX,centerZ,1,1);
        for(int gx=3;gx<GRID-3;gx++)for(int gz=3;gz<GRID-3;gz++){
            if(!open[gx][gz] || random.nextFloat()>.16F)continue;
            int nearby=(open[gx-1][gz]?1:0)+(open[gx+1][gz]?1:0)
                      +(open[gx][gz-1]?1:0)+(open[gx][gz+1]?1:0);
            if(nearby<3)continue;
            BlockPos pos=gridToWorld(centerX,centerZ,gx,gz).above();
            if(pos.distSqr(exitCell)<196 || pos.distSqr(start)<81)continue;
            if(!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
               || !level.getBlockState(pos.below()).isCollisionShapeFullBlock(level,pos.below()))continue;
            candidates.add(pos);
        }
        java.util.Collections.shuffle(candidates,new java.util.Random(random.nextLong()));
        List<BlockPos> chosen=new ArrayList<>();
        int limit=type==2?11:type==1?9:7;
        for(BlockPos pos:candidates){
            if(chosen.size()>=limit)break;
            if(chosen.stream().anyMatch(other->other.distSqr(pos)<36))continue;
            EntityType<? extends Mob> mobType=random.nextBoolean()
                    ? ModEntities.CORRUPTED_SKELETON.get() : ModEntities.CORRUPTED_ZOMBIE.get();
            Mob mob=mobType.create(level);
            if(mob==null)continue;
            mob.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,random.nextFloat()*360F,0);
            mob.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.TRIGGERED,null);
            if(!level.noCollision(mob))continue;
            if(level.addFreshEntity(mob))chosen.add(pos);
        }
    }

    private static void enchant(ServerLevel level, ItemStack stack, ResourceKey<Enchantment> enchantment, int power) {
        level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(enchantment)
                .ifPresent(holder -> stack.enchant(holder, power));
    }

    /** Collapse reaches ALL stone, nests, barriers, exit and the final chest, not only open corridors. */
    private static void buildCollapseQueue(PocketRunEntry entry, boolean[][] open, int centerX, int centerZ,
                                           Map<Long, Integer> distances) {
        List<BlockPos> columns = new ArrayList<>();
        for (int x = entry.minX - 1; x <= entry.maxX + 1; x++) {
            for (int z = entry.minZ - 1; z <= entry.maxZ + 1; z++) {
                columns.add(new BlockPos(x, BASE_Y, z));
            }
        }
        // From entrance towards the final chest. Near walls fade first, distant ones last.
        columns.sort(Comparator.comparingInt(pos ->
                Math.abs(pos.getX() - entry.spawnPos.getX()) + Math.abs(pos.getZ() - entry.spawnPos.getZ())));
        for (BlockPos column : columns) {
            for (int y = BASE_Y - 1; y <= BASE_Y + HEIGHT + 1; y++) {
                entry.collapseBlocks.add(new BlockPos(column.getX(), y, column.getZ()).asLong());
            }
        }
    }

    /** Candidate walls join existing parallel corridors, adding optional shortcuts. */
    private static void buildLivingPassages(PocketRunEntry entry, boolean[][] open, int cx, int cz,
                                             RandomSource random, PocketRooms rooms) {
        List<Long> cells = new ArrayList<>();
        for (int x = 2; x < GRID - 2; x++) {
            for (int z = 2; z < GRID - 2; z++) {
                if (open[x][z] || rooms.nearRoom(x,z)) continue;
                boolean ns = open[x-1][z] && open[x+1][z];
                boolean ew = open[x][z-1] && open[x][z+1];
                if (!(ns || ew)) continue;
                BlockPos pos = gridToWorld(cx, cz, x, z);
                if (pos.distSqr(entry.spawnPos) < 80 || pos.distSqr(entry.exitPos) < 196) continue;
                cells.add(pos.asLong());
            }
        }
        java.util.Collections.shuffle(cells, new java.util.Random(random.nextLong()));
        entry.phaseCandidates.addAll(cells.subList(0, Math.min(cells.size(), 80)));
    }

    private static Map<Long, Integer> computeDistances(boolean[][] open, int sx, int sz) {
        Map<Long, Integer> distance = new HashMap<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{sx, sz});
        distance.put(key(sx, sz), 0);
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int base = distance.get(key(current[0], current[1]));
            for (int[] d : dirs) {
                int nx = current[0] + d[0];
                int nz = current[1] + d[1];
                if (nx < 0 || nz < 0 || nx >= GRID || nz >= GRID || !open[nx][nz]) continue;
                long key = key(nx, nz);
                if (!distance.containsKey(key)) {
                    distance.put(key, base + 1);
                    queue.add(new int[]{nx, nz});
                }
            }
        }
        return distance;
    }

    private static BlockPos findFarthest(boolean[][] open, Map<Long, Integer> distances, int centerX, int centerZ) {
        long best = distances.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).map(Map.Entry::getKey).orElse(key(GRID - 2, GRID - 2));
        int gx = (int)(best >> 32);
        int gz = (int)(long)best;
        return gridToWorld(centerX, centerZ, gx, gz);
    }

    /** The final 5x5 exit chamber never destroys a themed room or its entrances. */
    private static BlockPos findFarthestOutsideRooms(boolean[][] open, Map<Long,Integer> distances,
                                                       PocketRooms rooms, int centerX, int centerZ) {
        long best = -1L;
        int bestDist = -1;
        for (Map.Entry<Long,Integer> pair : distances.entrySet()) {
            int x=(int)(pair.getKey()>>32), z=(int)(long)pair.getKey();
            if (rooms.overlapsArena(x,z) || x<8 || z<8 || x>=GRID-8 || z>=GRID-8) continue;
            if (pair.getValue()>bestDist) {best=pair.getKey();bestDist=pair.getValue();}
        }
        return best<0 ? findFarthest(open,distances,centerX,centerZ)
                : gridToWorld(centerX,centerZ,(int)(best>>32),(int)best);
    }

    private static long key(int x, int z) {
        return ((long)x << 32) | (z & 0xffffffffL);
    }

    private static BlockPos gridToWorld(int centerX, int centerZ, int gx, int gz) {
        return new BlockPos(centerX - GRID / 2 + gx, BASE_Y, centerZ - GRID / 2 + gz);
    }

    private static BlockState floorBlock(int type, RandomSource random) {
        return random.nextFloat() < 0.22F
                ? ModBlocks.CRACKED_POCKET_STONE.get().defaultBlockState()
                : ModBlocks.POCKET_STONE.get().defaultBlockState();
    }

    private static BlockState roofBlock(int type, RandomSource random) {
        return random.nextFloat() < 0.18F
                ? ModBlocks.CRACKED_POCKET_STONE.get().defaultBlockState()
                : ModBlocks.VOID_BRICKS.get().defaultBlockState();
    }

    private static BlockState wallBlock(int type, RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.13F) return ModBlocks.CRACKED_POCKET_STONE.get().defaultBlockState();
        if (roll < 0.43F) return ModBlocks.CRACKED_VOID_BRICKS.get().defaultBlockState();
        if (roll < 0.67F) return ModBlocks.VOID_BRICKS.get().defaultBlockState();
        return ModBlocks.POCKET_STONE.get().defaultBlockState();
    }
}
