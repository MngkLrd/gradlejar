package dev.minemods.traumacraft.event;

import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.climb.ClimbManager;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class FunctionEvents {
    private static final ResourceLocation ATTACK=ResourceLocation.fromNamespaceAndPath("traumacraft","arm_attack_speed");
    private FunctionEvents(){}
    public static void update(ServerPlayer p){
        var a=p.getAttribute(Attributes.ATTACK_SPEED);
        boolean active=WorldLogicBridge.functionsEnabled(p);
        float speed=active?PhysicalFunction.attackSpeed(BodyStates.get(p),WorldLogicBridge.mainArm(p)):1;
        if(a!=null){
            var old=a.getModifier(ATTACK);
            double amount=speed-1;
            if(old==null||Math.abs(old.amount()-amount)>.001){
                if(old!=null)a.removeModifier(ATTACK);
                if(amount<-.001)a.addTransientModifier(new AttributeModifier(ATTACK,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        if(active&&!PhysicalFunction.canSprint(BodyStates.get(p)))p.setSprinting(false);
    }
    public static void attack(AttackEntityEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&(ClimbManager.active(p)||WorldLogicBridge.functionsEnabled(p)&&WorldLogicBridge.strikeFactor(p)<=0))e.setCanceled(true);
    }
    public static void breakBlock(BlockEvent.BreakEvent e){
        if(e.getPlayer() instanceof ServerPlayer p&&(ClimbManager.active(p)||WorldLogicBridge.functionsEnabled(p)&&WorldLogicBridge.miningFactor(p)<=0))e.setCanceled(true);
    }
    public static void jump(LivingEvent.LivingJumpEvent e){
        if(e.getEntity() instanceof ServerPlayer p&&WorldLogicBridge.functionsEnabled(p)){
            float factor=PhysicalFunction.jump(BodyStates.get(p));
            if(factor<.999f){var v=p.getDeltaMovement();p.setDeltaMovement(v.x,v.y*factor,v.z);p.hurtMarked=true;}
        }
    }
    public static boolean twoHanded(ItemStack stack){return stack.is(Items.BOW)||stack.is(Items.CROSSBOW);}
    public static void rightClickItem(PlayerInteractEvent.RightClickItem e){
        if(e.getEntity() instanceof ServerPlayer p&&(ClimbManager.active(p)||WorldLogicBridge.functionsEnabled(p)&&!PhysicalFunction.canUse(BodyStates.get(p),WorldLogicBridge.armForHand(p,e.getHand()),twoHanded(e.getItemStack()))))e.setCanceled(true);
    }
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock e){
        if(e.getEntity() instanceof ServerPlayer p&&(ClimbManager.active(p)||WorldLogicBridge.functionsEnabled(p)&&!PhysicalFunction.canUse(BodyStates.get(p),WorldLogicBridge.armForHand(p,e.getHand()),twoHanded(e.getItemStack()))))e.setCanceled(true);
    }
}
