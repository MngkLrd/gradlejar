package dev.kirill.embershelter.event;

import dev.kirill.embershelter.camp.CampBuilder;
import dev.kirill.embershelter.camp.CampProtection;
import dev.kirill.embershelter.camp.RetaliationTracker;
import dev.kirill.embershelter.camp.ProvokedAnimalGoal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class CampEvents {
    @SubscribeEvent
    public void serverTick(ServerTickEvent.Post event) {
        CampBuilder.tick(event.getServer());
        for(ServerLevel level : event.getServer().getAllLevels()) {
            if(level.getGameTime()%10!=0) continue;
            for(ServerPlayer player : level.players()) {
                if(!player.isAlive()) continue;
                for(Mob mob : level.getEntitiesOfClass(Mob.class,new AABB(player.blockPosition()).inflate(12))) {
                    if(mob.getTarget()==player && CampProtection.protectedFrom(mob,player,level)) {
                        mob.setTarget(null);
                        mob.getNavigation().stop();
                        if(mob instanceof Creeper c && !c.isIgnited()) c.setSwellDir(-1);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) ||
                !(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player) ||
                !(mob.level() instanceof ServerLevel level)) return;
        if (CampProtection.protectedFrom(mob, player, level)) {
            event.setNewAboutToBeSetTarget(null);
            if(mob instanceof Creeper c && !c.isIgnited()) c.setSwellDir(-1);
        }
    }

    @SubscribeEvent
    public void afterDamage(LivingDamageEvent.Post event) {
        if(event.getNewDamage()<=0 || !(event.getEntity() instanceof Mob mob)
            || !(mob.level() instanceof ServerLevel level)
            || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        RetaliationTracker.provoke(mob,player,level);
    }
    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide() || !(event.getEntity() instanceof Animal animal)) return;
        boolean installed=animal.goalSelector.getAvailableGoals().stream()
          .anyMatch(g->g.getGoal() instanceof ProvokedAnimalGoal);
        if(!installed) animal.goalSelector.addGoal(0,new ProvokedAnimalGoal(animal));
    }
    @SubscribeEvent
    public void serverStopped(ServerStoppedEvent event) {
        CampBuilder.clear();
        RetaliationTracker.clear();
    }
}
