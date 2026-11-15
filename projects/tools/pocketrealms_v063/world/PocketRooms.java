package com.kirill.pocketrealms.world;

import com.kirill.pocketrealms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Real enclosed, roofed chambers with doors, architecture and furniture.
 * A loop of connected walkway surrounds every chamber. Thus a wall never silently
 * cuts off the old maze; a weighted flood-fill additionally repairs edge cases.
 * The 2-D mask is the same mask used by player routing, monster spawning and exit selection.
 */
public final class PocketRooms {
    public enum Theme { OSSUARY, RITUAL, ARCHIVE, PRISON, FORGE, CISTERN }

    public record Room(int cx, int cz, int rx, int rz, Theme theme, boolean eastWest) {
        boolean interior(int x, int z) {return Math.abs(x-cx)<rx && Math.abs(z-cz)<rz;}
        boolean shell(int x,int z) {return Math.abs(x-cx)<=rx && Math.abs(z-cz)<=rz;}
        boolean surrounds(int x,int z) {return Math.abs(x-cx)<=rx+1 && Math.abs(z-cz)<=rz+1;}
        boolean door(int x,int z) {
            return eastWest ? (Math.abs(z-cz)<=1 && Math.abs(x-cx)==rx)
                    : (Math.abs(x-cx)<=1 && Math.abs(z-cz)==rz);
        }
    }

    private final List<Room> rooms = new ArrayList<>();
    private final boolean[][] protectedWalls;
    private final int size;

    private PocketRooms(int size) {
        this.size=size;
        this.protectedWalls=new boolean[size][size];
    }

    public boolean inRoom(int x,int z) {
        for(Room r:rooms) if(r.shell(x,z))return true;
        return false;
    }
    public boolean overlapsExit(int x,int z) {
        for(Room r:rooms) if(Math.abs(x-r.cx)<=r.rx+3 && Math.abs(z-r.cz)<=r.rz+3)return true;
        return false;
    }
    public boolean overlapsArena(int x,int z) {
        for(Room room:rooms) {
            if(Math.abs(x-room.cx)<=room.rx+7 && Math.abs(z-room.cz)<=room.rz+7)
                return true;
        }
        return false;
    }
    public boolean nearRoom(int x,int z) {
        for(Room r:rooms) if(r.surrounds(x,z))return true;
        return false;
    }

    public static PocketRooms plan(boolean[][] open,RandomSource random,int variant) {
        int n=open.length;
        PocketRooms result=new PocketRooms(n);
        // Five to six distinct chambers per 39x39 maze, never next to the spawn.
        int target=3+random.nextInt(2);
        for(int attempt=0;attempt<750 && result.rooms.size()<target;attempt++){
            Theme theme=Theme.values()[(result.rooms.size()+variant+random.nextInt(3))%Theme.values().length];
            // Mix compact shrines, long galleries and larger ceremonial chambers.
            int rx=3+random.nextInt(3);
            int rz=3+random.nextInt(3);
            if (theme==Theme.PRISON) rz=3;
            if (theme==Theme.ARCHIVE) rx=5;
            int x=rx+4+random.nextInt(n-2*(rx+4));
            int z=rz+4+random.nextInt(n-2*(rz+4));
            if(x-rx<=3 && z-rz<=3)continue;
            // Keep all themed chambers away from the 19x19 sanctuary AND its outer walkway.
            int sanct=n-12;
            if (Math.abs(x-sanct)<=rx+12 && Math.abs(z-sanct)<=rz+12) continue;
            Room room=new Room(x,z,rx,rz,theme,random.nextBoolean());
            boolean collides=false;
            for(Room other:result.rooms) {
                // Keep a corridor at least 2 wide between architectural shells.
                if(Math.abs(other.cx-x)<=other.rx+rx+3 && Math.abs(other.cz-z)<=other.rz+rz+3) {
                    collides=true;break;
                }
            }
            if(!collides)result.rooms.add(room);
        }
        for(Room r:result.rooms) {
            for(int x=r.cx-r.rx-1;x<=r.cx+r.rx+1;x++) {
                for(int z=r.cz-r.rz-1;z<=r.cz+r.rz+1;z++) {
                    if(r.shell(x,z)) {
                        boolean doorway=r.door(x,z);
                        open[x][z]=r.interior(x,z)||doorway;
                        result.protectedWalls[x][z]=!doorway && !r.interior(x,z);
                    } else {
                        // Make a reliable bypass corridor around the entire room exterior.
                        open[x][z]=true;
                    }
                }
            }
        }
        // The centre of the reserved sanctuary is a carved, reachable odd grid cell.
        open[n-12][n-12]=true;
        result.reconnectAll(open);
        return result;
    }

    private static boolean[][] reachable(boolean[][] open) {
        int n=open.length;
        boolean[][] visited=new boolean[n][n];
        ArrayDeque<int[]> queue=new ArrayDeque<>();
        queue.add(new int[]{1,1}); visited[1][1]=true;
        int[][] dir={{1,0},{-1,0},{0,1},{0,-1}};
        while(!queue.isEmpty()){
            int[] a=queue.removeFirst();
            for(int[] d:dir){int x=a[0]+d[0],z=a[1]+d[1];
                if(x>=1 && z>=1 && x<n-1 && z<n-1 && open[x][z] && !visited[x][z]){
                    visited[x][z]=true;queue.add(new int[]{x,z});
                }
            }
        }
        return visited;
    }
    /** Connect islands at minimal wall-breaking cost without erasing room walls. */
    private void reconnectAll(boolean[][] open) {
        int[][] dirs={{1,0},{-1,0},{0,1},{0,-1}};
        for(int pass=0;pass<size*size;pass++){
            boolean[][] seen=reachable(open);
            int targetX=-1,targetZ=-1;
            outer:for(int x=1;x<size-1;x++)for(int z=1;z<size-1;z++){
                if(open[x][z]&&!seen[x][z]){targetX=x;targetZ=z;break outer;}
            }
            if(targetX<0)return;
            // 0-1 Dijkstra from every reachable cell to target component.
            int[][] cost=new int[size][size],prev=new int[size][size];
            for(int i=0;i<size;i++){Arrays.fill(cost[i],Integer.MAX_VALUE);Arrays.fill(prev[i],-1);}
            PriorityQueue<int[]> pq=new PriorityQueue<>(Comparator.comparingInt(a->a[0]));
            for(int x=1;x<size-1;x++)for(int z=1;z<size-1;z++)if(seen[x][z]){
                cost[x][z]=0;pq.add(new int[]{0,x,z});
            }
            while(!pq.isEmpty()){
                int[] node=pq.poll();
                int x=node[1],z=node[2];
                if(node[0]!=cost[x][z])continue;
                if(x==targetX && z==targetZ)break;
                for(int d=0;d<dirs.length;d++){
                    int nx=x+dirs[d][0],nz=z+dirs[d][1];
                    if(nx<1||nz<1||nx>=size-1||nz>=size-1||protectedWalls[nx][nz])continue;
                    int next=node[0]+(open[nx][nz]?0:1);
                    if(next<cost[nx][nz]){cost[nx][nz]=next;prev[nx][nz]=d;pq.add(new int[]{next,nx,nz});}
                }
            }
            if(cost[targetX][targetZ]==Integer.MAX_VALUE)
                throw new IllegalStateException("Room layout isolated a maze region");
            int x=targetX,z=targetZ;
            while(!seen[x][z]){
                open[x][z]=true;
                int d=prev[x][z];
                if(d<0)throw new IllegalStateException("Invalid maze repair path");
                x-=dirs[d][0];z-=dirs[d][1];
            }
        }
        throw new IllegalStateException("Maze room connectivity repair did not converge");
    }

    public void build(ServerLevel level,int originX,int originZ,int floorY,int height,RandomSource random) {
        for(Room r:rooms) buildRoom(level,originX,originZ,floorY,height,r,random);
    }
    private static BlockPos at(int ox,int oz,int y,int x,int z) {return new BlockPos(ox+x,y,oz+z);}
    private static void block(ServerLevel level,BlockPos pos,BlockState state){level.setBlock(pos,state,2);}
    private static void buildRoom(ServerLevel level,int ox,int oz,int y,int height,Room r,RandomSource random){
        BlockState roomWall=ModBlocks.RIFT_CARVED_STONE.get().defaultBlockState();
        BlockState stone=ModBlocks.ASHEN_TILES.get().defaultBlockState();
        BlockState pillar=ModBlocks.RIBBED_VOID_PILLAR.get().defaultBlockState();
        // Structural walls and four massive corner buttresses. Genuine enclosure.
        for(int dx=-r.rx;dx<=r.rx;dx++)for(int dz=-r.rz;dz<=r.rz;dz++){
            int gx=r.cx+dx,gz=r.cz+dz;
            BlockPos floor=at(ox,oz,y,gx,gz);
            if(Math.abs(dx)==r.rx||Math.abs(dz)==r.rz){
                boolean entrance=r.door(gx,gz);
                for(int dy=1;dy<height;dy++){
                    if(entrance && dy<=3)block(level,floor.above(dy),Blocks.AIR.defaultBlockState());
                    else {
                        BlockState wall = roomWall;
                        if (r.theme==Theme.OSSUARY && random.nextFloat()<.28F)
                            wall=ModBlocks.BONE_WALL.get().defaultBlockState();
                        else if ((r.theme==Theme.PRISON||r.theme==Theme.FORGE) && random.nextFloat()<.23F)
                            wall=ModBlocks.CRACKED_VOID_BRICKS.get().defaultBlockState();
                        block(level,floor.above(dy),wall);
                    }
                }
                if(Math.abs(dx)==r.rx && Math.abs(dz)==r.rz)
                    for(int dy=1;dy<height;dy++)block(level,floor.above(dy),pillar);
            } else {
                // Deliberate tilework, not randomized noise everywhere.
                boolean mosaic=(Math.abs(dx)+Math.abs(dz))%3==0;
                block(level,floor,mosaic?ModBlocks.RITUAL_MOSAIC.get().defaultBlockState():stone);
            }
            if(Math.abs(dx)==r.rx||Math.abs(dz)==r.rz)
                block(level,floor,ModBlocks.ASHEN_TILES.get().defaultBlockState());
            // Shadowed ceiling coffer in each room, different from generic corridor roof.
            block(level,floor.above(height),((Math.abs(dx)==r.rx||Math.abs(dz)==r.rz)
                    ?pillar:roomWall));
        }
        // Pierced stone arches at both doorways, 3-block clear passage for hitboxes.
        if(r.eastWest()){
            for(int side:new int[]{-1,1})arch(level,ox,oz,y,height,r.cx+side*r.rx,r.cz,true,pillar);
        }else{
            for(int side:new int[]{-1,1})arch(level,ox,oz,y,height,r.cx,r.cz+side*r.rz,false,pillar);
        }
        // Four interior columns/fixtures: corner-sited to preserve a wide centre aisle.
        for(int dx:new int[]{-r.rx+1,r.rx-1})for(int dz:new int[]{-r.rz+1,r.rz-1}){
            BlockPos pos=at(ox,oz,y,r.cx+dx,r.cz+dz);
            if(r.theme==Theme.PRISON){
                block(level,pos.above(),Blocks.IRON_BARS.defaultBlockState());
                block(level,pos.above(2),Blocks.IRON_BARS.defaultBlockState());
            }else{
                block(level,pos.above(),ModBlocks.BROKEN_PILLAR.get().defaultBlockState());
                if(r.theme==Theme.OSSUARY)block(level,pos.above(2),Blocks.SKELETON_SKULL.defaultBlockState());
            }
        }
        BlockPos centre=at(ox,oz,y,r.cx,r.cz);
        switch(r.theme){
            case OSSUARY -> {
                block(level,centre.above(),Blocks.BONE_BLOCK.defaultBlockState());
                block(level,centre.above(2),Blocks.WITHER_SKELETON_SKULL.defaultBlockState());
                wallFeature(level,ox,oz,y,r,Blocks.BONE_BLOCK.defaultBlockState());
            }
            case RITUAL -> {
                block(level,centre,ModBlocks.RITUAL_MOSAIC.get().defaultBlockState());
                block(level,centre.above(),ModBlocks.RITUAL_SLAB.get().defaultBlockState());
                block(level,centre.above(2),Blocks.SOUL_TORCH.defaultBlockState());
                for(int dx:new int[]{-2,2})for(int dz:new int[]{-2,2}){
                    BlockPos pos=centre.offset(dx,1,dz);
                    if(level.getBlockState(pos).isAir())block(level,pos,Blocks.BLACK_CANDLE.defaultBlockState().setValue(CandleBlock.LIT,true));
                }
            }
            case ARCHIVE -> {
                wallFeature(level,ox,oz,y,r,Blocks.BOOKSHELF.defaultBlockState());
                block(level,centre.above(),Blocks.LECTERN.defaultBlockState());
            }
            case PRISON -> {
                wallFeature(level,ox,oz,y,r,Blocks.CHAIN.defaultBlockState());
                block(level,centre.above(),Blocks.CAULDRON.defaultBlockState());
                for(int dz:new int[]{-1,1}) {
                    BlockPos pos=centre.offset(2,1,dz);
                    if(level.getBlockState(pos).isAir())block(level,pos,Blocks.COBWEB.defaultBlockState());
                }
            }
            case FORGE -> {
                block(level,centre.above(),Blocks.ANVIL.defaultBlockState());
                wallFeature(level,ox,oz,y,r,Blocks.BLAST_FURNACE.defaultBlockState());
            }
            case CISTERN -> {
                block(level,centre.above(),Blocks.WATER_CAULDRON.defaultBlockState());
                wallFeature(level,ox,oz,y,r,Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
            }
        }
        // Real vent/floor grille with open holes and a solid backing one block below.
        if (r.theme==Theme.PRISON || r.theme==Theme.CISTERN || r.theme==Theme.FORGE) {
            for(int dx : new int[]{-2,2}) {
                BlockPos grate=at(ox,oz,y,r.cx+dx,r.cz+1);
                block(level,grate.below(),ModBlocks.CRACKED_VOID_BRICKS.get().defaultBlockState());
                block(level,grate,ModBlocks.RIFT_GRATE.get().defaultBlockState());
            }
        }
        // Optional crumbling tiles in a room's noncritical side alcove.
        // A backing floor one block below makes the fall escapable by jumping.
        if (random.nextFloat()<.60F) {
            BlockPos weak=at(ox,oz,y,r.cx+r.rx-2,r.cz+r.rz-2);
            block(level,weak.below(),ModBlocks.CRACKED_VOID_BRICKS.get().defaultBlockState());
            block(level,weak,ModBlocks.CRUMBLING_STONE.get().defaultBlockState());
        }
        // One low-light centre fixture in about half the chambers, never a guided trail.
        if(random.nextBoolean()){
            BlockPos lamp=centre.above(height-1);
            if(level.getBlockState(lamp).isAir())block(level,lamp,Blocks.SOUL_LANTERN.defaultBlockState()
                    .setValue(LanternBlock.HANGING,true));
        }
        // Optional side cache: meaningful reason to enter a room, not mandatory loot.
        if(random.nextFloat()<.32F){
            BlockPos cache=centre.offset(0,1,r.rz-1);
            if(level.getBlockState(cache).isAir()){
                block(level,cache,Blocks.BARREL.defaultBlockState());
                if(level.getBlockEntity(cache) instanceof Container inventory){
                    putLoot(inventory,random);
                }
            }
        }
    }
    private static void arch(ServerLevel level,int ox,int oz,int y,int height,
                             int gx,int gz,boolean onX,BlockState pillar){
        // The opening itself remains empty; carved imposts flank it and a lintel crowns it.
        BlockPos floor=at(ox,oz,y,gx,gz);
        block(level,floor.above(height-1),pillar);
        for(int sign:new int[]{-2,2}){
            BlockPos flank=onX?floor.offset(0,0,sign):floor.offset(sign,0,0);
            for(int dy=1;dy<height;dy++)block(level,flank.above(dy),pillar);
        }
    }
    private static void wallFeature(ServerLevel level,int ox,int oz,int y,Room r,BlockState state){
        // Upper wall niches behind furniture, without stealing the centre's walking space.
        BlockPos pos=at(ox,oz,y+2,r.cx+1,r.cz-r.rz);
        block(level,pos,state);
        pos=at(ox,oz,y+2,r.cx-1,r.cz+r.rz);
        block(level,pos,state);
    }
    private static void putLoot(Container inventory,RandomSource random){
        inventory.setItem(3,new ItemStack(Items.BONE,2+random.nextInt(5)));
        inventory.setItem(7,new ItemStack(Items.IRON_NUGGET,3+random.nextInt(7)));
        if(random.nextFloat()<.45F)inventory.setItem(12,new ItemStack(Items.ARROW,4+random.nextInt(8)));
        if(random.nextFloat()<.15F)inventory.setItem(19,new ItemStack(Items.ECHO_SHARD));
    }
}
