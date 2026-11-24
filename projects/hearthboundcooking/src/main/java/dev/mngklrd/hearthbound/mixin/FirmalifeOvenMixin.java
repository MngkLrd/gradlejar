package dev.mngklrd.hearthbound.mixin;
import dev.mngklrd.hearthbound.Quality;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets="com.eerussianguy.firmalife.common.blockentities.OvenTopBlockEntity",remap=false)
public abstract class FirmalifeOvenMixin {
    @Inject(method="getTicksLeft",at=@At("HEAD"),cancellable=true)
    private void hearthbound$hideTimer(int slot,CallbackInfoReturnable<Integer> cir){
        BlockEntity be=(BlockEntity)(Object)this;
        be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h->{if(slot>=0&&slot<h.getSlots()&&Quality.food(h.getStackInSlot(slot)))cir.setReturnValue(-1);});
    }
}
