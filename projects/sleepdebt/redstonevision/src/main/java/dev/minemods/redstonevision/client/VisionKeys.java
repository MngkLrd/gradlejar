package dev.minemods.redstonevision.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.minemods.redstonevision.network.ToggleVisionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class VisionKeys {
    private static final KeyMapping TOGGLE=new KeyMapping("key.redstonevision.toggle",
        InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_V,"key.categories.redstonevision");
    private VisionKeys() {}
    @EventBusSubscriber(modid="redstonevision",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) { e.register(TOGGLE); }
    }
    @EventBusSubscriber(modid="redstonevision",value=Dist.CLIENT)
    public static final class Runtime {
        @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
            Minecraft mc=Minecraft.getInstance();
            while (TOGGLE.consumeClick()) {
                if (!VisionState.hasGoggles()) {
                    if (mc.player!=null) mc.player.displayClientMessage(
                        Component.translatable("message.redstonevision.need_goggles"),true);
                } else PacketDistributor.sendToServer(new ToggleVisionPayload(mc.player.isShiftKeyDown()));
            }
            if (mc.level!=null && mc.player!=null) CircuitScanner.tick(mc);
        }
    }
}
