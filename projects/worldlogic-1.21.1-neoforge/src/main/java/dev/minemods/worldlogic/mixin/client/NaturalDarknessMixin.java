package dev.minemods.worldlogic.mixin.client;
import dev.minemods.worldlogic.NaturalDarkness;
import dev.minemods.worldlogic.RuleSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
@Mixin(LightTexture.class)
public abstract class NaturalDarknessMixin{
 private static boolean worldlogic$active(){
  var mc=Minecraft.getInstance();
  return mc.level!=null&&mc.level.dimensionType().hasSkyLight()&&mc.player!=null&&RuleSync.clientDarkness()&&!mc.player.hasEffect(MobEffects.NIGHT_VISION);
 }
 @Redirect(method="updateLightTexture",at=@At(value="INVOKE",target="Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
 private Object worldlogic$limitGamma(OptionInstance<?> option){
  Object value=option.get();
  if(worldlogic$active()&&option==Minecraft.getInstance().options.gamma()&&value instanceof Double gamma)return NaturalDarkness.gamma(gamma);
  return value;
 }
 @ModifyArg(method="updateLightTexture",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/NativeImage;setPixelRGBA(III)V"),index=2)
 private int worldlogic$dimLowLight(int color){return worldlogic$active()?NaturalDarkness.darken(color):color;}
}
