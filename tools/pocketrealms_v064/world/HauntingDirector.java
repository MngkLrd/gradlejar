package com.kirill.pocketrealms.world;

import com.kirill.pocketrealms.entity.VeilHound;
import com.kirill.pocketrealms.entity.VeilGhost;
import com.kirill.pocketrealms.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Per-sector budgets and intentional, observable scare encounters. */
public final class HauntingDirector {
    private HauntingDirector(){}

    public static void tick(MinecraftServer server) {
        ServerLevel level=server.getLevel(ModDimensions.POCKET_DIMENSION);
        if(level==null||level.players().isEmpty()||level.getGameTime()%20!=0)return;
        long now=level.getGameTime();
        for(PocketRunEntry run:PocketRunData.get(server).allRuns()){
            if(!run.generated||run.completed||run.retiring||run.spawnPos==null)continue;
            ServerPlayer player=level.players().stream()
                    .filter(p->p.getUUID().equals(run.owner)&&p.isAlive()&&!p.isCreative()
                            &&!p.isSpectator()&&run.contains(p.blockPosition())).findFirst().orElse(null);
            if(player==null)continue;
            AABB area=new AABB(run.minX,run.minY,run.minZ,run.maxX+1,run.maxY+1,run.maxZ+1);
            // Endermen should be very rare background threats, never a pile of
            // teleporting entities dying in the void below the corridors.
            List<EnderMan> endermen=level.getEntitiesOfClass(EnderMan.class,area);
            for(int i=0;i<endermen.size();i++){
                EnderMan e=endermen.get(i);
                if(i>=2 || e.getY()<run.spawnPos.getY()-1 || !run.contains(e.blockPosition())){
                    e.discard();
                }
            }
            boolean sanctuary=Math.abs(player.getBlockX()-run.exitPos.getX())<=10
                    && Math.abs(player.getBlockZ()-run.exitPos.getZ())<=10;
            if(sanctuary)continue;
            if(now-run.startedAt>20*32 && now%100==((run.id%5)*20L)
                    && level.getEntitiesOfClass(VeilHound.class,area).isEmpty()
                    && level.random.nextFloat()<.46){
                BlockPos where=findFloorBehind(level,run,player,7,15);
                if(where!=null){
                    VeilHound dog=ModEntities.VEIL_HOUND.get().create(level);
                    if(dog!=null){
                        dog.moveTo(where.getX()+.5,where.getY(),where.getZ()+.5,0,0);
                        if(level.addFreshEntity(dog))level.playSound(null,where,
                                net.minecraft.sounds.SoundEvents.WOLF_GROWL,
                                net.minecraft.sounds.SoundSource.HOSTILE,.72F,.48F);
                    }
                }
            }
            if(now-run.startedAt>20*15 && now%140==((run.id%7)*20L)
                    && level.getEntitiesOfClass(VeilGhost.class,area).isEmpty()
                    && level.random.nextFloat()<.52){
                BlockPos where=findFloorBehind(level,run,player,8,17);
                if(where!=null){
                    VeilGhost ghost=ModEntities.VEIL_GHOST.get().create(level);
                    if(ghost!=null){
                        ghost.moveTo(where.getX()+.5,where.getY(),where.getZ()+.5,0,0);
                        if(level.addFreshEntity(ghost))level.playSound(null,where,
                                net.minecraft.sounds.SoundEvents.ENDERMAN_AMBIENT,
                                net.minecraft.sounds.SoundSource.HOSTILE,.85F,.48F);
                    }
                }
            }
        }
    }

    private static BlockPos findFloorBehind(ServerLevel level,PocketRunEntry run,
                                            ServerPlayer player,int min,int max){
        Vec3 look=player.getLookAngle();
        for(int attempt=0;attempt<65;attempt++){
            double dist=min+level.random.nextDouble()*(max-min);
            double side=(level.random.nextDouble()-.5)*8;
            int x=(int)Math.floor(player.getX()-look.x*dist-look.z*side);
            int z=(int)Math.floor(player.getZ()-look.z*dist+look.x*side);
            BlockPos feet=new BlockPos(x,run.spawnPos.getY()+1,z);
            if(!run.contains(feet) || feet.distSqr(run.exitPos)<160
                    || feet.distSqr(run.spawnPos)<36)continue;
            if(level.getBlockState(feet).isAir()
                    && level.getBlockState(feet.above()).isAir()
                    && level.getBlockState(feet.below()).isCollisionShapeFullBlock(level,feet.below()))
                return feet;
        }
        return null;
    }
}
