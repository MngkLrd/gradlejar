package dev.minemods.nofoodinfurnace.mixin;
import dev.minemods.nofoodinfurnace.FoodRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 26.x canBurn has no furnace argument. Scope blocking to the ordinary furnace tick only. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class OrdinaryFurnaceMixin {
 private static final ThreadLocal<Boolean> nofoodinfurnace$blocked=ThreadLocal.withInitial(()->false);
 @Inject(method="serverTick",at=@At("HEAD"))
 private static void nofoodinfurnace$begin(ServerLevel level,BlockPos pos,BlockState state,AbstractFurnaceBlockEntity furnace,CallbackInfo ci){
   nofoodinfurnace$blocked.set(state.is(Blocks.FURNACE) && FoodRules.forbidden(level,furnace.getItem(0)));
 }
 @Inject(method="canBurn",at=@At("HEAD"),cancellable=true)
 private static void nofoodinfurnace$deny(CallbackInfoReturnable<Boolean> cir){
   if(nofoodinfurnace$blocked.get())cir.setReturnValue(false);
 }
 @Inject(method="serverTick",at=@At("RETURN"))
 private static void nofoodinfurnace$end(ServerLevel level,BlockPos pos,BlockState state,AbstractFurnaceBlockEntity furnace,CallbackInfo ci){
   nofoodinfurnace$blocked.remove();
 }
}
