package dev.bearback.client;

import dev.bearback.Bearback;
import dev.bearback.BearNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import java.util.HashMap;
import java.util.Map;

public final class ClientHooks {
 private static final KeyMapping SWIPE=new KeyMapping("key.bearback.swipe",82,KeyMapping.Category.GAMEPLAY);
 private static final Map<Integer,Boolean> SADDLE=new HashMap<>(), BAGS=new HashMap<>();
 private ClientHooks(){}
 public static void init(){
    EntityRenderersEvent.RegisterRenderers.BUS.addListener(e->{
        e.registerEntityRenderer(EntityTypes.POLAR_BEAR,BearPolarRenderer::new);
        e.registerEntityRenderer(Bearback.BLACK_BEAR.get(),BlackBearRenderer::new);
    });
    RegisterKeyMappingsEvent.BUS.addListener(e->e.register(SWIPE));
    TickEvent.ClientTickEvent.Post.BUS.addListener(e->tick());
    ScreenEvent.Opening.BUS.addListener(e->{
        if(!(e.getNewScreen() instanceof InventoryScreen))return false;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null&&mc.player.getVehicle() instanceof PolarBear bear&&bags(bear)){
            BearNetwork.openCargo(bear.getId());
            return true;
        }
        return false;
    });
 }
 public static void syncEquipment(int id,boolean saddled,boolean bags){
    SADDLE.put(id,saddled);BAGS.put(id,bags);
 }
 public static boolean saddled(PolarBear b){return SADDLE.getOrDefault(b.getId(),false);}
 public static boolean bags(PolarBear b){return BAGS.getOrDefault(b.getId(),false);}
 private static void tick(){
    Minecraft mc=Minecraft.getInstance();
    if(mc.player==null||mc.gui.screen()!=null||!(mc.player.getVehicle() instanceof PolarBear))return;
    float f=(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0);
    float s=(mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0);
    BearNetwork.ride(f,s,mc.options.keyJump.isDown(),SWIPE.consumeClick(),mc.player.getYRot());
 }
}
