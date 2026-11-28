package dev.minemods.treechopper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid=TreeChopper.ID,value=Dist.CLIENT)
public final class ChopClient {
    private static LocalPlayer owner;
    private static BlockPos target;
    private static int lastRequest=Integer.MIN_VALUE;
    private static boolean stoppedVanilla;
    private static void clear(){target=null;stoppedVanilla=false;}
    private static BlockPos select(Minecraft mc){
        LocalPlayer player=mc.player;
        if(player==null||mc.level==null||mc.gameMode==null||mc.screen!=null||player.isUsingItem())return null;
        if(owner!=player){owner=player;lastRequest=Integer.MIN_VALUE;clear();}
        HitResult hit=mc.hitResult;
        if(target!=null&&TreeChopper.choppingTarget(player,target)&&TreeChopper.aimsAt(player,target,hit))return target;
        if(hit instanceof BlockHitResult block&&hit.getType()==HitResult.Type.BLOCK&&TreeChopper.choppingTarget(player,block.getBlockPos()))return block.getBlockPos();
        return null;
    }
    private static void request(Minecraft mc,BlockPos pos){
        target=pos.immutable();
        if(!stoppedVanilla){mc.gameMode.stopDestroyBlock();stoppedVanilla=true;}
        int now=mc.player.tickCount;
        if(lastRequest!=Integer.MIN_VALUE&&now>=lastRequest&&now-lastRequest<2)return;
        lastRequest=now;
        PacketDistributor.sendToServer(new ChopNetwork.Strike(target));
    }
    @SubscribeEvent
    public static void onAttack(InputEvent.InteractionKeyMappingTriggered event){
        if(!event.isAttack())return;
        Minecraft mc=Minecraft.getInstance();
        BlockPos pos=select(mc);
        if(pos==null){clear();return;}
        event.setCanceled(true);
        event.setSwingHand(false);
        request(mc,pos);
    }
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.gameMode==null||mc.screen!=null||!mc.options.keyAttack.isDown()){clear();return;}
        if(target!=null){
            BlockPos pos=select(mc);
            if(pos!=null)request(mc,pos);else clear();
        }
    }
}
