package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.HotFood;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=BaseContainerBlockEntity.class,remap=false)
public abstract class FurnaceExtractionMixin {
 @Inject(method="removeItem",at=@At("RETURN"),remap=false)
 private void worldlogic$extract(int slot,int count,CallbackInfoReturnable<ItemStack> cir){worldlogic$stamp(slot,cir.getReturnValue());}
 @Inject(method="removeItemNoUpdate",at=@At("RETURN"),remap=false)
 private void worldlogic$extractAll(int slot,CallbackInfoReturnable<ItemStack> cir){worldlogic$stamp(slot,cir.getReturnValue());}
 @org.spongepowered.asm.mixin.Unique
 private void worldlogic$stamp(int slot,ItemStack stack){
  if(slot!=2||!((Object)this instanceof AbstractFurnaceBlockEntity furnace))return;
  if(furnace.getLevel() instanceof ServerLevel l){long until=((dev.minemods.worldlogic.FurnaceHeatAccess)(Object)furnace).worldlogic$getHotUntil();if(until>HotFood.now(l))HotFood.stamp(stack,until);}
 }
}
