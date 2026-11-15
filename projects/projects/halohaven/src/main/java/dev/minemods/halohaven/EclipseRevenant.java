package dev.minemods.halohaven;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;

/**
 * A singular ruin sentinel. The second phase begins at 55% health and
 * summons two Dusk Wraiths once, not every time it is reloaded.
 */
public final class EclipseRevenant extends Vex {
    private final ServerBossEvent bossBar=new ServerBossEvent(
        Component.translatable("entity.halohaven.eclipse_revenant"),
        BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);

    public EclipseRevenant(EntityType<? extends Vex> type,Level level) {
        super(type,level);
        this.setCustomName(Component.translatable("entity.halohaven.eclipse_revenant"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player){
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player){
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void aiStep(){
        super.aiStep();
        if(level().isClientSide())return;
        bossBar.setProgress(Math.max(0f,Math.min(1f,getHealth()/getMaxHealth())));
        if(tickCount%50==0){
            Player nearby=level().getNearestPlayer(this,26.0);
            if(nearby!=null && nearby.isAlive())this.setTarget(nearby);
        }
        if(getHealth()<getMaxHealth()*0.55f && !getPersistentData().getBoolean("halohaven_second_phase")){
            getPersistentData().putBoolean("halohaven_second_phase",true);
            for(int k=0;k<2;k++){
                Vex minion=HaloEntities.DUSK_WRAITH.get().create((ServerLevel)level());
                if(minion==null)continue;
                minion.moveTo(getX()+k*3-1.5,getY()+1,getZ()+k*3-1.5,
                    getYRot(),getXRot());
                minion.setPersistenceRequired();
                level().addFreshEntity(minion);
            }
            ((ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                getX(),getY()+0.8,getZ(),80,1.3,1.3,1.3,0.075);
        }
    }
}
