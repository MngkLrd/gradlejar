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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
    private static final Map<Integer,Boolean> BAGS=new HashMap<>();
    private ClientHooks(){}

    public static void init(IEventBus bus){
        bus.addListener(ClientHooks::renderers);
        bus.addListener(ClientHooks::keybind);
        bus.addListener(ClientHooks::layers);
        MinecraftForge.EVENT_BUS.addListener(ClientHooks::clientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientHooks::logout);
        MinecraftForge.EVENT_BUS.addListener(ClientHooks::onInventory);
    }
    private static void renderers(EntityRenderersEvent.RegisterRenderers e){
        e.registerEntityRenderer(Bearback.BLACK_BEAR.get(),BlackBearRenderer::new);
    }
    private static void keybind(RegisterKeyMappingsEvent e){e.register(SWIPE);}
    private static void layers(EntityRenderersEvent.AddLayers e){
        PolarBearRenderer white=e.getRenderer(EntityType.POLAR_BEAR);
        if(white!=null){white.addLayer(new SaddleLayer(white));white.addLayer(new SaddlebagsLayer(white));}
        PolarBearRenderer black=e.getRenderer(Bearback.BLACK_BEAR.get());
        if(black!=null){black.addLayer(new SaddleLayer(black));black.addLayer(new SaddlebagsLayer(black));}
    }
    public static void syncEquipment(int id,boolean saddled,boolean bags){SADDLES.put(id,saddled);BAGS.put(id,bags);}
    public static boolean hasBags(PolarBear bear){return BAGS.getOrDefault(bear.getId(),false);}
    public static boolean isSaddled(PolarBear bear){return SADDLES.getOrDefault(bear.getId(),false);}
    private static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){
        SADDLES.clear();BAGS.clear();
    }
    private static void onInventory(ScreenEvent.Opening event) {
        if(!(event.getNewScreen() instanceof InventoryScreen))return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null&&mc.player.getVehicle() instanceof PolarBear bear&&hasBags(bear)){
            event.setCanceled(true);
            BearNetwork.openCargo(bear.getId());
        }
    }

    private static void clientTick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.screen!=null||!(mc.player.getVehicle() instanceof PolarBear))return;
        boolean attack=SWIPE.consumeClick();
        float forward=(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0);
        float strafe=(mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0);
        BearNetwork.ride(forward,strafe,mc.options.keyJump.isDown(),attack,mc.player.getYRot());
    }
}
