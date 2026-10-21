package dev.witchhunted.world;

import dev.witchhunted.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class HollowWorld {
    private HollowWorld() {}

    public static void ensure(ServerLevel level) {
        if (level.getBlockState(new BlockPos(0, 1, 0)).is(Blocks.CRYING_OBSIDIAN)) return;

        // Build an isolated sanctuary. The central seal is also an idempotency marker.
        for (int x = -9; x <= 9; x++) {
            for (int z = -9; z <= 9; z++) {
                if (x*x + z*z <= 81) {
                    place(level, x, 1, z, ((x+z)&3)==0
                        ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : Blocks.POLISHED_BLACKSTONE.defaultBlockState());
                    place(level, x, 2, z, Blocks.AIR.defaultBlockState());
                }
            }
        }
        // The eye mosaic observes all exits.
        for (int x = -6; x <= 6; x++) {
            place(level, x, 1, -2, Blocks.AMETHYST_BLOCK.defaultBlockState());
            place(level, x, 1, 2, Blocks.AMETHYST_BLOCK.defaultBlockState());
        }
        for (int z = -2; z <= 2; z++) {
            place(level, -6, 1, z, Blocks.AMETHYST_BLOCK.defaultBlockState());
            place(level, 6, 1, z, Blocks.AMETHYST_BLOCK.defaultBlockState());
        }
        for (int x : new int[]{-6,6}) for (int z : new int[]{-6,6}) {
            for (int y=2; y<=5; y++) place(level,x,y,z,Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            place(level,x,6,z,Blocks.SHROOMLIGHT.defaultBlockState());
        }
        place(level,0,1,0,Blocks.CRYING_OBSIDIAN.defaultBlockState());
        place(level,0,2,0,ModContent.WITCH_HAT.get().defaultBlockState());

        // Straight sightlines are intentional: the oppressive architecture forms a single axis.
        for (int s : new int[]{-1,1}) {
            for (int i=10; i<=39; i++) {
                int x=s*i;
                for (int z=-1;z<=1;z++) {
                    place(level,x,1,z,Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                    place(level,x,2,z,Blocks.AIR.defaultBlockState());
                }
                if (i%7==0) {
                    place(level,x,2,3,Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
                    place(level,x,3,3,Blocks.SHROOMLIGHT.defaultBlockState());
                }
            }
        }
        createCrypt(level, 46, 0, true);
        createCrypt(level, -46, 0, false);

        // Tormented lantern posts make the dead flatland readable at night.
        for (int i=0; i<28; i++) {
            double a=i*Math.PI*2/28.0;
            int x=(int)Math.round(Math.cos(a)*22), z=(int)Math.round(Math.sin(a)*22);
            place(level,x,1,z,Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            place(level,x,2,z,Blocks.CRYING_OBSIDIAN.defaultBlockState());
            place(level,x,3,z,Blocks.SHROOMLIGHT.defaultBlockState());
        }
    }

    private static void createCrypt(ServerLevel level, int cx, int cz, boolean archive) {
        BlockState wall = archive ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                                  : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        for (int x=-7; x<=7; x++) for(int z=-7; z<=7; z++) {
            place(level,cx+x,1,cz+z, ((x+z)&4)==0
                ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState() : wall);
            for(int y=2;y<=8;y++) {
                boolean outer = Math.abs(x)==7 || Math.abs(z)==7;
                place(level,cx+x,y,cz+z,outer ? wall:Blocks.AIR.defaultBlockState());
            }
            place(level,cx+x,9,cz+z,Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        }
        for(int y=2; y<=5; y++) {
            // Both doors face the central path.
            int doorX = archive ? -7 : 7;
            for(int z=-1; z<=1; z++) place(level,cx+doorX,y,cz+z,Blocks.AIR.defaultBlockState());
        }
        for(int y=3;y<=7;y+=4) for(int z : new int[]{-4,4}) {
            place(level,cx, y,cz+z,Blocks.PURPLE_STAINED_GLASS.defaultBlockState());
        }
        for(int x : new int[]{-5,5}) for(int z : new int[]{-5,5}) {
            place(level,cx+x,2,cz+z,Blocks.CRYING_OBSIDIAN.defaultBlockState());
            place(level,cx+x,3,cz+z,Blocks.SHROOMLIGHT.defaultBlockState());
        }
        place(level,cx,2,cz,Blocks.CHEST.defaultBlockState());
        BlockEntity be = level.getBlockEntity(new BlockPos(cx,2,cz));
        if(be instanceof ChestBlockEntity chest) {
            chest.setItem(11,new ItemStack(Items.AMETHYST_SHARD,12));
            chest.setItem(13,archive
                ? new ItemStack(ModContent.OBSERVER_EYE.get())
                : new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
            chest.setItem(15,new ItemStack(Items.PUMPKIN_PIE,6));
            chest.setChanged();
        }
        // Hanging markers on the facade: a mouth without a face.
        for (int z=-4; z<=4; z+=2) {
            place(level,cx,10,cz+z,Blocks.CRYING_OBSIDIAN.defaultBlockState());
        }
        place(level,cx,11,cz,Blocks.SHROOMLIGHT.defaultBlockState());
    }

    private static void place(ServerLevel level,int x,int y,int z,BlockState state) {
        level.setBlock(new BlockPos(x,y,z),state,3);
    }
}
