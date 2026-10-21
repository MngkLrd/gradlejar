package dev.bearback.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.bearback.BearRidePacket;
import dev.bearback.Bearback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.PolarBear;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Bearback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientHooks {
    private static final KeyMapping SWIPE = new KeyMapping("key.bearback.swipe",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.bearback");

    static {
        NeoForge.EVENT_BUS.addListener(ClientHooks::onClientTick);
    }

    @SubscribeEvent
    public static void renderers(RegisterRenderers event) {
        event.registerEntityRenderer(Bearback.BLACK_BEAR.get(), BlackBearRenderer::new);
    }

    @SubscribeEvent
    public static void keybind(RegisterKeyMappingsEvent event) {
        event.register(SWIPE);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers event) {
        PolarBearRenderer polar = event.getRenderer(EntityType.POLAR_BEAR);
        if (polar != null) polar.addLayer(new SaddleLayer(polar));
        PolarBearRenderer black = event.getRenderer(Bearback.BLACK_BEAR.get());
        if (black != null) black.addLayer(new SaddleLayer(black));
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getVehicle() instanceof PolarBear)) return;
        if (mc.screen != null) return;
        boolean attack = SWIPE.consumeClick();
        float forward = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        float strafe = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
        PacketDistributor.sendToServer(new BearRidePacket(forward, strafe,
                mc.options.keyJump.isDown(), attack, mc.player.getYRot()));
    }
}
