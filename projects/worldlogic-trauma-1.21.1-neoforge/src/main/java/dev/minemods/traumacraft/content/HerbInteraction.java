package dev.minemods.traumacraft.content;
import dev.minemods.traumacraft.registry.ModItems;
import dev.minemods.traumacraft.integration.SurfaceWater;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
public final class HerbInteraction {
    private HerbInteraction(){}
    private static boolean canWork(ServerLevel l,BlockPos support,Player p,ItemStack input){
        var at=support.above();
        return !p.isSpectator()&&p.getAbilities().mayBuild&&l.mayInteract(p,support)&&l.mayInteract(p,at)&&p.mayUseItemAt(at,Direction.UP,input)&&
            (!(p instanceof ServerPlayer sp)||!dev.minemods.traumacraft.integration.WorldLogicBridge.functionsEnabled(sp)||dev.minemods.traumacraft.integration.WorldLogicBridge.miningFactor(sp)>0);
    }
    public static boolean wash(ServerLevel l,BlockPos support,Player p){
        var water=p.getMainHandItem();var at=support.above();
        if(!canWork(l,support,p,water)||!HerbWorkpieceBlock.dryingSurface(l,support)||!l.isEmptyBlock(at)||!SurfaceWater.clean(water))return false;
        if(!l.setBlockAndUpdate(at,HerbContent.WORKPIECE.get().defaultBlockState()))return false;
        if(!(l.getBlockEntity(at) instanceof HerbWorkpieceEntity work)){l.removeBlock(at,false);return false;}
        work.prepareSurface();
        if(!p.getAbilities().instabuild){
            water.shrink(1);var bottle=new ItemStack(Items.GLASS_BOTTLE);
            if(water.isEmpty())p.setItemInHand(InteractionHand.MAIN_HAND,bottle);else if(!p.getInventory().add(bottle))p.drop(bottle,false);
            p.getInventory().setChanged();
        }
        l.playSound(null,support,SoundEvents.BOTTLE_EMPTY,SoundSource.BLOCKS,.7f,1f);
        return true;
    }
    public static boolean place(ServerLevel l,BlockPos support,Player p,ItemStack raw){
        BlockPos at=support.above();HerbSpecies species=HerbSpecies.input(raw);
        if(!canWork(l,support,p,raw)||species==HerbSpecies.UNKNOWN)return false;
        if(HerbWorkpieceBlock.dryingSurface(l,support)){
            if(!species.botanical()||HerbWorkpieceEntity.fromItem(raw)!=0||!(l.getBlockEntity(at) instanceof HerbWorkpieceEntity ready)||!ready.washed()||!ready.emptySurface())return false;
            ready.initialize(raw,species);if(!p.getAbilities().instabuild)raw.shrink(1);return true;
        }
        if(!HerbWorkpieceBlock.surface(l,support)||!l.isEmptyBlock(at))return false;
        if(!l.setBlockAndUpdate(at,HerbContent.WORKPIECE.get().defaultBlockState()))return false;
        if(!(l.getBlockEntity(at) instanceof HerbWorkpieceEntity work)){l.removeBlock(at,false);return false;}
        work.initialize(raw,species);if(!p.getAbilities().instabuild)raw.shrink(1);return true;
    }
    public static void right(PlayerInteractEvent.RightClickBlock e){
        if(e.isCanceled()||e.getHand()!=InteractionHand.MAIN_HAND||e.getEntity().isSpectator())return;
        if(e.getFace()==Direction.UP&&HerbWorkpieceBlock.dryingSurface(e.getLevel(),e.getPos())&&SurfaceWater.waterBottle(e.getItemStack())){
            if(e.getLevel() instanceof ServerLevel l){boolean ok=wash(l,e.getPos(),e.getEntity());e.getEntity().displayClientMessage(Component.translatable(ok?"message.traumacraft.surface_washed":"message.traumacraft.wash_rejected"),true);}
            e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);return;
        }
        if(e.getFace()==Direction.UP&&e.getLevel().getBlockEntity(e.getPos()) instanceof HerbWorkpieceEntity ready&&ready.washed()&&ready.emptySurface()&&HerbSpecies.input(e.getItemStack()).botanical()){
            if(e.getLevel() instanceof ServerLevel l){boolean ok=place(l,e.getPos().below(),e.getEntity(),e.getItemStack());e.getEntity().displayClientMessage(Component.translatable(ok?"message.traumacraft.drying_started":"message.traumacraft.drying_rejected"),true);}
            e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);return;
        }
        if(e.getFace()==Direction.UP&&HerbWorkpieceBlock.dryingSurface(e.getLevel(),e.getPos())&&HerbSpecies.input(e.getItemStack()).botanical()){
            if(e.getLevel() instanceof ServerLevel l){boolean ok=place(l,e.getPos(),e.getEntity(),e.getItemStack());e.getEntity().displayClientMessage(Component.translatable(ok?"message.traumacraft.drying_started":"message.traumacraft.wash_first"),true);}
            e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);return;
        }
        if(!e.isCanceled()&&e.getHand()==InteractionHand.MAIN_HAND&&e.getItemStack().isEmpty()&&e.getEntity().isShiftKeyDown()&&e.getLevel().getBlockEntity(e.getPos()) instanceof HerbWorkpieceEntity work&&work.species().botanical()){
            if(e.getLevel() instanceof ServerLevel)e.getEntity().displayClientMessage(Component.translatable("message.traumacraft.herb_drying",work.drySeconds()*100/HerbSpecies.DRY_SECONDS),true);
            e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);return;
        }
        if(e.isCanceled()||e.getHand()!=InteractionHand.MAIN_HAND||e.getEntity().isSpectator()||HerbSpecies.input(e.getItemStack())==HerbSpecies.UNKNOWN||e.getFace()!=Direction.UP||!HerbWorkpieceBlock.surface(e.getLevel(),e.getPos())||!e.getLevel().isEmptyBlock(e.getPos().above()))return;
        if(e.getLevel() instanceof ServerLevel l&&!place(l,e.getPos(),e.getEntity(),e.getItemStack()))return;
        e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
    }
    public static void left(PlayerInteractEvent.LeftClickBlock e){
        if(e.isCanceled()||!e.getLevel().getBlockState(e.getPos()).is(HerbContent.WORKPIECE.get()))return;
        // Match Thermal's START path: client must still send the interaction to server.
        if(!(e.getLevel() instanceof ServerLevel l))return;
        e.setCanceled(true);
        if(e.getAction()!=PlayerInteractEvent.LeftClickBlock.Action.START||!(e.getEntity() instanceof ServerPlayer p)||p.isSpectator()||!l.mayInteract(p,e.getPos())||!p.getAbilities().mayBuild)return;
        HitResult trace=p.pick(p.blockInteractionRange(),1f,false);
        if(!(trace instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(e.getPos())||hit.getDirection()!=Direction.UP)return;
        Vec3 v=hit.getLocation();int cell=HerbMask.cell(v.x-e.getPos().getX(),v.z-e.getPos().getZ());
        if(cell<0)return;
        String action=strike(l,e.getPos(),p,cell,p.isShiftKeyDown());
        if("changed".equals(action)&&l.getBlockEntity(e.getPos()) instanceof HerbWorkpieceEntity work){
            p.displayClientMessage(Component.translatable(HerbMask.finished(work.mask())?"message.traumacraft.herbs_ready":"message.traumacraft.herbs_progress",HerbMask.progress(work.mask())),true);
            l.playSound(null,e.getPos(),SoundEvents.GRASS_BREAK,SoundSource.BLOCKS,.55f,.95f);
        }
    }
    public static String strike(ServerLevel l,BlockPos at,Player p,int cell,boolean lift){
        if(cell<0||cell>=16||p.isSpectator()||!p.getAbilities().mayBuild||!l.mayInteract(p,at)||!(l.getBlockEntity(at) instanceof HerbWorkpieceEntity work))return "ignored";
        if(p instanceof ServerPlayer sp&&dev.minemods.traumacraft.integration.WorldLogicBridge.functionsEnabled(sp)&&dev.minemods.traumacraft.integration.WorldLogicBridge.miningFactor(sp)<=0)return "ignored";
        ItemStack hand=p.getMainHandItem();boolean tool=hand.is(Items.FLINT)||hand.is(Items.COBBLESTONE);
        if(!hand.isEmpty()&&!tool)return "ignored";
        if(work.washed()){
            if(work.emptySurface()||lift||work.drySeconds()>=HerbSpecies.DRY_SECONDS){l.removeBlock(at,false);return "removed";}
            if(p instanceof ServerPlayer sp&&sp.connection!=null)sp.displayClientMessage(Component.translatable("message.traumacraft.herb_drying",work.drySeconds()*100/HerbSpecies.DRY_SECONDS),true);
            return "ignored";
        }
        if(lift||HerbMask.finished(work.mask())){l.removeBlock(at,false);return "removed";}
        int next=HerbMask.crush(work.mask(),cell,tool);
        if(next==work.mask())return "ignored";
        work.update(next);return "changed";
    }
}
