package dev.minemods.traumacraft.mixin;

import dev.minemods.traumacraft.climb.ClimbManager;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerClimbMovementMixin {
    @Shadow public ServerPlayer player;
    @Shadow private boolean clientIsFloating;
    @Inject(method="handleMovePlayer",at=@At("HEAD"),cancellable=true)
    private void traumatic$serverOwnsClimb(ServerboundMovePlayerPacket packet,CallbackInfo ci){
        PacketUtils.ensureRunningOnSameThread(packet,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());
        Vec3 reported=new Vec3(packet.getX(player.getX()),packet.getY(player.getY()),packet.getZ(player.getZ()));
        if(!ClimbManager.blocksMove(player,reported))return;
        // Never accept client position while climbing. Allow normal look input only.
        float yaw=packet.getYRot(player.getYRot()),pitch=packet.getXRot(player.getXRot());
        if(Float.isFinite(yaw)&&Float.isFinite(pitch)){
            player.setYRot(Mth.wrapDegrees(yaw));
            player.setXRot(Mth.clamp(pitch,-90,90));
        }
        clientIsFloating=false;
        ci.cancel();
    }
}
