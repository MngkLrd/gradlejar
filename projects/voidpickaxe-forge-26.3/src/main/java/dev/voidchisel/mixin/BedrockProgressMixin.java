package dev.voidchisel.mixin;
import dev.voidchisel.VoidPickaxe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BedrockProgressMixin {
    @Inject(method="getDestroyProgress",at=@At("HEAD"),cancellable=true)
    private void voidpickaxe$mine(Player player, BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> cir){
        if (((BlockState)(Object)this).is(Blocks.BEDROCK)
            && !player.isCreative()
            && !player.isSpectator()
            && player.getMainHandItem().is(VoidPickaxe.PICKAXE.get()))
            cir.setReturnValue(0.125F);
    }
}
