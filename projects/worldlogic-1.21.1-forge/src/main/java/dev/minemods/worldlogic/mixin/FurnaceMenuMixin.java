package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=AbstractFurnaceMenu.class,remap=false)
public abstract class FurnaceMenuMixin {
 @Inject(method="quickMoveStack",at=@At("HEAD"),remap=false)
 private void worldlogic$mark(Player p,int index,CallbackInfoReturnable<ItemStack> ci){
  if(index!=2||!(p.level() instanceof ServerLevel l)||!Rules.on("hot_meal"))return;
  Slot slot=((AbstractFurnaceMenu)(Object)this).getSlot(2);
  if(slot.container instanceof AbstractFurnaceBlockEntity furnace){long until=((FurnaceHeatAccess)(Object)furnace).worldlogic$getHotUntil();if(until>HotFood.now(l))HotFood.stamp(slot.getItem(),until);}
 }
 @Inject(method="quickMoveStack",at=@At("RETURN"),remap=false)
 private void worldlogic$clear(Player p,int index,CallbackInfoReturnable<ItemStack> ci){
  if(index!=2||!(p.level() instanceof ServerLevel))return;
  ItemStack stack=((AbstractFurnaceMenu)(Object)this).getSlot(2).getItem();
  if(!stack.isEmpty()&&HotFood.deadline(stack)>0)CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.remove(HotFood.KEY));
 }
}
