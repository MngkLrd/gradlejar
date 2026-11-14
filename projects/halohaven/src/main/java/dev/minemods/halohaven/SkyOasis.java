package dev.minemods.halohaven;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Makes a compact, self-supporting sky oasis in the OVERWORLD.
 * Uses only sky-empty coordinates; never rewrites existing player builds.
 * Stages 15 concentric island rings over 15 ticks, decorates it in the next
 * 12 ticks, and flies its owner upward with a visible server particle stream.
 * A central quartz/amethyst altar is ready for a hand-placed bell and Sigil.
 */
public final class SkyOasis {
    private SkyOasis(){}

    private static final int ISLAND_RADIUS=14;
    private static final Map<UUID,Ascent> ASCENTS=new HashMap<>();

    private static final class Ascent {
        final UUID player;
        final ServerLevel level;
        final int x,z,top;
        int tick;
        boolean landed;
        Ascent(ServerPlayer p,int cx,int cz,int y){
            player=p.getUUID();
            level=p.serverLevel();
            x=cx;z=cz;top=y;
        }
    }

    public static boolean begin(ServerPlayer player){
        ServerLevel level=player.serverLevel();
        if(level.dimension().equals(HaloHaven.HEAVEN)){
            player.displayClientMessage(Component.translatable("message.halohaven.skyseed_overworld"),true);
            return false;
        }
        if(ASCENTS.containsKey(player.getUUID())){
            player.displayClientMessage(Component.translatable("message.halohaven.skyseed_busy"),true);
            return false;
        }
        if(!level.canSeeSky(player.blockPosition().above())){
            player.displayClientMessage(Component.translatable("message.halohaven.skyseed_open_sky"),true);
            return false;
        }
        int floor=Math.max(183,player.getBlockY()+38);
        if(floor+19>=level.getMaxBuildHeight()){
            player.displayClientMessage(Component.translatable("message.halohaven.skyseed_too_high"),true);
            return false;
        }
        // The island grows 19 blocks EAST of the ascent column.
        // Player rises in clear air alongside it instead of slamming into
        // its solid underside as the rings are created.
        int x=player.getBlockX()+19,z=player.getBlockZ();
        if(!clear(level,x,z,floor)){
            player.displayClientMessage(Component.translatable("message.halohaven.skyseed_obstructed"),true);
            return false;
        }
        Ascent ascent=new Ascent(player,x,z,floor);
        ASCENTS.put(player.getUUID(),ascent);
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,20*18,0,false,false));
        player.setDeltaMovement(0,1.3,0);
        player.hasImpulse=true;
        player.hurtMarked=true;
        player.fallDistance=0;
        level.playSound(null,player.blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.PLAYERS,1.4f,1.7f);
        level.sendParticles(ParticleTypes.CLOUD,player.getX(),player.getY()+0.7,player.getZ(),
            65,1.2,0.3,1.2,0.07);
        player.displayClientMessage(Component.translatable("message.halohaven.skyseed_flight"),false);
        return true;
    }

    private static boolean clear(ServerLevel level,int cx,int cz,int floor){
        // Only reject actual solids around the build envelope.
        // In vanilla Overworld at Y=183 most of this is free space.
        for(int dx=-ISLAND_RADIUS;dx<=ISLAND_RADIUS;dx+=2)
        for(int dz=-ISLAND_RADIUS;dz<=ISLAND_RADIUS;dz+=2){
            if(dx*dx+dz*dz>(ISLAND_RADIUS+1)*(ISLAND_RADIUS+1))continue;
            for(int dy=-7;dy<=17;dy+=3){
                if(!level.isEmptyBlock(new BlockPos(cx+dx,floor+dy,cz+dz)))return false;
            }
        }
        // Protect the exact central ritual volume from occupied blocks too.
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)
            for(int y=floor;y<=floor+7;y++)
                if(!level.isEmptyBlock(new BlockPos(cx+dx,y,cz+dz)))return false;
        return true;
    }

    public static void onLevelTick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        if(ASCENTS.isEmpty())return;
        Iterator<Map.Entry<UUID,Ascent>> it=ASCENTS.entrySet().iterator();
        while(it.hasNext()){
            Ascent a=it.next().getValue();
            if(a.level!=level)continue;
            if(a.tick<15)buildRing(a,a.tick);
            if(a.tick==15)prepareAltar(a);
            if(a.tick==19)makeGarden(a);
            if(a.tick>=15 && a.tick<=24 && a.tick%3==0){
                level.sendParticles(ParticleTypes.END_ROD,a.x+.5,a.top+2,a.z+.5,
                    30,5,1.2,5,0.08);
            }

            ServerPlayer player=level.getServer().getPlayerList().getPlayer(a.player);
            if(player!=null && player.level()==level && !a.landed){
                if(player.getY()>=a.top+0.2 && a.tick>=21){
                    // Shift gently onto the west-side landing court only
                    // after rising above the entire solid island shell.
                    player.teleportTo(level,a.x-10.5,a.top+1.1,a.z+0.5,
                        player.getYRot(),player.getXRot());
                    player.setDeltaMovement(0,0,0);
                    player.fallDistance=0;
                    a.landed=true;
                    player.displayClientMessage(
                        Component.translatable("message.halohaven.skyseed_ready"),false);
                    level.playSound(null,new BlockPos(a.x,a.top+1,a.z),
                        SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.BLOCKS,1.5f,1.3f);
                    level.sendParticles(ParticleTypes.END_ROD,a.x+.5,a.top+3,a.z+.5,
                        80,4,2,4,0.09);
                }else{
                    double remaining=a.top+1-player.getY();
                    player.setDeltaMovement(0,Math.min(1.65,Math.max(0.55,remaining*0.25)),0);
                    player.hasImpulse=true;
                    player.hurtMarked=true;
                    player.fallDistance=0;
                    if(a.tick%3==0){
                        level.sendParticles(ParticleTypes.CLOUD,player.getX(),player.getY()-0.6,
                            player.getZ(),12,0.5,0.2,0.5,0.03);
                        level.sendParticles(ParticleTypes.END_ROD,player.getX(),player.getY()+0.5,
                            player.getZ(),6,0.7,0.8,0.7,0.04);
                    }
                    if(a.tick%20==0)
                        level.playSound(null,player.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,
                            SoundSource.PLAYERS,0.6f,1.1f+a.tick*0.003f);
                }
            }
            a.tick++;
            if((a.landed && a.tick>30)||a.tick>200)it.remove();
        }
    }

    private static void block(Ascent a,int dx,int y,int dz,Block b){
        a.level.setBlock(new BlockPos(a.x+dx,y,a.z+dz),b.defaultBlockState(),2);
    }

    private static void buildRing(Ascent a,int ring){
        // Stable scalloped edge, thick taper beneath the platform.
        for(int dx=-ring-1;dx<=ring+1;dx++)for(int dz=-ring-1;dz<=ring+1;dz++){
            double d=Math.hypot(dx,dz);
            if((int)Math.round(d)!=ring)continue;
            if(d>13.5+0.6*Math.sin(dx*.8+dz*.3))continue;
            int depth=2+(int)((ISLAND_RADIUS-d)*0.48);
            for(int dy=1;dy<=depth;dy++){
                // Gives the otherwise flat ground a curled cloudstone underside.
                if(dy==depth&&((dx*17+dz*11)&7)==0)continue;
                block(a,dx,a.top-dy,dz,HaloHaven.CLOUDSTONE.get());
            }
            block(a,dx,a.top,dz,HaloHaven.SKYGRASS.get());
            if(a.tick%2==0)
                a.level.sendParticles(ParticleTypes.CLOUD,a.x+dx+0.5,a.top+0.65,
                    a.z+dz+0.5,2,0.3,0.1,0.3,0.02);
        }
    }

    private static void prepareAltar(Ascent a){
        // A completed lower altar, deliberately missing the bell.
        // Bell Y = top+2 (> 170), so the original Sigil works unchanged.
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
            Block b=(Math.abs(dx)==3||Math.abs(dz)==3)?
                HaloHaven.SUNGOLD_TILES.get():HaloHaven.PEARL_BRICKS.get();
            if(Math.abs(dx)==1 && Math.abs(dz)==1)b=Blocks.AMETHYST_BLOCK;
            if(dx==0&&dz==0)b=Blocks.QUARTZ_BLOCK;
            block(a,dx,a.top+1,dz,b);
        }
        // One stair leading up from the safe landing area.
        for(int z=-1;z<=1;z++){
            a.level.setBlock(new BlockPos(a.x+4,a.top,a.z+z),
                HaloHaven.PEARL_STAIRS.get().defaultBlockState()
                    .setValue(StairBlock.FACING,Direction.EAST)
                    .setValue(StairBlock.HALF,Half.BOTTOM),2);
        }
        for(int i=-1;i<=1;i++){
            block(a,5,a.top,i,HaloHaven.CELESTIAL_MOSAIC.get());
            block(a,6,a.top,i,HaloHaven.CELESTIAL_MOSAIC.get());
        }
        a.level.sendParticles(ParticleTypes.END_ROD,a.x+.5,a.top+1.8,a.z+.5,
            85,2.3,0.6,2.3,0.07);
    }

    private static void makeGarden(Ascent a){
        // Four golden lanterns around the portal rather than a visual obstruction
        // above the bell. Every decorative object stays away from the sky-opening.
        for(int dx:new int[]{-5,5})for(int dz:new int[]{-5,5}){
            block(a,dx,a.top+1,dz,HaloHaven.GILDED_PILLAR.get());
            block(a,dx,a.top+2,dz,HaloHaven.CLOUD_LANTERN.get());
        }
        for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++){
            double d=Math.hypot(dx,dz);
            if(d<5||d>12)continue;
            int hash=Math.floorMod(dx*37+dz*71+dx*dz*3,31);
            if(hash==0||hash==3){
                block(a,dx,a.top+1,dz,HaloHaven.CELESTIAL_BLOOM.get());
            }
        }
        // Little enclosed water court to the north west of the altar.
        for(int dx=-9;dx<=-5;dx++)for(int dz=4;dz<=8;dz++){
            boolean wall=dx==-9||dx==-5||dz==4||dz==8;
            block(a,dx,a.top+1,dz,wall?HaloHaven.PEARL_BRICKS.get():Blocks.WATER);
        }
        // A miniature sky orchard with a real airy crown.
        for(int sx:new int[]{-10,9}){
            int zz=sx<0?-7:7;
            for(int dy=1;dy<=4;dy++)block(a,sx,a.top+dy,zz,Blocks.CHERRY_LOG);
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int dy=-1;dy<=2;dy++){
                if(dx*dx+dz*dz+(dy*dy)>11)continue;
                BlockPos pos=new BlockPos(a.x+sx+dx,a.top+4+dy,a.z+zz+dz);
                a.level.setBlock(pos,HaloHaven.SKY_LEAVES.get().defaultBlockState()
                    .setValue(LeavesBlock.PERSISTENT,true),2);
            }
        }
        for(int i=0;i<16;i++){
            double angle=i*Math.PI*2.0/16.0;
            int xx=(int)Math.round(12.8*Math.cos(angle));
            int zz=(int)Math.round(12.8*Math.sin(angle));
            block(a,xx,a.top+1,zz,HaloHaven.AZURE_FENCE.get());
        }
    }
}
