package dev.bearback.client;

import dev.bearback.Bearback;
import dev.bearback.BearNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.entity.EntityTypes;
import java.util.HashMap;
import java.util.Map;

public final class ClientHooks {
    private static final KeyMapping SWIPE=new KeyMapping("key.bearback.swipe",
            82,KeyMapping.Category.GAMEPLAY);
    private static final Map<Integer,Boolean> SADDLES=new HashMap<>();

    public static void register(IEventBus bus){
        bus.addListener(ClientHooks::registerRenderers);
        bus.addListener(ClientHooks::registerKeys);
        NeoForge.EVENT_BUS.addListener(ClientHooks::tick);
    }
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
        e.registerEntityRenderer(EntityTypes.POLAR_BEAR,BearPolarRenderer::new);
        e.registerEntityRenderer(Bearback.BLACK_BEAR.get(),BlackBearRenderer::new);
    }
    private static void registerKeys(RegisterKeyMappingsEvent e){e.register(SWIPE);}
    public static void syncSaddle(int id,boolean saddled){SADDLES.put(id,saddled);}
    public static boolean isSaddled(PolarBear bear){return SADDLES.getOrDefault(bear.getId(),false);}

    private static void tick(ClientTickEvent.Post e){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!(mc.player.getVehicle() instanceof PolarBear))return;
        boolean attack=SWIPE.consumeClick();
        float f=(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0);
        float s=(mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0);
        BearNetwork.ride(f,s,mc.options.keyJump.isDown(),attack,mc.player.getYRot());
    }
}
