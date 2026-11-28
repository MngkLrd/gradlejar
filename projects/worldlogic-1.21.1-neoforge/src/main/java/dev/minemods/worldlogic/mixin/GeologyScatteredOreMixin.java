package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.GeologyGeneration;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ScatteredOreFeature.class)
public abstract class GeologyScatteredOreMixin {
 @Inject(method="place",at=@At("HEAD"),cancellable=true)
 private void worldlogic$replaceVanillaOre(FeaturePlaceContext<OreConfiguration> context,CallbackInfoReturnable<Boolean> cir){
  if(GeologyGeneration.enabled(context.chunkGenerator())&&GeologyGeneration.replaces(context.config()))cir.setReturnValue(false);
 }
}
