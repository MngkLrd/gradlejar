package dev.voidchisel.client;

import dev.voidchisel.MiningInputPacket;
import dev.voidchisel.VoidChisel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = VoidChisel.MOD_ID, value = Dist.CLIENT)
public final class ClientMiningInput {
    private static BlockPos target;
    private static int heartbeat;

    private ClientMiningInput() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos next = null;

        if (mc.player != null && mc.level != null && mc.screen == null
                && mc.options.keyAttack.isDown()
                && mc.player.getMainHandItem().is(VoidChisel.VOID_CHISEL.get())
                && mc.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK
                && mc.level.getBlockState(hit.getBlockPos()).is(Blocks.BEDROCK)) {
            next = hit.getBlockPos();
        }

        if (next == null) {
            if (target != null && mc.getConnection() != null) {
                PacketDistributor.sendToServer(new MiningInputPacket(0, false));
            }
            target = null;
            heartbeat = 0;
            return;
        }

        if (!next.equals(target)) {
            target = next.immutable();
            heartbeat = 0;
            PacketDistributor.sendToServer(new MiningInputPacket(target.asLong(), true));
        } else if (++heartbeat >= 4) {
            heartbeat = 0;
            PacketDistributor.sendToServer(new MiningInputPacket(target.asLong(), true));
        }
    }
}
