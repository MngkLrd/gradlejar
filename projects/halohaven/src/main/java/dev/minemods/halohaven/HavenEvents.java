package dev.minemods.halohaven;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class HavenEvents {
    private HavenEvents() {}
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if(event.getLevel().isClientSide())return;
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        BlockPos pos=event.getPos();
        if(!event.getLevel().getBlockState(pos).is(HaloHaven.WAYSTONE.get()))return;
        if(!p.level().dimension().equals(HaloHaven.HEAVEN))return;
        if(p.getCooldowns().isOnCooldown(HaloHaven.SKY_COMPASS.get()))return;
        // The gateway heading is fixed; shift-click reverses it.
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
        event.setCanceled(true);
    }
}
