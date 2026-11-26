package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Keep internal output unmodified so the vanilla recipe output comparison still works. */
@Mixin(value=AbstractFurnaceBlockEntity.class,remap=false)
public abstract class FurnaceFoodMixin {
 @Unique private int worldlogic$before;
 @Inject(method="serverTick",at=@At("HEAD"),remap=false)
 private static void worldlogic$before(Level level,BlockPos pos,BlockState state,AbstractFurnaceBlockEntity furnace,CallbackInfo ci){
  ((FurnaceFoodMixin)(Object)furnace).worldlogic$before=furnace.getItem(2).getCount();
 }
 @Inject(method="serverTick",at=@At("RETURN"),remap=false)
 private static void worldlogic$after(Level level,BlockPos pos,BlockState state,AbstractFurnaceBlockEntity furnace,CallbackInfo ci){
  if(!Rules.on("hot_meal")||!(level instanceof ServerLevel l))return;
  if(furnace.getItem(2).has(net.minecraft.core.component.DataComponents.FOOD)&&furnace.getItem(2).getCount()>((FurnaceFoodMixin)(Object)furnace).worldlogic$before){
   furnace.getPersistentData().putLong(HotFood.KEY,HotFood.now(l)+1200);furnace.setChanged();
  }
 }
 @Inject(method="removeItem",at=@At("RETURN"),remap=false)
 private void worldlogic$extract(int slot,int count,CallbackInfoReturnable<ItemStack> cir){
  if(slot!=2)return;AbstractFurnaceBlockEntity furnace=(AbstractFurnaceBlockEntity)(Object)this;
  if(furnace.getLevel() instanceof ServerLevel l){long until=furnace.getPersistentData().getLong(HotFood.KEY);if(until>HotFood.now(l))HotFood.stamp(cir.getReturnValue(),until);}
 }
 @Inject(method="removeItemNoUpdate",at=@At("RETURN"),remap=false)
 private void worldlogic$extractAll(int slot,CallbackInfoReturnable<ItemStack> cir){worldlogic$extract(slot,64,cir);}
}
