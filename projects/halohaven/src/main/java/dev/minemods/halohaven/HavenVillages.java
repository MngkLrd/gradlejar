package dev.minemods.halohaven;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.Half;
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
        // The Aether Feather is functional equipment, not a decorative icon.
        // Offhand support allows mining while descending over the void.
        if(p.tickCount%6==0 && !p.onGround() && p.getDeltaMovement().y<0
            && (p.getMainHandItem().is(HaloHaven.AETHER_FEATHER.get())
                || p.getOffhandItem().is(HaloHaven.AETHER_FEATHER.get()))) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 24, 0, true, false));
        }
        if (!p.level().dimension().equals(HaloHaven.HEAVEN)) return;
        // Low-density ambient magic for the four distinct celestial silhouettes.
        if(p.tickCount%32==0) {
            ServerLevel world=(ServerLevel)p.level();
            for(Skyfolk npc : world.getEntitiesOfClass(Skyfolk.class,p.getBoundingBox().inflate(20))) {
                world.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    npc.getX(),npc.getY()+2.3,npc.getZ(),2,0.22,0.08,0.22,0.005);
            }
            for(Allay lumen : world.getEntitiesOfClass(Allay.class,p.getBoundingBox().inflate(18),
                a->a.getType()==HaloEntities.LUMEN.get())) {
                world.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    lumen.getX(),lumen.getY()+0.3,lumen.getZ(),2,0.18,0.25,0.18,0.014);
            }
            for(Vex shade : world.getEntitiesOfClass(Vex.class,p.getBoundingBox().inflate(18),
                a->a.getType()==HaloEntities.DUSK_WRAITH.get())) {
                world.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    shade.getX(),shade.getY()+0.4,shade.getZ(),2,0.22,0.3,0.22,0.013);
            }
        }
        // The Dawn Guardian's restorative aura makes a village a useful refuge.
        if(p.tickCount % 140 == 0) {
            boolean protectedHere=!p.level().getEntitiesOfClass(IronGolem.class,
                p.getBoundingBox().inflate(9), g -> g.getType()==HaloEntities.DAWN_GUARDIAN.get()).isEmpty();
            if(protectedHere) p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 180, 0, true, false));
        }
        if(p.tickCount % 100 != 0) return;
        // Existing Halo Haven 1.0 settlements migrate vanilla villagers to skyfolk
        // without erasing player builds or regenerating the village.
        if(p.tickCount % 200 == 0) {
            for(Villager old : p.level().getEntitiesOfClass(Villager.class,
                    p.getBoundingBox().inflate(40), v -> v.getType()==EntityType.VILLAGER)){
                Skyfolk sky=HaloEntities.SKYFOLK.get().create((ServerLevel)p.level());
                if(sky==null)continue;
                sky.moveTo(old.getX(),old.getY(),old.getZ(),old.getYRot(),old.getXRot());
                sky.populateSkyTrades();
                sky.setPersistenceRequired();
                p.level().addFreshEntity(sky);
                old.discard();
            }
        }
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
        // Star ore appears below the grass within the island shell.
        for (int n=0;n<230;n++) {
            int dx=random.nextInt(77)-38, dz=random.nextInt(77)-38;
            if(dx*dx+dz*dz>36*36)continue;
            int y=TOP_Y-1-random.nextInt(4);
            if(level.getBlockState(new BlockPos(cx+dx,y,cz+dz)).is(HaloHaven.CLOUDSTONE.get()))
                place(level,cx+dx,y,cz+dz,HaloHaven.STAR_ORE.get());
        }
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
        // Exploration loop: star-wraith ruins, an observatory, a suspended sanctuary.
        for (int x : new int[]{-1,1}) {
            ruin(level,cx+x*62,cz+x*24,random);
            skybridge(level,cx+x*35,cz+x*13,cx+x*62,cz+x*24);
        }
        observatory(level,cx-33,cz-8);
        sanctuary(level,cx,cz+34);
        HeavenArchitecture.generate(level,cx,cz,random);
        for (int x : new int[]{-36,36}) for (int z : new int[]{-9,9}) {
            monument(level,cx+x,cz+z);
        }
        for (int dx=-30;dx<=30;dx++) for (int dz=-30;dz<=30;dz++) {
            if (Math.abs(dx)<4 || Math.abs(dz)<4 || Math.abs(dx)>28 || Math.abs(dz)>28) continue;
            if (random.nextInt(160)==0) {
                place(level,cx+dx,TOP_Y+1,cz+dz,random.nextBoolean()?Blocks.ALLIUM:Blocks.LILY_OF_THE_VALLEY);
            }
        }
        // Small ambient wildlife; spawned exactly once with the village marker.
        for(int n=0;n<6;n++){
            double angle=2*Math.PI*n/6.0;
            int dx=(int)(Math.cos(angle)*33),dz=(int)(Math.sin(angle)*33);
            spawn(level,HaloEntities.CLOUDHARE.get(),cx+dx,TOP_Y+2,cz+dz);
        }
        for(int n=0;n<3;n++){
            spawn(level,HaloEntities.LUMEN.get(),cx+5+n*4,TOP_Y+5,cz-10);
        }
        spawn(level,HaloEntities.DAWN_GUARDIAN.get(),cx-5,TOP_Y+2,cz+5);
        // Persist only after ALL components have been built.
        place(level,cx,93,cz,Blocks.BEDROCK);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
            cx + .5, TOP_Y+8, cz+.5, 90, 10, 3, 10, 0.02);
    }

    private static void path(ServerLevel l,int x,int z,boolean accent) {
        place(l,x,TOP_Y,z,accent ? HaloHaven.SUNGOLD_TILES.get() : HaloHaven.PEARL_BRICKS.get());
        place(l,x,TOP_Y+1,z,Blocks.AIR);
    }
    private static void house(ServerLevel l,int cx,int cz,Random rand,int sx,int sz) {
        int w=5+rand.nextInt(2),d=4+rand.nextInt(2);
        boolean moonHouse=rand.nextInt(5)==0;
        for(int x=-w;x<=w;x++)for(int z=-d;z<=d;z++){
            place(l,cx+x,TOP_Y,cz+z,HaloHaven.AZURE_PLANKS.get());
            for(int y=TOP_Y+1;y<=TOP_Y+6;y++){
                boolean wall = Math.abs(x)==w || Math.abs(z)==d;
                if(!wall) { place(l,cx+x,y,cz+z,Blocks.AIR); continue; }
                Block b = moonHouse ? HaloHaven.MOONSTONE.get() : HaloHaven.PEARL_BRICKS.get();
                if (x == 0 && z == -d && y<=TOP_Y+3) b=Blocks.AIR;
                if (y==TOP_Y+3 && ((Math.abs(x)==w && Math.abs(z)==2) || (Math.abs(z)==d && Math.abs(x)==3)))
                    b=moonHouse ? HaloHaven.DAWN_GLASS.get() : HaloHaven.AURORA_GLASS.get();
                if ((Math.abs(x)==w && Math.abs(z)==d) || (Math.abs(x)==w && y==TOP_Y+6))
                    b=HaloHaven.AZURE_PLANKS.get();
                place(l,cx+x,y,cz+z,b);
            }
        }
        // Four tier roof: real shaped stairs and warm glazed eaves instead of full cubes.
        for(int tier=0;tier<4;tier++){
            int rw=w+2-tier,rd=d+2-tier,y=TOP_Y+7+tier;
            for(int x=-rw;x<=rw;x++)for(int z=-rd;z<=rd;z++){
                if(Math.abs(x)==rw||Math.abs(z)==rd){
                    Direction direction=Math.abs(z)==rd?
                        (z<0?Direction.NORTH:Direction.SOUTH):
                        (x<0?Direction.WEST:Direction.EAST);
                    l.setBlock(new BlockPos(cx+x,y,cz+z),
                        HaloHaven.SUNGOLD_STAIRS.get().defaultBlockState()
                            .setValue(StairBlock.FACING,direction)
                            .setValue(StairBlock.HALF,Half.BOTTOM),2);
                } else if(tier==3 || x%3==0 || z%3==0){
                    place(l,cx+x,y,cz+z,moonHouse?HaloHaven.MOONSTONE.get():HaloHaven.AZURE_PLANKS.get());
                }
            }
        }
        place(l,cx,TOP_Y+11,cz,HaloHaven.STARSHADE.get());
        for(int x=-w-1;x<=w+1;x++){
            if(Math.abs(x)<2)continue;
            place(l,cx+x,TOP_Y+1,cz-d-2,HaloHaven.PEARL_SLAB.get());
            place(l,cx+x,TOP_Y+2,cz-d-2,HaloHaven.AZURE_FENCE.get());
        }
        for(int x:new int[]{-w,w})for(int z:new int[]{-d,d}){
            for(int y=TOP_Y+1;y<=TOP_Y+6;y++)
                place(l,cx+x,y,cz+z,HaloHaven.GILDED_PILLAR.get());
            place(l,cx+x,TOP_Y+7,cz+z,HaloHaven.CLOUD_LANTERN.get());
        }
        for(int z=-d-1;z<=-d+1;z++)for(int x=-1;x<=1;x++)
            place(l,cx+x,TOP_Y+1,cz+z,HaloHaven.PEARL_BRICKS.get());
        place(l,cx+3,TOP_Y+1,cz+1,Blocks.CHEST);
        BlockEntity be=l.getBlockEntity(new BlockPos(cx+3,TOP_Y+1,cz+1));
        if(be instanceof ChestBlockEntity chest){
            chest.setItem(0,new ItemStack(HaloHaven.SKYBERRY.get(),3+rand.nextInt(6)));
            chest.setItem(1,new ItemStack(Items.EMERALD,1+rand.nextInt(3)));
            chest.setItem(2,new ItemStack(HaloHaven.STARDUST.get(),1+rand.nextInt(4)));
            if(rand.nextInt(5)==0)chest.setItem(3,new ItemStack(HaloHaven.AETHER_TONIC.get()));
        }
        place(l,cx-3,TOP_Y+1,cz+1,Blocks.CRAFTING_TABLE);
        place(l,cx-3,TOP_Y+1,cz+2,HaloHaven.STARSHADE.get());
        spawnVillager(l,cx,TOP_Y+2,cz-3);
        spawnVillager(l,cx+2,TOP_Y+2,cz-3);
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
            place(l,cx+x,TOP_Y+1,cz+z,HaloHaven.CLOUD_LANTERN.get());
        }
        // Cross-island waystone; right-click to go forward, shift-click backwards.
        for(int x=8;x<=12;x++)for(int z=-2;z<=2;z++)
            place(l,cx+x,TOP_Y,cz+z,HaloHaven.CELESTIAL_MOSAIC.get());
        for(int y=TOP_Y+1;y<=TOP_Y+3;y++)place(l,cx+10,y,cz,HaloHaven.GILDED_PILLAR.get());
        place(l,cx+10,TOP_Y+4,cz,HaloHaven.WAYSTONE.get());
        for(int z : new int[]{-2,2})
            place(l,cx+10,TOP_Y+1,cz+z,HaloHaven.STARSHADE.get());
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
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=-1;dy<=3;dy++) {
            if(dx*dx+dz*dz+(dy-1)*(dy-1)>18)continue;
            l.setBlock(new BlockPos(x+dx,TOP_Y+5+dy,z+dz),
                HaloHaven.SKY_LEAVES.get().defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT,true),2);
        }
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
            if(Math.abs(dx)+Math.abs(dz)==4)
                place(l,x+dx,TOP_Y+1,z+dz,HaloHaven.CELESTIAL_BLOOM.get());
        }
    }
    private static void spawnVillager(ServerLevel l,double x,double y,double z){
        Skyfolk v=HaloEntities.SKYFOLK.get().create(l);
        if(v!=null){
            v.moveTo(x+0.5,y,z+0.5,0,0);
            v.populateSkyTrades();
            v.setPersistenceRequired();
            l.addFreshEntity(v);
        }
    }

    private static <T extends Mob> void spawn(ServerLevel l,EntityType<T> type,double x,double y,double z) {
        T entity=type.create(l);
        if(entity==null)return;
        entity.moveTo(x+0.5,y,z+0.5,0,0);
        entity.setPersistenceRequired();
        l.addFreshEntity(entity);
    }

    private static void skybridge(ServerLevel l,int x1,int z1,int x2,int z2) {
        int steps=Math.max(Math.abs(x2-x1),Math.abs(z2-z1))*2;
        double dd=Math.hypot(x2-x1,z2-z1);
        double perpX=-(z2-z1)/dd,perpZ=(x2-x1)/dd;
        for(int s=0;s<=steps;s++){
            double t=s/(double)steps;
            double px=x1+(x2-x1)*t,pz=z1+(z2-z1)*t;
            for(int lane=-1;lane<=1;lane++){
                int x=(int)Math.round(px+perpX*lane);
                int z=(int)Math.round(pz+perpZ*lane);
                place(l,x,TOP_Y,z, s%10==0 ?
                    HaloHaven.SUNGOLD_TILES.get():HaloHaven.CELESTIAL_MOSAIC.get());
                place(l,x,TOP_Y-1,z,HaloHaven.GILDED_PILLAR.get());
                for(int h=1;h<=3;h++)place(l,x,TOP_Y+h,z,Blocks.AIR);
            }
            // Two side glass balustrades, with lamps at intervals.
            if(s%2==0){
                for(int side : new int[]{-2,2}){
                    int x=(int)Math.round(px+perpX*side),z=(int)Math.round(pz+perpZ*side);
                    place(l,x,TOP_Y,z,HaloHaven.PEARL_BRICKS.get());
                    place(l,x,TOP_Y+1,z,s%16==0 ?
                        HaloHaven.CLOUD_LANTERN.get():HaloHaven.AURORA_GLASS.get());
                }
            }
        }
    }

    private static void observatory(ServerLevel l,int cx,int cz){
        // Open gazebo with a vaulted canopy and functional telescope (spyglass loot).
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
            if(dx*dx+dz*dz>36)continue;
            place(l,cx+dx,TOP_Y,cz+dz,HaloHaven.CELESTIAL_MOSAIC.get());
        }
        for(int dx : new int[]{-4,4})for(int dz : new int[]{-4,4}){
            for(int y=TOP_Y+1;y<=TOP_Y+7;y++)
                place(l,cx+dx,y,cz+dz,HaloHaven.GILDED_PILLAR.get());
            place(l,cx+dx,TOP_Y+8,cz+dz,HaloHaven.CLOUD_LANTERN.get());
        }
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
            if(dx*dx+dz*dz<45 && dx*dx+dz*dz>22)continue;
            if(dx*dx+dz*dz<=22)
                place(l,cx+dx,TOP_Y+8,cz+dz,HaloHaven.DAWN_GLASS.get());
        }
        place(l,cx,TOP_Y+1,cz,Blocks.LECTERN);
        place(l,cx+1,TOP_Y+1,cz,Blocks.CHEST);
        BlockEntity entity=l.getBlockEntity(new BlockPos(cx+1,TOP_Y+1,cz));
        if(entity instanceof ChestBlockEntity chest){
            chest.setItem(0,new ItemStack(Items.SPYGLASS));
            chest.setItem(1,new ItemStack(HaloHaven.SKY_COMPASS.get()));
            chest.setItem(2,new ItemStack(HaloHaven.STARDUST.get(),4));
        }
    }

    private static void sanctuary(ServerLevel l,int cx,int cz) {
        // Sacred garden: pale stone rings, sky berries and lanterns.
        for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++){
            double d=Math.sqrt(dx*dx+dz*dz);
            if(d<7.8&&d>6.3)place(l,cx+dx,TOP_Y,cz+dz,HaloHaven.SUNGOLD_TILES.get());
            if(d<5.9)place(l,cx+dx,TOP_Y,cz+dz,HaloHaven.SKYGRASS.get());
            if(d>4.5&&d<5.6 && (dx+dz)%3==0)
                place(l,cx+dx,TOP_Y+1,cz+dz,Blocks.LILY_OF_THE_VALLEY);
        }
        place(l,cx,TOP_Y,cz,HaloHaven.CELESTIAL_MOSAIC.get());
        place(l,cx,TOP_Y+1,cz,HaloHaven.PILGRIM_ALTAR.get());
        place(l,cx,TOP_Y+5,cz,HaloHaven.CLOUD_LANTERN.get());
        for(int dx : new int[]{-6,6})for(int dz:new int[]{-6,6}){
            for(int y=TOP_Y+1;y<=TOP_Y+3;y++)
                place(l,cx+dx,y,cz+dz,HaloHaven.PEARL_BRICKS.get());
            place(l,cx+dx,TOP_Y+4,cz+dz,HaloHaven.CLOUD_LANTERN.get());
        }
    }

    private static void ruin(ServerLevel l,int cx,int cz,Random rand){
        // Independent satellite island connected to the village by a suspended bridge.
        for(int dx=-15;dx<=15;dx++)for(int dz=-15;dz<=15;dz++){
            int r2=dx*dx+dz*dz;
            if(r2>210)continue;
            int depth=2+(int)(4*Math.max(0,1-r2/210.0));
            for(int y=TOP_Y-depth;y<TOP_Y;y++)
                place(l,cx+dx,y,cz+dz,HaloHaven.MOONSTONE.get());
            place(l,cx+dx,TOP_Y,cz+dz,rand.nextInt(8)==0?
                HaloHaven.CELESTIAL_MOSAIC.get():HaloHaven.CLOUDSTONE.get());
            if(r2>100&&rand.nextInt(13)==0)
                place(l,cx+dx,TOP_Y+1,cz+dz,Blocks.SHORT_GRASS);
        }
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)
            if((Math.abs(x)==6||Math.abs(z)==6) && rand.nextInt(6)!=0)
                for(int y=TOP_Y+1;y<=TOP_Y+2+rand.nextInt(5);y++)
                    place(l,cx+x,y,cz+z,HaloHaven.MOONSTONE.get());
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
            if(x*x+z*z<10)
                place(l,cx+x,TOP_Y,cz+z,HaloHaven.CELESTIAL_MOSAIC.get());
        }
        for(int y=TOP_Y+1;y<=TOP_Y+7;y++)
            place(l,cx,y,cz,HaloHaven.GILDED_PILLAR.get());
        place(l,cx,TOP_Y+8,cz,HaloHaven.STARSHADE.get());
        place(l,cx+3,TOP_Y+1,cz+2,Blocks.CHEST);
        BlockEntity entity=l.getBlockEntity(new BlockPos(cx+3,TOP_Y+1,cz+2));
        if(entity instanceof ChestBlockEntity chest){
            chest.setItem(0,new ItemStack(HaloHaven.STARDUST.get(),4+rand.nextInt(4)));
            chest.setItem(1,new ItemStack(HaloHaven.SUNSHARD.get(),1+rand.nextInt(2)));
            if(rand.nextInt(3)==0)chest.setItem(2,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
        }
        for(int n=0;n<2;n++)
            spawn(l,HaloEntities.DUSK_WRAITH.get(),cx+5*n-3,TOP_Y+4,cz-4);
        // Fallen astral court: roughly half the ruined islets are guarded
        // by a real two-phase mini-boss (never on the central safe village).
        if(rand.nextInt(2)==0){
            spawn(l,HaloEntities.ECLIPSE_REVENANT.get(),cx,TOP_Y+5,cz-1);
        }
    }
    private static void place(ServerLevel l,int x,int y,int z,Block b) {
        l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);
    }
}
