package dev.minemods.halohaven;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class HavenEvents {
    private HavenEvents() {}

    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if(event.getLevel().isClientSide())return;
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        if(!p.level().dimension().equals(HaloHaven.HEAVEN))return;
        BlockPos pos=event.getPos();
        if(event.getLevel().getBlockState(pos).is(HaloHaven.PILGRIM_ALTAR.get())) {
            ItemStack item=p.getItemInHand(event.getHand());
            int favor=p.getPersistentData().getInt("halohaven_favor");
            if(item.is(HaloHaven.SKYBERRY.get())){
                if(!p.getAbilities().instabuild)item.shrink(1);
                p.getPersistentData().putInt("halohaven_favor",Math.min(10000,favor+1));
                p.displayClientMessage(Component.translatable("message.halohaven.favor",favor+1),true);
                ((ServerLevel)p.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5,20,0.5,0.5,0.5,0.1);
                p.level().playSound(null,pos,SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.BLOCKS,1,1.35f);
                complete(event);
            }else if(item.isEmpty()){
                long last=p.getPersistentData().getLong("halohaven_altar_claim");
                if(favor>=12&&(last==0||p.level().getGameTime()-last>=20)){
                    p.getPersistentData().putInt("halohaven_favor",favor-12);
                    p.getPersistentData().putLong("halohaven_altar_claim",p.level().getGameTime());
                    ItemStack prize=new ItemStack(HaloHaven.SUNSHARD.get(),2);
                    if(!p.getInventory().add(prize))p.drop(prize,false);
                    p.displayClientMessage(Component.translatable("message.halohaven.redeemed",favor-12),true);
                    ((ServerLevel)p.level()).sendParticles(ParticleTypes.END_ROD,
                        pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5,60,0.6,0.8,0.6,0.06);
                    p.level().playSound(null,pos,SoundEvents.BEACON_POWER_SELECT,SoundSource.BLOCKS,1,1.3f);
                }else{
                    p.displayClientMessage(Component.translatable("message.halohaven.need_favor",favor),true);
                }
                complete(event);
            }
            return;
        }
        if(!event.getLevel().getBlockState(pos).is(HaloHaven.WAYSTONE.get()))return;
        if(p.getCooldowns().isOnCooldown(HaloHaven.SKY_COMPASS.get()))return;
        int offset=p.isShiftKeyDown()?-512:512;
        int cx=Math.round(pos.getX()/512f)*512;
        int cz=Math.round(pos.getZ()/512f)*512;
        int destX=cx+offset;
        ServerLevel world=(ServerLevel)p.level();
        HavenVillages.ensure(world,destX,cz);
        p.teleportTo(world,destX+0.5,HavenVillages.WALK_Y+1.0,cz+8.5,p.getYRot(),p.getXRot());
        world.playSound(null,p.blockPosition(),SoundEvents.END_PORTAL_FRAME_FILL,SoundSource.BLOCKS,0.9f,1.25f);
        p.getCooldowns().addCooldown(HaloHaven.SKY_COMPASS.get(),60);
        p.displayClientMessage(Component.translatable("message.halohaven.transit",destX,cz),true);
        complete(event);
    }
    private static void complete(PlayerInteractEvent.RightClickBlock event){
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    public static void onLivingDeath(LivingDeathEvent event){
        if(event.getEntity().level().isClientSide())return;
        if(!(event.getSource().getEntity() instanceof ServerPlayer p))return;
        if(!p.level().dimension().equals(HaloHaven.HEAVEN))return;
        if(event.getEntity().getType()==HaloEntities.DUSK_WRAITH.get()){
            int defeated=p.getPersistentData().getInt("halohaven_wraith_defeats")+1;
            p.getPersistentData().putInt("halohaven_wraith_defeats",defeated);
            if(defeated==3 || defeated==8){
                int favor=p.getPersistentData().getInt("halohaven_favor");
                p.getPersistentData().putInt("halohaven_favor",favor+(defeated==3?6:12));
                p.displayClientMessage(Component.translatable("message.halohaven.trial_wraith",defeated),false);
            }
        }else if(event.getEntity().getType()==HaloEntities.ECLIPSE_REVENANT.get()){
            int killed=p.getPersistentData().getInt("halohaven_eclipse_defeats")+1;
            p.getPersistentData().putInt("halohaven_eclipse_defeats",killed);
            int favor=p.getPersistentData().getInt("halohaven_favor");
            p.getPersistentData().putInt("halohaven_favor",favor+20);
            p.displayClientMessage(Component.translatable("message.halohaven.eclipse_defeated"),false);
            ((ServerLevel)p.level()).sendParticles(ParticleTypes.END_ROD,
                event.getEntity().getX(),event.getEntity().getY(),event.getEntity().getZ(),
                140,2,2,2,0.08);
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if(event.getLevel().isClientSide())return;
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        if(!p.level().dimension().equals(HaloHaven.HEAVEN))return;
        ItemStack held=p.getItemInHand(event.getHand());
        if(event.getTarget().getType()==HaloEntities.LUMEN.get()&&held.is(HaloHaven.SKYBERRY.get())){
            long last=event.getTarget().getPersistentData().getLong("halohaven_gift_time");
            if(event.getTarget().getPersistentData().contains("halohaven_gift_time")
                && p.level().getGameTime()-last<20*180){
                p.displayClientMessage(Component.translatable("message.halohaven.lumen_rest"),true);
            }else{
                if(!p.getAbilities().instabuild)held.shrink(1);
                event.getTarget().getPersistentData().putLong("halohaven_gift_time",p.level().getGameTime());
                ItemStack prize=new ItemStack(HaloHaven.STARDUST.get(),1);
                if(!p.getInventory().add(prize))p.drop(prize,false);
                p.displayClientMessage(Component.translatable("message.halohaven.lumen_gift"),true);
                p.level().playSound(null,event.getTarget().blockPosition(),
                    SoundEvents.ALLAY_AMBIENT_WITH_ITEM,SoundSource.NEUTRAL,1f,1.2f);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if(event.getTarget().getType()==HaloEntities.CLOUDHARE.get()
                && held.is(Items.DANDELION)){
            if(!p.getAbilities().instabuild)held.shrink(1);
            int favor=p.getPersistentData().getInt("halohaven_favor");
            p.getPersistentData().putInt("halohaven_favor",Math.min(10000,favor+1));
            p.displayClientMessage(Component.translatable("message.halohaven.hare_gift",favor+1),true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
