package dev.minemods.worldlogic.mixin.client;
import dev.minemods.worldlogic.ShaderPlantMapping;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Iris 1.21.1 material mapping fallback. Explicit pack mappings take precedence. */
@Pseudo
@Mixin(targets="net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping",remap=false)
abstract class IrisPlantMaterialMixin{
 @Inject(method="createBlockStateIdMap",at=@At("RETURN"),require=0,remap=false)
 private static void worldlogic$inheritGrass(CallbackInfoReturnable<Object2IntMap<BlockState>> cir){ShaderPlantMapping.apply(cir.getReturnValue());}
}
