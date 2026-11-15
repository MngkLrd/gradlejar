package dev.creeperarmor.gameplay;
import dev.creeperarmor.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.ExplosionEvent;
public final class CreeperGameplayEvents {
    private static final EquipmentSlot[] ORDER={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
    @SubscribeEvent public void onExplosion(ExplosionEvent.Detonate e){
        if(!(e.getLevel() instanceof ServerLevel))return;
        e.getAffectedEntities().removeIf(entity->{
            if(!(entity instanceof ServerPlayer p))return false;
            if(BlastProtection.isOwnBlastActive(p))return true;
            if(!hasCompleteSet(p))return false;
            for(EquipmentSlot slot:ORDER){ItemStack s=p.getItemBySlot(slot);if(!s.isEmpty())s.hurtAndBreak(4,p,x->x.broadcastBreakEvent(slot));}
            return true;
        });
    }
    @SubscribeEvent public void onAttack(LivingAttackEvent e){
        if(e.getEntity() instanceof ServerPlayer p && e.getSource().is(DamageTypeTags.IS_EXPLOSION)
           && (BlastProtection.isOwnBlastActive(p)||hasCompleteSet(p)))e.setCanceled(true);
    }
    @SubscribeEvent public void onDamage(LivingDamageEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level))return;
        if(e.getAmount()<=0 || p.getHealth()-e.getAmount()>=2F)return;
        EquipmentSlot slot=nextPiece(p);if(slot==null)return;
        e.setCanceled(true);
        if(p.getHealth()<2)p.setHealth(2);
        p.setItemSlot(slot,ItemStack.EMPTY);
        level.playSound(null,p.blockPosition(),SoundEvents.ITEM_BREAK,SoundSource.PLAYERS,1F,.85F);
        BlastProtection.explodeSafely(level,p,new Vec3(p.getX(),p.getY()+p.getBbHeight()*.5,p.getZ()),2.5F);
    }
    @SubscribeEvent public void onDrops(LivingDropsEvent e){
        if(!(e.getEntity() instanceof Creeper c) || !(c.level() instanceof ServerLevel level))return;
        int count=c.isPowered()?2:1;
        e.getDrops().add(new ItemEntity(level,c.getX(),c.getY()+.3,c.getZ(),new ItemStack(ModItems.CREEPER_PLATE.get(),count)));
        e.getDrops().add(new ItemEntity(level,c.getX(),c.getY()+.3,c.getZ(),new ItemStack(ModItems.CREEPER_POWDER.get(),count)));
    }
    private static boolean hasCompleteSet(ServerPlayer p){return p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CREEPER_HELMET.get())&&p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CREEPER_CHESTPLATE.get())&&p.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.CREEPER_LEGGINGS.get())&&p.getItemBySlot(EquipmentSlot.FEET).is(ModItems.CREEPER_BOOTS.get());}
    private static EquipmentSlot nextPiece(ServerPlayer p){
        for(EquipmentSlot s:ORDER){ItemStack v=p.getItemBySlot(s);
            if(s==EquipmentSlot.HEAD && v.is(ModItems.CREEPER_HELMET.get()))return s;
            if(s==EquipmentSlot.CHEST && v.is(ModItems.CREEPER_CHESTPLATE.get()))return s;
            if(s==EquipmentSlot.LEGS && v.is(ModItems.CREEPER_LEGGINGS.get()))return s;
            if(s==EquipmentSlot.FEET && v.is(ModItems.CREEPER_BOOTS.get()))return s;}
        return null;
    }
}
