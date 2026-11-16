package dev.bearback.client;

import dev.bearback.Bearback;
import dev.bearback.BearNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.PolarBear;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import java.util.HashMap;
import java.util.Map;

public final class ClientHooks implements ClientModInitializer {
    private static final KeyMapping SWIPE=new KeyMapping("key.bearback.swipe",
            InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,"key.categories.bearback");
    private static final Map<Integer,Boolean> SADDLED=new HashMap<>();
    private static final Map<Integer,Boolean> BAGS=new HashMap<>();
    private static boolean opening;

    @Override public void onInitializeClient(){
        KeyBindingHelper.registerKeyBinding(SWIPE);
        EntityRendererRegistry.register(EntityType.POLAR_BEAR,BearPolarRenderer::new);
        EntityRendererRegistry.register(Bearback.BLACK_BEAR,BlackBearRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(BearNetwork.Equipment.TYPE,(packet,context)->{
            context.client().execute(()->{
                SADDLED.put(packet.id(),packet.saddle());
                BAGS.put(packet.id(),packet.bags());
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(ClientHooks::onTick);
    }
    public static boolean saddled(PolarBear bear){return SADDLED.getOrDefault(bear.getId(),false);}
    public static boolean bags(PolarBear bear){return BAGS.getOrDefault(bear.getId(),false);}
    private static void onTick(Minecraft mc){
        if(mc.player==null)return;
        if(mc.player.getVehicle() instanceof PolarBear bear && mc.screen instanceof InventoryScreen && bags(bear)){
            mc.setScreen(null);
            if(!opening){
                opening=true;
                ClientPlayNetworking.send(new BearNetwork.OpenCargo(bear.getId()));
            }
            return;
        }
        if(mc.screen!=null){opening=false;return;}
        opening=false;
        if(!(mc.player.getVehicle() instanceof PolarBear))return;
        float f=(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0);
        float s=(mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0);
        ClientPlayNetworking.send(new BearNetwork.Ride(f,s,mc.options.keyJump.isDown(),
                SWIPE.consumeClick(),mc.player.getYRot()));
    }
}
