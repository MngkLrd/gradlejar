package dev.minemods.riverweir.logic;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.block.WeirNetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge normally bypasses Block.use for sneak-use with a held item. */
@Mod.EventBusSubscriber(modid=RiverWeirMod.ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class WeirMaintenanceEvents {
    @SubscribeEvent
    public static void repair(PlayerInteractEvent.RightClickBlock event){
        if(!event.getEntity().isShiftKeyDown()||!WeirLogic.isJuteNet(event.getItemStack()))return;
        BlockState state=event.getLevel().getBlockState(event.getPos());
        boolean stake=state.is(RiverWeirMod.POST.get());
        if(!stake&&!state.is(RiverWeirMod.NET.get()))return;
        if(event.getLevel() instanceof ServerLevel level){
            BlockPos post=stake?event.getPos():WeirLogic.findLinkedPost(level,event.getPos(),state.getValue(WeirNetBlock.AXIS));
            if(post!=null)WeirLogic.repair(level,post,event.getEntity(),event.getHand());
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }
}
