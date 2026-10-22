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

    private static final int GRID = 49;
    private static final int HEIGHT = 5;
    private static final int BASE_Y = 80;
    private static final int SPACING = 256;
    private static final int SECTOR_X = 8192;
    private static final int SECTOR_Z = 16384;

    public static void generate(ServerLevel level, PocketRunEntry entry) {
        RandomSource random = RandomSource.create(level.getSeed() ^ (entry.id * 341873128712L));
        int type = entry.stage>=2?2:entry.id%3;
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
        // The southeast sanctuary is fixed per pocket: all routes lead to a real destination.
        // 37,37 is guaranteed by the DFS's odd-cell maze carving.
        BlockPos exitCell = gridToWorld(centerX, centerZ, GRID - 12, GRID - 12);
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
        if(entry.stage==1)placeDescentGate(level,exitCell);
        else {
            // Stage II earned rewards are separate from Stage I chest.
            BlockPos reward=exitCell.offset(-6,1,-5);
            if(level.getBlockEntity(reward) instanceof Container c) {
                c.setItem(22,new ItemStack(Items.NETHERITE_SCRAP,2));
                c.setItem(25,new ItemStack(Items.ECHO_SHARD,3));
            }
        }
        sculptHazards(level, open, rooms, centerX, centerZ, startCell, exitCell, random,entry.stage>=2);
        spawnMobs(level, open, centerX, centerZ, random, type, exitCell);
        placeCeilingNests(level, entry, open, centerX, centerZ, random, exitCell, rooms);

        // Four minutes minimum plus time proportional to the ACTUAL shortest-path length.
        // A run with 150 turns should not have the same deadline as a 70-step run.
        int pathSteps = distances.values().stream().mapToInt(Integer::intValue).max().orElse(90);
        int bonusSeconds = Math.min(65, Math.max(0, (pathSteps-60)));
        entry.collapseStartAt = level.getGameTime()+20L*((entry.stage>=2?155:180)+bonusSeconds);
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


    /** Second portal sits to the side; the original Return Portal and chest stay functional. */
    private static void placeDescentGate(ServerLevel level, BlockPos exit) {
        BlockPos gate=exit.offset(6,1,0);
        level.setBlock(gate,ModBlocks.DESCENT_GATE.get().defaultBlockState(),3);
        level.setBlock(gate.above(),ModBlocks.DESCENT_GATE.get().defaultBlockState(),3);
        for(int dx=-1;dx<=1;dx++)
            level.setBlock(gate.offset(dx,-1,0),ModBlocks.RITUAL_MOSAIC.get().defaultBlockState(),2);
    }

    /** Reserve a walkable ring around a 19x19 boss sanctuary, connected to the original maze. */
    private static void reserveArena(boolean[][] open, int cx, int cz, BlockPos centre) {
        int gx=centre.getX()-(cx-GRID/2), gz=centre.getZ()-(cz-GRID/2);
        for(int dx=-10;dx<=10;dx++)for(int dz=-10;dz<=10;dz++) {
            int x=gx+dx,z=gz+dz;
            if(x>0&&z>0&&x<GRID-1&&z<GRID-1)open[x][z]=true;
        }
    }

    /** Nineteen by nineteen: no boss yet, uninterrupted centre, protected loot and portal. */
    private static void placeExit(ServerLevel level, PocketRunEntry entry, BlockPos exit, RandomSource random) {
        for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++) {
            BlockPos floor=exit.offset(dx,0,dz);
            int edge=Math.max(Math.abs(dx),Math.abs(dz));
            boolean trim=edge>=8;
            level.setBlock(floor,trim?ModBlocks.RIFT_CARVED_STONE.get().defaultBlockState():
                    ((Math.abs(dx)%5==0 || Math.abs(dz)%5==0)
                            ? ModBlocks.RITUAL_MOSAIC.get().defaultBlockState()
                            : ModBlocks.ASHEN_TILES.get().defaultBlockState()),2);
            level.setBlock(floor.above(HEIGHT),trim?ModBlocks.RIBBED_VOID_PILLAR.get().defaultBlockState()
                    :ModBlocks.RIFT_CARVED_STONE.get().defaultBlockState(),2);
            boolean wall=edge==9;
            // Three ceremonial 5-block openings, with the northern wall holding the portal.
            boolean gate = (dx==-9 && Math.abs(dz)<=2)
                    || (dx==9 && Math.abs(dz)<=2)
                    || (dz==9 && Math.abs(dx)<=2);
            for(int dy=1;dy<HEIGHT;dy++)
                level.setBlock(floor.above(dy),wall&&!gate
                        ? ModBlocks.CRACKED_VOID_BRICKS.get().defaultBlockState()
                        : Blocks.AIR.defaultBlockState(),2);
        }
        // Symmetrical buttresses and four braziers; all outside the central combat disc.
        for(int dx:new int[]{-7,7})for(int dz:new int[]{-7,7}){
            BlockPos base=exit.offset(dx,0,dz);
            for(int dy=1;dy<HEIGHT;dy++)
                level.setBlock(base.above(dy),ModBlocks.RIBBED_VOID_PILLAR.get().defaultBlockState(),2);
            level.setBlock(base.above(HEIGHT-1),Blocks.SOUL_LANTERN.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LanternBlock.HANGING,true),3);
        }
        // End wall focus: no boss, no central obstruction.
        BlockPos portal=exit.offset(0,1,-8);
        for(int dx=-3;dx<=3;dx++)for(int dy=1;dy<=4;dy++) {
            boolean opening=Math.abs(dx)<=1 && dy<=2;
            level.setBlock(exit.offset(dx,dy,-8),opening
                    ? ModBlocks.RETURN_PORTAL.get().defaultBlockState()
                    : ModBlocks.VOID_BRICKS.get().defaultBlockState(),2);
        }
        // Place chest LAST and keep its whole 3x3 neighbourhood outside future collapse.
        BlockPos reward=exit.offset(-6,1,-5);
        level.setBlock(reward,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,Direction.SOUTH),3);
        if(level.getBlockEntity(reward) instanceof Container container)fillFinalChest(container,random);
        level.playSound(null,portal,SoundEvents.END_PORTAL_FRAME_FILL,SoundSource.BLOCKS,1F,.52F);
    }

    /** Remove nonessential floors, but never disconnect the spawn from the reward sanctuary. */
    private static void sculptHazards(ServerLevel level, boolean[][] open, PocketRooms rooms,
                                      int cx,int cz,BlockPos start,BlockPos exit,RandomSource random,boolean deep) {
        List<int[]> candidates=new ArrayList<>();
        for(int gx=3;gx<GRID-3;gx++)for(int gz=3;gz<GRID-3;gz++) {
            if(!open[gx][gz] || rooms.nearRoom(gx,gz))continue;
            BlockPos floor=gridToWorld(cx,cz,gx,gz);
            if(floor.distSqr(start)<110 ||
               (Math.abs(floor.getX()-exit.getX())<=11 &&
                Math.abs(floor.getZ()-exit.getZ())<=11))continue;
            candidates.add(new int[]{gx,gz});
        }
        java.util.Collections.shuffle(candidates,new java.util.Random(random.nextLong()));
        int holes=0,unstable=0;
        for(int[] point:candidates){
            int gx=point[0],gz=point[1];
            BlockPos floor=gridToWorld(cx,cz,gx,gz);
            if(!level.getBlockState(floor.above()).isAir() ||
               !level.getBlockState(floor).isCollisionShapeFullBlock(level,floor))continue;
            // Never make a pit in a passage which is the only route to the portal.
            if(holes<(deep?48:32) && random.nextFloat()<(deep?.34F:.24F)){
                open[gx][gz]=false;
                if(hasRoute(open,1,1,GRID-12,GRID-12)){
                    level.setBlock(floor,Blocks.AIR.defaultBlockState(),2);
                    holes++;
                    continue;
                }
                open[gx][gz]=true;
            }
            if(unstable<(deep?100:60) && random.nextFloat()<(deep?.58F:.42F)){
                level.setBlock(floor,ModBlocks.UNSTABLE_VOID_BRICKS.get().defaultBlockState(),2);
                unstable++;
            }
        }
    }

    private static boolean hasRoute(boolean[][] open,int sx,int sz,int ex,int ez){
        boolean[][] seen=new boolean[GRID][GRID];
        ArrayDeque<int[]> todo=new ArrayDeque<>();
        todo.add(new int[]{sx,sz});seen[sx][sz]=true;
        int[][] directions={{1,0},{-1,0},{0,1},{0,-1}};
        while(!todo.isEmpty()){
            int[] p=todo.removeFirst();
            if(p[0]==ex&&p[1]==ez)return true;
            for(int[] d:directions){
                int x=p[0]+d[0],z=p[1]+d[1];
                if(x>0 && z>0 && x<GRID-1 && z<GRID-1 && open[x][z] && !seen[x][z]){
                    seen[x][z]=true;todo.addLast(new int[]{x,z});
                }
            }
        }
        return false;
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
                        || (Math.abs(base.getX()-exitCell.getX())<=11 && Math.abs(base.getZ()-exitCell.getZ())<=11)) continue;
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
                if (pos.distSqr(entry.spawnPos) < 80 || (Math.abs(pos.getX()-entry.exitPos.getX())<=11 && Math.abs(pos.getZ()-entry.exitPos.getZ())<=11)) continue;
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
