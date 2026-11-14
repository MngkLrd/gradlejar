package dev.minemods.halohaven;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class HavenVillages {
    private HavenVillages() {}
    public static final int WALK_Y = 111;
    private static final int SPACING = 512;
    private static final int TOP_Y = 110;

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!p.level().dimension().equals(HaloHaven.HEAVEN) || p.tickCount % 100 != 0) return;
        int cx = Math.round(p.getBlockX() / (float) SPACING) * SPACING;
        int cz = Math.round(p.getBlockZ() / (float) SPACING) * SPACING;
        if (Math.abs(p.getBlockX() - cx) < 108 && Math.abs(p.getBlockZ() - cz) < 108) {
            ensure((ServerLevel) p.level(), cx, cz);
        }
    }

    public static void ensure(ServerLevel level, int cx, int cz) {
        // Bedrock is a persistent marker, so a saved world never rebuilds villages.
        BlockPos marker = new BlockPos(cx, 93, cz);
        if (level.getBlockState(marker).is(Blocks.BEDROCK)) return;
        Random random = new Random(level.getSeed() ^ (cx * 71057317L) ^ (cz * 117135737L));

        // Each settlement rests on a deliberately sculpted island high above the lower islands.
        for (int dx = -47; dx <= 47; dx++) for (int dz = -47; dz <= 47; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            double wobble = 2.5 * Math.sin((cx+dx) * 0.23) + 2.4 * Math.cos((cz+dz)*0.18);
            if (d > 43 + wobble) continue;
            int deep = 2 + (int) (7 * Math.max(0, 1-d/49.0));
            int x = cx + dx, z = cz + dz;
            for (int y = TOP_Y - deep; y < TOP_Y; y++) {
                place(level, x, y, z, HaloHaven.CLOUDSTONE.get());
            }
            place(level, x, TOP_Y, z, HaloHaven.SKYGRASS.get());
            if (d > 25 && random.nextInt(85) == 0) {
                place(level, x, TOP_Y+1, z, random.nextBoolean() ? Blocks.AZURE_BLUET : Blocks.PINK_TULIP);
            }
            if (d > 24 && random.nextInt(320) == 0) tree(level, x, z);
        }
        // Invisible and unbreakable generation marker below the actual island.
        place(level, cx, 93, cz, Blocks.BEDROCK);
        // Carved roads: a luminous marble cross with metallic inlays.
        for (int t=-38; t<=38; t++) for (int s=-2; s<=2; s++) {
            if (Math.abs(t) > 37 || Math.sqrt(t*t+s*s)>40) continue;
            path(level,cx+t,cz+s,(t%8==0));
            path(level,cx+s,cz+t,(t%8==0));
        }
        for (int sx : new int[]{-1,1}) for (int sz : new int[]{-1,1}) {
            // All four homes share the same village vocabulary, rotated by their placement.
            house(level, cx+sx*20, cz+sz*20, random, sx, sz);
            for (int v=5;v<20;v++) {
                path(level,cx+sx*v,cz+sz*20,v%5==0);
                path(level,cx+sx*20,cz+sz*v,v%5==0);
            }
        }
        plaza(level, cx, cz);
        for (int x : new int[]{-36,36}) for (int z : new int[]{-9,9}) {
            monument(level,cx+x,cz+z);
        }
        for (int dx=-30;dx<=30;dx++) for (int dz=-30;dz<=30;dz++) {
            if (Math.abs(dx)<4 || Math.abs(dz)<4 || Math.abs(dx)>28 || Math.abs(dz)>28) continue;
            if (random.nextInt(160)==0) {
                place(level,cx+dx,TOP_Y+1,cz+dz,random.nextBoolean()?Blocks.ALLIUM:Blocks.LILY_OF_THE_VALLEY);
            }
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
            cx + .5, TOP_Y+8, cz+.5, 90, 10, 3, 10, 0.02);
    }

    private static void path(ServerLevel l,int x,int z,boolean accent) {
        place(l,x,TOP_Y,z,accent ? HaloHaven.SUNGOLD_TILES.get() : HaloHaven.PEARL_BRICKS.get());
        place(l,x,TOP_Y+1,z,Blocks.AIR);
    }
    private static void house(ServerLevel l,int cx,int cz,Random rand,int sx,int sz) {
        int w=6,d=5;
        for(int x=-w;x<=w;x++)for(int z=-d;z<=d;z++){
            place(l,cx+x,TOP_Y,cz+z,HaloHaven.AZURE_PLANKS.get());
            for(int y=TOP_Y+1;y<=TOP_Y+6;y++){
                boolean wall = Math.abs(x)==w || Math.abs(z)==d;
                if(!wall) { place(l,cx+x,y,cz+z,Blocks.AIR); continue; }
                Block b = HaloHaven.PEARL_BRICKS.get();
                if (x == 0 && z == -d && y<=TOP_Y+3) b=Blocks.AIR;
                if (y==TOP_Y+3 && ((Math.abs(x)==w && Math.abs(z)==2) || (Math.abs(z)==d && Math.abs(x)==3)))
                    b=HaloHaven.AURORA_GLASS.get();
                if ((Math.abs(x)==w && Math.abs(z)==d) || (Math.abs(x)==w && y==TOP_Y+6))
                    b=HaloHaven.AZURE_PLANKS.get();
                place(l,cx+x,y,cz+z,b);
            }
        }
        // Architectural stepped pagoda roofs (including a generous overhang).
        for(int tier=0;tier<3;tier++) {
            int rw=w+2-tier,rd=d+2-tier,y=TOP_Y+7+tier;
            for(int x=-rw;x<=rw;x++)for(int z=-rd;z<=rd;z++){
                place(l,cx+x,y,cz+z,Math.abs(x)==rw||Math.abs(z)==rd
                    ?HaloHaven.SUNGOLD_TILES.get():HaloHaven.AZURE_PLANKS.get());
            }
        }
        place(l,cx,TOP_Y+10,cz,HaloHaven.STARSHADE.get());
        for(int z=-d-1;z<=-d+1;z++)for(int x=-1;x<=1;x++)
            place(l,cx+x,TOP_Y+1,cz+z,HaloHaven.PEARL_BRICKS.get());
        place(l,cx+3,TOP_Y+1,cz+1,Blocks.CHEST);
        BlockEntity be=l.getBlockEntity(new BlockPos(cx+3,TOP_Y+1,cz+1));
        if(be instanceof ChestBlockEntity chest){
            chest.setItem(0,new ItemStack(HaloHaven.SKYBERRY.get(),3+rand.nextInt(6)));
            chest.setItem(1,new ItemStack(Items.EMERALD,1+rand.nextInt(3)));
            chest.setItem(2,new ItemStack(HaloHaven.ASCENSION_SIGIL.get()));
        }
        place(l,cx-3,TOP_Y+1,cz+1,Blocks.CRAFTING_TABLE);
        place(l,cx-3,TOP_Y+1,cz+2,HaloHaven.STARSHADE.get());
        spawnVillager(l,cx,TOP_Y+2,cz-3);
    }
    private static void plaza(ServerLevel l,int cx,int cz) {
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)
            if(x*x+z*z<=36)place(l,cx+x,TOP_Y,cz+z,
                x*x+z*z>=27?HaloHaven.SUNGOLD_TILES.get():HaloHaven.PEARL_BRICKS.get());
        // Illuminated fountain, eight water cells and a gold sun above it.
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            if(Math.abs(x)==2||Math.abs(z)==2)place(l,cx+x,TOP_Y+1,cz+z,HaloHaven.SUNGOLD_TILES.get());
            else place(l,cx+x,TOP_Y+1,cz+z,Blocks.WATER);
        }
        for(int y=TOP_Y+1;y<=TOP_Y+5;y++)place(l,cx,y,cz,HaloHaven.PEARL_BRICKS.get());
        place(l,cx,TOP_Y+6,cz,HaloHaven.STARSHADE.get());
        for(int x : new int[]{-5,5})for(int z:new int[]{-5,5}) {
            place(l,cx+x,TOP_Y+1,cz+z,HaloHaven.STARSHADE.get());
        }
    }
    private static void monument(ServerLevel l,int x,int z) {
        for(int y=TOP_Y+1;y<=TOP_Y+5;y++)
            place(l,x,y,z,y==TOP_Y+5?HaloHaven.STARSHADE.get():HaloHaven.PEARL_BRICKS.get());
        for(int d=-1;d<=1;d++) {
            place(l,x+d,TOP_Y,z,HaloHaven.SUNGOLD_TILES.get());
            place(l,x,TOP_Y,z+d,HaloHaven.SUNGOLD_TILES.get());
        }
    }
    private static void tree(ServerLevel l,int x,int z) {
        for(int y=TOP_Y+1;y<=TOP_Y+5;y++)place(l,x,y,z,Blocks.CHERRY_LOG);
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int dy=0;dy<=3;dy++) {
            if(dx*dx+dz*dz+(dy-1)*(dy-1) > 10) continue;
            place(l,x+dx,TOP_Y+5+dy,z+dz,Blocks.CHERRY_LEAVES);
        }
    }
    private static void spawnVillager(ServerLevel l,double x,double y,double z){
        Villager v=EntityType.VILLAGER.create(l);
        if(v!=null){v.moveTo(x+0.5,y,z+0.5,0,0);l.addFreshEntity(v);}
    }
    private static void place(ServerLevel l,int x,int y,int z,Block b) {
        l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);
    }
}
