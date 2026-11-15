package dev.minemods.halohaven;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * A building vocabulary for Halo Haven: architectural curves, columns,
 * polychrome arcades, stained glass, and carefully framed vistas.
 * No command blocks, server/client-only visuals, or runtime schematics.
 */
public final class HeavenArchitecture {
    private HeavenArchitecture() {}
    private static final int GROUND=110;

    public static void generate(ServerLevel level,int cx,int cz,Random rand) {
        basilica(level,cx+30,cz,rand);
        gardens(level,cx,cz,rand);
        torchesAndArcades(level,cx,cz);
    }
    private static void p(ServerLevel l,int x,int y,int z,Block block) {
        l.setBlock(new BlockPos(x,y,z),block.defaultBlockState(),3);
    }
    private static void stairs(ServerLevel l,int x,int y,int z,Direction outward) {
        l.setBlock(new BlockPos(x,y,z),
            HaloHaven.SUNGOLD_STAIRS.get().defaultBlockState()
                .setValue(StairBlock.FACING,outward)
                .setValue(StairBlock.HALF,Half.BOTTOM),2);
    }
    private static void basilica(ServerLevel l,int cx,int cz,Random rand) {
        // Ovoid elevated podium, arch galleries, and a gilded celestial observatory dome.
        for(int dx=-11;dx<=11;dx++)for(int dz=-12;dz<=12;dz++){
            if((dx*dx/121.0)+(dz*dz/144.0)>1.0)continue;
            p(l,cx+dx,GROUND-1,cz+dz,HaloHaven.CLOUDSTONE.get());
            p(l,cx+dx,GROUND,cz+dz,((Math.abs(dx)%5==0)||(Math.abs(dz)%5==0))?
                HaloHaven.CELESTIAL_MOSAIC.get():HaloHaven.PEARL_BRICKS.get());
        }
        // Eight-sided colonnade. Inner room is open to sky and sunlight.
        for(int dx : new int[]{-9,-5,0,5,9})for(int dz : new int[]{-10,10}) {
            for(int h=1;h<=12;h++)
                p(l,cx+dx,GROUND+h,cz+dz,h%5==0?
                    HaloHaven.PEARL_BRICKS.get():HaloHaven.GILDED_PILLAR.get());
            p(l,cx+dx,GROUND+13,cz+dz,HaloHaven.CLOUD_LANTERN.get());
        }
        for(int dz : new int[]{-6,-2,2,6})for(int dx : new int[]{-9,9}){
            for(int h=1;h<=11;h++)
                p(l,cx+dx,GROUND+h,cz+dz,HaloHaven.GILDED_PILLAR.get());
            p(l,cx+dx,GROUND+12,cz+dz,HaloHaven.CLOUD_LANTERN.get());
        }
        // Column-to-column flying ribs. No walls enclosing the open centre.
        for(int dx=-10;dx<=10;dx++)for(int dz=-11;dz<=11;dz++){
            double radius=Math.sqrt(dx*dx/100.0+dz*dz/121.0);
            if(radius>=0.81&&radius<=1.10) {
                p(l,cx+dx,GROUND+12,cz+dz,HaloHaven.SUNGOLD_TILES.get());
                if(radius>=0.96){
                    Direction outward=Math.abs(dz)>Math.abs(dx)?
                        (dz<0?Direction.NORTH:Direction.SOUTH):
                        (dx<0?Direction.WEST:Direction.EAST);
                    stairs(l,cx+dx,GROUND+13,cz+dz,outward);
                }
            }
        }
        // Rose stained-glass dome with six height steps and a central finial.
        for(int tier=0;tier<=6;tier++){
            int radius=10-tier;
            int height=GROUND+14+tier;
            for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                double distance=Math.hypot(dx,dz);
                if(distance>radius+0.1 || distance<radius-1.15)continue;
                boolean rib=dx%4==0||dz%4==0;
                Block skin=rib?HaloHaven.GILDED_PILLAR.get():
                    ((tier+dx+dz)&1)==0?HaloHaven.AURORA_GLASS.get():HaloHaven.DAWN_GLASS.get();
                p(l,cx+dx,height,cz+dz,skin);
            }
        }
        for(int h=GROUND+20;h<=GROUND+25;h++)p(l,cx,h,cz,HaloHaven.GILDED_PILLAR.get());
        p(l,cx,GROUND+26,cz,HaloHaven.STARSHADE.get());
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
            if(Math.abs(x)+Math.abs(z)==5)
                p(l,cx+x,GROUND,cz+z,HaloHaven.SUNGOLD_TILES.get());
            else if(Math.abs(x)+Math.abs(z)<5)
                p(l,cx+x,GROUND,cz+z,HaloHaven.CELESTIAL_MOSAIC.get());
        }
        // Sun altar, hanging fixtures and aisle seats.
        for(int h=1;h<=4;h++)p(l,cx,GROUND+h,cz,HaloHaven.GILDED_PILLAR.get());
        p(l,cx,GROUND+5,cz,HaloHaven.STARSHADE.get());
        for(int z=-8;z<=8;z+=4)for(int x:new int[]{-7,7}){
            p(l,cx+x,GROUND+1,cz+z,HaloHaven.PEARL_SLAB.get());
            p(l,cx+x,GROUND+2,cz+z,HaloHaven.CLOUD_LANTERN.get());
        }
        p(l,cx+8,GROUND+1,cz+5,Blocks.CHEST);
        BlockEntity entity=l.getBlockEntity(new BlockPos(cx+8,GROUND+1,cz+5));
        if(entity instanceof ChestBlockEntity chest){
            chest.setItem(0,new ItemStack(HaloHaven.AETHER_FEATHER.get()));
            chest.setItem(1,new ItemStack(HaloHaven.SUNSHARD.get(),2));
            chest.setItem(2,new ItemStack(HaloHaven.STARDUST.get(),3+rand.nextInt(4)));
            chest.setItem(3,new ItemStack(Items.BOOK,2));
        }
        // Golden boulevard leading east from the original circular village plaza.
        for(int x=cx-19;x<=cx-11;x++)for(int z=-2;z<=2;z++)
            p(l,x,GROUND,cz+z,(x+z)%5==0?HaloHaven.SUNGOLD_TILES.get():HaloHaven.CELESTIAL_MOSAIC.get());
    }
    private static void gardens(ServerLevel l,int cx,int cz,Random rand) {
        // Satellite terraces, very different silhouettes from houses or ruins.
        for(int index=0;index<2;index++){
            int x=cx+(index==0?-35:35);
            int z=cz+(index==0?25:-25);
            for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++){
                double d=Math.hypot(dx,dz);
                if(d>8.5)continue;
                int y=GROUND-(d>6?2:d>3.5?1:0);
                p(l,x+dx,y,z+dz,d>7?HaloHaven.PEARL_BRICKS.get():HaloHaven.SKYGRASS.get());
                for(int down=y-1;down>=GROUND-7+(int)(d/4);down--)
                    p(l,x+dx,down,z+dz,HaloHaven.CLOUDSTONE.get());
                if(d>2.5&&rand.nextInt(8)==0)
                    p(l,x+dx,y+1,z+dz,HaloHaven.CELESTIAL_BLOOM.get());
            }
            for(int dx:new int[]{-7,7})for(int dz:new int[]{-5,5}){
                p(l,x+dx,GROUND+1,z+dz,HaloHaven.AZURE_FENCE.get());
                p(l,x+dx,GROUND+2,z+dz,HaloHaven.CLOUD_LANTERN.get());
            }
            // A distinctive tree with its own foliage, glowing fruitlike lanterns.
            int trunk=5;
            for(int h=1;h<=trunk;h++)p(l,x,GROUND+h,z,HavenVillages.nativeWood("heartwood_log"));
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=-2;dy<=3;dy++){
                if(dx*dx+dz*dz+dy*dy>20)continue;
                BlockPos at=new BlockPos(x+dx,GROUND+trunk+dy,z+dz);
                l.setBlock(at,HaloHaven.SKY_LEAVES.get().defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT,true),2);
            }
            for(int dx : new int[]{-4,4})for(int dz:new int[]{-3,3})
                p(l,x+dx,GROUND+4,z+dz,HaloHaven.CLOUD_LANTERN.get());
        }
    }
    private static void torchesAndArcades(ServerLevel l,int cx,int cz) {
        // Tall paired ceremonial lamp posts create hierarchy and perspective.
        for(int x : new int[]{-13,13})for(int z : new int[]{-8,8}){
            for(int h=1;h<=5;h++)
                p(l,cx+x,GROUND+h,cz+z,HaloHaven.GILDED_PILLAR.get());
            p(l,cx+x,GROUND+6,cz+z,HaloHaven.STARSHADE.get());
        }
        // Balustraded overlook, facing out from the fountain.
        for(int z=-6;z<=6;z++){
            p(l,cx-14,GROUND,cz+z,HaloHaven.PEARL_SLAB.get());
            p(l,cx-15,GROUND+1,cz+z,z%4==0?HaloHaven.CLOUD_LANTERN.get():HaloHaven.AZURE_FENCE.get());
        }
    }
}
