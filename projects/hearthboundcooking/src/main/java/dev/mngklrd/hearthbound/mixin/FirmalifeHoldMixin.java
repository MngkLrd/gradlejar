package dev.mngklrd.hearthbound.mixin;
import dev.mngklrd.hearthbound.HoldTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.eerussianguy.firmalife.common.blockentities.OvenTopBlockEntity",remap=false)
public abstract class FirmalifeHoldMixin {
    @Inject(method="serverTick",at=@At("RETURN"))
    private static void hearthbound$hold(Level level,BlockPos pos,BlockState state,@Coerce BlockEntity oven,CallbackInfo ci){
        if(HoldTracker.tick(oven,0,4)&&oven instanceof InventoryBlockEntity<?> inv)inv.markForSync();
    }
}
