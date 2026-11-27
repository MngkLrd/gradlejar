package dev.minemods.traumacraft.item;

import dev.minemods.traumacraft.content.HerbSpecies;
import dev.minemods.traumacraft.registry.ModItems;
import dev.minemods.traumacraft.health.BodyStates;
import dev.minemods.traumacraft.health.PhysicalFunction;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

/** Typed botanical material is deliberately separate from legacy generic crushed herbs. */
public final class BotanicalPowderItem extends Item {
    public BotanicalPowderItem(Properties p){super(p);}
    public static ItemStack prepare(ItemStack powder,ItemStack dressing){
        if(!powder.is(ModItems.BOTANICAL_POWDER.get())||!dressing.is(ModItems.RAG_DRESSING.get()))return ItemStack.EMPTY;
        var species=HerbSpecies.read(powder);
        // Only the existing closed-bruise preparation is supported in this slice.
        if(species!=HerbSpecies.CHAMOMILE)return ItemStack.EMPTY;
        var out=new ItemStack(ModItems.HERBAL_POULTICE.get());
        HerbSpecies.write(out,species,HerbSpecies.dry(powder));return out;
    }
    public static boolean mix(Player player){
        if(player.isSpectator())return false;
        if(player instanceof ServerPlayer p&&WorldLogicBridge.functionsEnabled(p)&&
                (!PhysicalFunction.canUse(BodyStates.get(p),WorldLogicBridge.armForHand(p,InteractionHand.MAIN_HAND),false)||
                 !PhysicalFunction.canUse(BodyStates.get(p),WorldLogicBridge.armForHand(p,InteractionHand.OFF_HAND),false)))return false;
        var powder=player.getMainHandItem();var dressing=player.getOffhandItem();var out=prepare(powder,dressing);
        if(out.isEmpty())return false;
        if(!player.getAbilities().instabuild){powder.shrink(1);dressing.shrink(1);}
        if(!player.getInventory().add(out))player.drop(out,false);
        player.getInventory().setChanged();return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(hand!=InteractionHand.MAIN_HAND||player.isSpectator()||prepare(stack,player.getOffhandItem()).isEmpty())return InteractionResultHolder.pass(stack);
        if(!level.isClientSide()&&!mix(player))return InteractionResultHolder.fail(stack);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide());
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(s,c,lines,flag);HerbSpecies.tooltip(s,lines);
        lines.add(Component.translatable(HerbSpecies.read(s)==HerbSpecies.CHAMOMILE?"tooltip.traumacraft.chamomile_mix":"tooltip.traumacraft.botanical_pending"));
    }
}
