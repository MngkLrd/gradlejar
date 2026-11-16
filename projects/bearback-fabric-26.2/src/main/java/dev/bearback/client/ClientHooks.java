package dev.bearback.client;
import dev.bearback.Bearback;
import dev.bearback.BearNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import java.util.HashMap;
import java.util.Map;

public final class ClientHooks implements ClientModInitializer {
 private static final Map<Integer,Boolean> SADDLE=new HashMap<>(),BAGS=new HashMap<>();
 private static final KeyMapping SWIPE=new KeyMapping("key.bearback.swipe",82,KeyMapping.Category.GAMEPLAY);
 private static boolean opening;
 @Override public void onInitializeClient(){
    KeyBindingHelper.registerKeyBinding(SWIPE);
    EntityRendererRegistry.register(EntityTypes.POLAR_BEAR,BearPolarRenderer::new);
    EntityRendererRegistry.register(Bearback.BLACK_BEAR,BlackBearRenderer::new);
    ClientPlayNetworking.registerGlobalReceiver(BearNetwork.Equipment.TYPE,(packet,context)->
            context.client().execute(()->{
                SADDLE.put(packet.id(),packet.saddle());
                BAGS.put(packet.id(),packet.bags());
            }));
    ClientTickEvents.END_CLIENT_TICK.register(ClientHooks::tick);
 }
 public static boolean saddled(PolarBear b){return SADDLE.getOrDefault(b.getId(),false);}
 public static boolean bags(PolarBear b){return BAGS.getOrDefault(b.getId(),false);}
 private static void tick(Minecraft mc){
    if(mc.player==null)return;
    if(mc.player.getVehicle() instanceof PolarBear b && mc.screen instanceof InventoryScreen && bags(b)){
       mc.setScreen(null);
       if(!opening){opening=true;ClientPlayNetworking.send(new BearNetwork.OpenCargo(b.getId()));}
       return;
    }
    if(mc.screen!=null){opening=false;return;}
    opening=false;
    if(!(mc.player.getVehicle() instanceof PolarBear))return;
    float f=(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0);
    float s=(mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0);
    ClientPlayNetworking.send(new BearNetwork.Ride(f,s,mc.options.keyJump.isDown(),SWIPE.consumeClick(),mc.player.getYRot()));
 }
}
