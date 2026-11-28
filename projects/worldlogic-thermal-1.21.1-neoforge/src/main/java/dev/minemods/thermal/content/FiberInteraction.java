package dev.minemods.thermal.content;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
public final class FiberInteraction {
    private FiberInteraction(){}
    public static boolean place(ServerLevel l,BlockPos support,Player p,ItemStack raw){
        BlockPos at=support.above();
        if(!raw.is(ThermalContent.RAW_FIBER.get())||!FiberWorkpieceBlock.surface(l,support)||!l.isEmptyBlock(at)||!l.mayInteract(p,support)||!l.mayInteract(p,at)||!p.mayUseItemAt(at,Direction.UP,raw))return false;
        if(!l.setBlockAndUpdate(at,ThermalContent.WORKPIECE.get().defaultBlockState()))return false;
        if(!(l.getBlockEntity(at) instanceof FiberWorkpieceEntity work)){l.removeBlock(at,false);return false;}
        long saved=FiberWorkpieceEntity.fromItem(raw);
        work.update(saved);if(!p.getAbilities().instabuild)raw.shrink(1);
        return true;
    }
    public static void right(PlayerInteractEvent.RightClickBlock e){
        if(e.getHand()!=InteractionHand.MAIN_HAND||e.getEntity().isSpectator()||!e.getItemStack().is(ThermalContent.RAW_FIBER.get())||e.getFace()!=Direction.UP||!FiberWorkpieceBlock.surface(e.getLevel(),e.getPos())||!e.getLevel().isEmptyBlock(e.getPos().above()))return;
        if(e.getLevel() instanceof ServerLevel l&&!place(l,e.getPos(),e.getEntity(),e.getItemStack()))return;
        e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
    }
    public static void left(PlayerInteractEvent.LeftClickBlock e){
        if(!(e.getLevel() instanceof ServerLevel l)||!l.getBlockState(e.getPos()).is(ThermalContent.WORKPIECE.get()))return;
        e.setCanceled(true);
        if(e.getAction()!=PlayerInteractEvent.LeftClickBlock.Action.START||!(e.getEntity() instanceof ServerPlayer p)||p.isSpectator()||!l.mayInteract(p,e.getPos())||!p.getAbilities().mayBuild)return;
        HitResult trace=p.pick(p.blockInteractionRange(),1f,false);
        if(!(trace instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(e.getPos())||hit.getDirection()!=Direction.UP)return;
        Vec3 v=hit.getLocation();
        int cell=FiberMask.index(v.x-e.getPos().getX(),v.z-e.getPos().getZ());
        if(cell<0)return;
        String action=strike(l,e.getPos(),p,cell,p.isShiftKeyDown());
        if("changed".equals(action)&&l.getBlockEntity(e.getPos()) instanceof FiberWorkpieceEntity work){
            p.displayClientMessage(Component.translatable(FiberMask.finished(work.mask())?"message.worldlogic_thermal.fiber_ready":"message.worldlogic_thermal.fiber_progress",FiberMask.progress(work.mask())),true);
            l.playSound(null,e.getPos(),SoundEvents.GRASS_BREAK,SoundSource.BLOCKS,.55f,1.3f);
        }
    }
    public static String strike(ServerLevel l,BlockPos at,Player p,int cell,boolean lift){
        if(cell<0||cell>=64||p.isSpectator()||!l.mayInteract(p,at)||!(l.getBlockEntity(at) instanceof FiberWorkpieceEntity work))return "ignored";
        ItemStack hand=p.getMainHandItem();
        boolean scraper=hand.is(ThermalContent.SCRAPER.get());
        if(!hand.isEmpty()&&!scraper)return "ignored";
        if(lift||FiberMask.finished(work.mask())){l.removeBlock(at,false);return "removed";}
        long next=FiberMask.scrape(work.mask(),cell,scraper);
        if(next==work.mask())return "ignored";
        work.update(next);
        if(scraper&&!p.getAbilities().instabuild)hand.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);
        return "changed";
    }
}
