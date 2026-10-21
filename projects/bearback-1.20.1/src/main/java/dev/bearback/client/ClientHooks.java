package dev.bearback.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.bearback.Bearback;
import dev.bearback.BearNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.PolarBearRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientTickEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public final class ClientHooks {
    private static final KeyMapping SWIPE=new KeyMapping("key.bearback.swipe",
        InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,"key.categories.bearback");
    private static final Map<Integer,Boolean> SADDLES=new HashMap<>();
    private ClientHooks(){}

    public static void init(IEventBus bus){
        bus.addListener(ClientHooks::renderers);
        bus.addListener(ClientHooks::keybind);
        bus.addListener(ClientHooks::layers);
        MinecraftForge.EVENT_BUS.addListener(ClientHooks::clientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientHooks::logout);
    }
    private static void renderers(EntityRenderersEvent.RegisterRenderers e){
        e.registerEntityRenderer(Bearback.BLACK_BEAR.get(),BlackBearRenderer::new);
    }
    private static void keybind(RegisterKeyMappingsEvent e){e.register(SWIPE);}
    private static void layers(EntityRenderersEvent.AddLayers e){
        PolarBearRenderer white=e.getRenderer(EntityType.POLAR_BEAR);
        if(white!=null)white.addLayer(new SaddleLayer(white));
        PolarBearRenderer black=e.getRenderer(Bearback.BLACK_BEAR.get());
        if(black!=null)black.addLayer(new SaddleLayer(black));
    }
    public static void syncSaddle(int id,boolean saddled){SADDLES.put(id,saddled);}
    public static boolean isSaddled(PolarBear bear){return SADDLES.getOrDefault(bear.getId(),false);}
    private static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){
        SADDLES.clear();
    }
    private static void clientTick(ClientTickEvent.Post e){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.screen!=null||!(mc.player.getVehicle() instanceof PolarBear))return;
        boolean attack=SWIPE.consumeClick();
        float forward=(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0);
        float strafe=(mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0);
        BearNetwork.ride(forward,strafe,mc.options.keyJump.isDown(),attack,mc.player.getYRot());
    }
}
