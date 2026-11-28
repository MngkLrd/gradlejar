package dev.minemods.excavation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** A physical button press requests one strike; holding is never an auto-miner. */
@EventBusSubscriber(modid=Excavation.ID,value=Dist.CLIENT)
public final class StrikeClient {
    private static LocalPlayer owner;
    private static boolean latched;
    @SubscribeEvent public static void onAttack(InputEvent.InteractionKeyMappingTriggered event){
        if(!event.isAttack())return;
        var mc=Minecraft.getInstance();
        if(mc.player!=owner){owner=mc.player;latched=false;}
        if(mc.player==null||mc.level==null||mc.gameMode==null||mc.screen!=null||mc.player.isCreative()||mc.player.isSpectator())return;
        boolean fresh=!latched;latched=true;
        if(!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK||!Excavation.target(mc.level.getBlockState(hit.getBlockPos())))return;
        event.setCanceled(true);event.setSwingHand(false);
        if(fresh){mc.gameMode.stopDestroyBlock();PacketDistributor.sendToServer(new StrikeNetwork.Strike(hit.getBlockPos()));}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(mc.player!=owner||mc.screen!=null||!mc.options.keyAttack.isDown()){owner=mc.player;latched=false;}
    }
}
