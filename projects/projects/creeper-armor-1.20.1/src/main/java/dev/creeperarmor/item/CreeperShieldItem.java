package dev.creeperarmor.item;
import dev.creeperarmor.gameplay.BlastProtection;
import dev.creeperarmor.registry.ModItems;
import dev.creeperarmor.client.CreeperShieldRenderer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;
public final class CreeperShieldItem extends ShieldItem {
    public CreeperShieldItem(Properties props){super(props);}
    @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){
        if(!(level instanceof ServerLevel server) || !(user instanceof ServerPlayer p))return;
        if(stack.getUseDuration()-remaining!=12)return;
        EquipmentSlot slot=p.getUsedItemHand()==InteractionHand.OFF_HAND?EquipmentSlot.OFFHAND:EquipmentSlot.MAINHAND;
        if(stack.getMaxDamage()-stack.getDamageValue()<50)return;
        BlastProtection.explodeSafely(server,p,p.getEyePosition().add(p.getLookAngle().scale(2.4)),1.8F);
        stack.hurtAndBreak(50,p,u->u.broadcastBreakEvent(slot));
    }
    @Override public boolean isValidRepairItem(ItemStack a,ItemStack b){return b.is(ModItems.CREEPER_PLATE.get());}
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer){
        consumer.accept(new IClientItemExtensions(){
            private BlockEntityWithoutLevelRenderer render;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(render==null)render=new CreeperShieldRenderer();return render;
            }
        });
    }
}
