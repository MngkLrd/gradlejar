package dev.minemods.halohaven;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * Biome-sensitive landmarks, not identical four-house villages in every region.
 * Dawn/Chalk/Canopy retain civilized settlements; Gorge/Choirs/Veil receive
 * standalone traversable ruin islands, different on the silhouette level.
 */
public final class HavenLandmarks {
    private HavenLandmarks(){}

    private static final String[] KEYS={
        "dawn_gardens","chalk_terraces","eternal_canopy",
        "ringing_gorges","forgotten_choirs","lower_veil"
    };
    @SuppressWarnings("unchecked")
    private static final ResourceKey<Biome>[] BIOMES=new ResourceKey[6];
    static{
        for(int n=0;n<6;n++)
            BIOMES[n]=ResourceKey.create(Registries.BIOME,HaloHaven.id(KEYS[n]));
    }

    public static int index(ServerLevel world,int cx,int cz){
        var biome=world.getBiome(new BlockPos(cx,HavenVillages.WALK_Y,cz));
        for(int n=0;n<BIOMES.length;n++)if(biome.is(BIOMES[n]))return n;
        return -1; // existing 1.3 fixed-biome worlds
    }

    public static boolean needsOutpost(ServerLevel world,int cx,int cz){
        return (cx!=0||cz!=0)&&index(world,cx,cz)>=3;
    }

    private static void p(ServerLevel level,int x,int y,int z,Block block){
        level.setBlock(new BlockPos(x,y,z),block.defaultBlockState(),2);
    }

    private static void column(ServerLevel l,int x,int y,int z,int h,Block block){
        for(int k=0;k<h;k++)p(l,x,y+k,z,block);
    }

    public static void generate(ServerLevel l,int cx,int cz,int biome){
        if(biome<3)return;
        Random rng=new Random(l.getSeed()^(cx*71889371L)^(cz*1333877L));
        int floor=HavenVillages.WALK_Y-1;
        Block face=biome==3?HavenBiomeBlocks.RESONANT_SHALE.get():biome==4?HavenBiomeBlocks.CHOIRSTONE.get():HavenBiomeBlocks.UMBRAL_SILT.get();
        Block wall=biome==3?HavenBiomeBlocks.CHALK_STRATA.get():biome==4?HavenBiomeBlocks.CHOIR_TILES.get():HavenBiomeBlocks.VEILSTONE.get();
        int radius=biome==3?33:biome==4?30:29;
        // Rock shell is shaped, thick in the interior and scalloped at the rim.
        // In the gorges, a true void canyon divides two land masses.
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            double d=Math.hypot(dx,dz*1.12);
            double edge=radius-2+1.7*Math.sin((cx+dx)*.19)+1.2*Math.cos((cz+dz)*.23);
            if(d>edge)continue;
            boolean chasm=biome==3&&Math.abs(dx-4*Math.sin(dz*.12))<6&&Math.abs(dz)>2;
            if(chasm)continue;
            int depth=2+(int)(7*Math.max(0,1-d/radius));
            for(int n=1;n<=depth;n++)
                p(l,cx+dx,floor-n,cz+dz,HaloHaven.CLOUDSTONE.get());
            p(l,cx+dx,floor,cz+dz,face);
            if(d>16&&d<radius-4&&rng.nextInt(biome==3?100:biome==4?80:46)==0)
                p(l,cx+dx,floor+1,cz+dz,
                    biome==5?HavenBiomeBlocks.VEIL_LICHEN.get():biome==4?HavenBiomeBlocks.MOURNING_BLOOM.get():HavenBiomeBlocks.GORGE_THISTLE.get());
        }
        // Waystone anchor and the safe central crossing never generate over a void.
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
            p(l,cx+x,floor,cz+z,HaloHaven.PEARL_BRICKS.get());
            if(Math.abs(x)==3||Math.abs(z)==3)
                p(l,cx+x,floor,cz+z,HaloHaven.SUNGOLD_TILES.get());
        }
        p(l,cx,floor+1,cz,HaloHaven.WAYSTONE.get());

        if(biome==3){
            // RINGING GORGES: long slender aqueduct over the cleft,
            // ragged calcite fingers, two watchtowers, no houses.
            for(int x=-22;x<=22;x++)for(int dz=-1;dz<=1;dz++){
                p(l,cx+x,floor,cz+dz,HaloHaven.CELESTIAL_MOSAIC.get());
                if(Math.abs(dz)==1&&Math.abs(x)>4)
                    p(l,cx+x,floor+1,cz+dz,HaloHaven.AZURE_FENCE.get());
            }
            for(int sx:new int[]{-1,1}){
                int xx=cx+sx*21;
                for(int dz:new int[]{-10,10}){
                    int height=9+Math.floorMod(sx*dz+17,5);
                    column(l,xx,floor+1,cz+dz,height,wall);
                    p(l,xx,floor+height+1,cz+dz,HaloHaven.SUNGOLD_TILES.get());
                }
                // A fragment of an elevated sky-arch joins each pair of monoliths.
                for(int dz=-9;dz<=9;dz++)
                    p(l,xx,floor+14-(int)(3*Math.sqrt(1-Math.pow(dz/10d,2))),cz+dz,wall);
            }
        }else if(biome==4){
            // FORGOTTEN CHOIRS: ruined open-air cathedral, an unfinished nave,
            // asymmetric toppled columns and exposed inner sanctum.
            for(int z=-21;z<=22;z++)for(int x=-9;x<=9;x++){
                if(Math.abs(x)==9||Math.abs(z)==21)
                    p(l,cx+x,floor,cz+z,HaloHaven.CELESTIAL_MOSAIC.get());
                if((z+21)%7==0&&Math.abs(x)==8){
                    int height=3+rng.nextInt(9);
                    column(l,cx+x,floor+1,cz+z,height,HaloHaven.GILDED_PILLAR.get());
                    p(l,cx+x,floor+height+1,cz+z,HaloHaven.PEARL_SLAB.get());
                }
            }
            // An intentionally broken rose-window wall.
            for(int x=-6;x<=6;x++)for(int y=1;y<=12;y++){
                int d=x*x+(y-7)*(y-7);
                if(d>=27&&d<=45 && ((x+y)&7)!=0)
                    p(l,cx+x,floor+y,cz-17,wall);
                if(d>=16&&d<=23)
                    p(l,cx+x,floor+y,cz-17,HaloHaven.DAWN_GLASS.get());
            }
            p(l,cx,floor+1,cz-12,HaloHaven.PILGRIM_ALTAR.get());
        }else{
            // LOWER VEIL: nested deep-violet platforms with suspended light
            // beams. A safe first island retains the expected waystone Y=111.
            for(int ring=4;ring<=20;ring+=4){
                for(int a=0;a<48;a++){
                    double theta=a*Math.PI/24;
                    int x=cx+(int)Math.round(ring*Math.cos(theta));
                    int z=cz+(int)Math.round(ring*Math.sin(theta));
                    if(a%5!=0)p(l,x,floor+1,z,(ring%8==0?HavenBiomeBlocks.VEILSTONE.get():HaloHaven.MOONSTONE.get()));
                }
            }
            for(int x:new int[]{-17,17})for(int z:new int[]{-17,17}){
                column(l,cx+x,floor+1,cz+z,7,HaloHaven.GILDED_PILLAR.get());
                p(l,cx+x,floor+8,cz+z,HaloHaven.CLOUD_LANTERN.get());
            }
            // A lower hanging terrace creates a second destination on this site.
            for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++){
                double d=Math.hypot(dx,dz);
                if(d>10.5)continue;
                for(int dep=1;dep<=3;dep++)
                    p(l,cx+dx+28,floor-12-dep,cz+dz+12,HaloHaven.CLOUDSTONE.get());
                p(l,cx+dx+28,floor-12,cz+dz+12,wall);
            }
            for(int x=13;x<=27;x++){
                int step=(x-13)*12/15;
                p(l,cx+x,floor-step,cz+12,HaloHaven.CELESTIAL_MOSAIC.get());
                if(x%3==0)p(l,cx+x,floor-step+1,cz+11,HaloHaven.CLOUD_LANTERN.get());
            }
        }

        // Each hostile district contains discoverable supplies, never forced
        // boss-only loot. Player may return without fighting.
        BlockPos chestPos=new BlockPos(cx+6,floor+1,cz+5);
        p(l,chestPos.getX(),chestPos.getY(),chestPos.getZ(),Blocks.CHEST);
        BlockEntity be=l.getBlockEntity(chestPos);
        if(be instanceof ChestBlockEntity chest){
            chest.setItem(0,new ItemStack(HaloHaven.STARDUST.get(),2+rng.nextInt(3)));
            chest.setItem(1,new ItemStack(HaloHaven.SKYBERRY.get(),2+rng.nextInt(4)));
            chest.setItem(2,new ItemStack(HaloHaven.SUNSHARD.get()));
        }
        // The cliffs, abandoned cathedral and veil now have distinctly
        // dangerous encounters. Peaceful difficulty never creates attackers.
        if(biome>=3 && l.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL){
            int count=biome==3?2:biome==4?3:4;
            for(int i=0;i<count;i++){
                Mob wraith=HaloEntities.DUSK_WRAITH.get().create(l);
                if(wraith!=null){
                    double angle=2*Math.PI*i/count;
                    wraith.moveTo(cx+Math.cos(angle)*14+.5,
                        floor+3,cz+Math.sin(angle)*14+.5,0,0);
                    wraith.setPersistenceRequired();
                    l.addFreshEntity(wraith);
                }
            }
        }
        p(l,cx,93,cz,Blocks.BEDROCK);
    }
}
