package com.kirill.pocketrealms.world;

import com.kirill.pocketrealms.entity.Knocker;
import com.kirill.pocketrealms.entity.CeilingLurker;
import com.kirill.pocketrealms.entity.ChoirWraith;
import com.kirill.pocketrealms.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bounded stage-II encounter director. Never spawns wider bodies in one-block
 * corridors or a monster on the start tile / terminal reward floor.
 */
public final class DepthsEncounterDirector {
    private DepthsEncounterDirector(){}
    public static void tick(MinecraftServer server){
        ServerLevel level=server.getLevel(ModDimensions.POCKET_DIMENSION);
        if(level==null || level.getGameTime()%20!=0)return;
        long now=level.getGameTime();
        for(PocketRunEntry run:PocketRunData.get(server).allRuns()){
            if(run.stage<2||run.completed||run.retiring||!run.generated||run.spawnPos==null)continue;
            ServerPlayer player=server.getPlayerList().getPlayer(run.owner);
            if(player==null || player.serverLevel()!=level || !player.isAlive()
                    || !run.contains(player.blockPosition()))continue;
            if(Math.abs(player.getBlockX()-run.exitPos.getX())<=10
                    && Math.abs(player.getBlockZ()-run.exitPos.getZ())<=10)continue;
            AABB sector=new AABB(run.minX,run.minY,run.minZ,run.maxX+1,run.maxY+1,run.maxZ+1);
            if(now-run.startedAt<20*24L || now%140 !=(run.id%7)*20L)continue;
            int count=level.getEntitiesOfClass(Knocker.class,sector).size()
                    +level.getEntitiesOfClass(CeilingLurker.class,sector).size()
                    +level.getEntitiesOfClass(ChoirWraith.class,sector).size();
            if(count>=4)continue;
            boolean knocker=level.getEntitiesOfClass(Knocker.class,sector).isEmpty();
            boolean lurker=level.getEntitiesOfClass(CeilingLurker.class,sector).isEmpty();
            boolean choir=level.getEntitiesOfClass(ChoirWraith.class,sector).isEmpty();
            // Prioritize the monsters the player has not met yet in this sector.
            int[] choices=new int[3];int n=0;
            if(knocker)choices[n++]=0;
            if(lurker)choices[n++]=1;
            if(choir)choices[n++]=2;
            if(n==0)continue;
            int choice=choices[level.random.nextInt(n)];
            BlockPos at=findSpawn(level,run,player,choice==0);
            if(at==null)continue;
            Mob mob=switch(choice){
                case 0 -> ModEntities.KNOCKER.get().create(level);
                case 1 -> ModEntities.CEILING_LURKER.get().create(level);
                default -> ModEntities.CHOIR_WRAITH.get().create(level);
            };
            if(mob==null)continue;
            if(choice==1)at=at.above(3);
            mob.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,level.random.nextFloat()*360,0);
            mob.finalizeSpawn(level,level.getCurrentDifficultyAt(at),MobSpawnType.TRIGGERED,null);
            if(choice!=1&&!level.noCollision(mob))continue;
            level.addFreshEntity(mob);
        }
    }
    private static BlockPos findSpawn(ServerLevel level,PocketRunEntry run,ServerPlayer player,boolean wide){
        List<BlockPos> options=new ArrayList<>();
        int footY=run.spawnPos.getY()+1;
        int pX=player.getBlockX(),pZ=player.getBlockZ();
        for(int dx=-17;dx<=17;dx++)for(int dz=-17;dz<=17;dz++){
            if(dx*dx+dz*dz<45 || dx*dx+dz*dz>289)continue;
            BlockPos feet=new BlockPos(pX+dx,footY,pZ+dz);
            if(!run.contains(feet)||feet.distSqr(run.spawnPos)<110
                    || (Math.abs(feet.getX()-run.exitPos.getX())<=11
                        && Math.abs(feet.getZ()-run.exitPos.getZ())<=11))continue;
            if(!level.getBlockState(feet).isAir()||!level.getBlockState(feet.above()).isAir()
                    ||!level.getBlockState(feet.below()).isCollisionShapeFullBlock(level,feet.below()))continue;
            if(wide){
                boolean clear=true;
                for(int ax=-1;ax<=1;ax++)for(int az=-1;az<=1;az++){
                    BlockPos around=feet.offset(ax,0,az);
                    if(!level.getBlockState(around).isAir()||!level.getBlockState(around.above()).isAir()
                            ||!level.getBlockState(around.below()).isCollisionShapeFullBlock(level,around.below())){
                        clear=false;break;
                    }
                }
                if(!clear)continue;
            }
            options.add(feet);
        }
        if(options.isEmpty())return null;
        return options.get(level.random.nextInt(options.size()));
    }
}
