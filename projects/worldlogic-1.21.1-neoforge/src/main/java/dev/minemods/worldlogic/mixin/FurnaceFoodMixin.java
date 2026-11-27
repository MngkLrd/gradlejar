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
public abstract class FurnaceFoodMixin implements FurnaceHeatAccess {
 @Unique private long worldlogic$hotUntil;
 @Override public long worldlogic$getHotUntil(){return worldlogic$hotUntil;}
 @Override public void worldlogic$setHotUntil(long until){worldlogic$hotUntil=until;}
 @Unique private int worldlogic$before;
 @Inject(method="serverTick",at=@At("HEAD"),remap=false)
 private static void worldlogic$before(Level level,BlockPos pos,BlockState state,AbstractFurnaceBlockEntity furnace,CallbackInfo ci){
  ((FurnaceFoodMixin)(Object)furnace).worldlogic$before=furnace.getItem(2).getCount();
 }
 @Inject(method="serverTick",at=@At("RETURN"),remap=false)
 private static void worldlogic$after(Level level,BlockPos pos,BlockState state,AbstractFurnaceBlockEntity furnace,CallbackInfo ci){
  if(!Rules.on("hot_meal")||!(level instanceof ServerLevel l))return;
  if(furnace.getItem(2).has(net.minecraft.core.component.DataComponents.FOOD)&&furnace.getItem(2).getCount()>((FurnaceFoodMixin)(Object)furnace).worldlogic$before){
   ((FurnaceHeatAccess)(Object)furnace).worldlogic$setHotUntil(HotFood.now(l)+1200);furnace.setChanged();
  }
 }
 @Inject(method="loadAdditional",at=@At("RETURN"),remap=false)
 private void worldlogic$load(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup,CallbackInfo ci){worldlogic$hotUntil=tag.getLong(HotFood.KEY);}
 @Inject(method="saveAdditional",at=@At("RETURN"),remap=false)
 private void worldlogic$save(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup,CallbackInfo ci){tag.putLong(HotFood.KEY,worldlogic$hotUntil);}
}
