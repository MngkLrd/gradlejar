package dev.minemods.worldlogic.mixin.client;
import dev.minemods.worldlogic.client.TabRail;
import dev.minemods.worldlogic.client.compat.NativeTraumaView;
import dev.minemods.worldlogic.TabLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Optional compatibility with Trauma's existing interactive inventory/anatomy screen. */
@Pseudo
@Mixin(targets="dev.minemods.traumacraft.client.gui.TraumaInventoryScreen",remap=false)
abstract class NativeTraumaScreenMixin implements NativeTraumaView {
 @Shadow(remap=false) private boolean anatomyOpen;
 @Override public boolean worldlogic$anatomy(){return anatomyOpen;}
 @Override public void worldlogic$openAnatomy(){anatomyOpen=true;}
 @ModifyArg(method="init",at=@At(value="INVOKE",target="Ldev/minemods/traumacraft/client/gui/MedicalDashboardLayout;of(II)Ldev/minemods/traumacraft/client/gui/MedicalDashboardLayout;",remap=false),index=0,require=0,remap=false)
 private int worldlogic$reserveRail(int width){return TabLayout.nativeContentWidth(width);}
 @Inject(method="tab",at=@At("HEAD"),cancellable=true,require=0,remap=false)
 private void worldlogic$noDuplicate(GuiGraphics g,int mx,int my,CallbackInfo ci){
  if(!anatomyOpen&&TabRail.replacesLegacy((Screen)(Object)this,"worldlogic_trauma"))ci.cancel();
 }
 @Inject(method="onTab",at=@At("HEAD"),cancellable=true,require=0,remap=false)
 private void worldlogic$noDuplicateClick(double x,double y,CallbackInfoReturnable<Boolean> cir){
  if(!anatomyOpen&&TabRail.replacesLegacy((Screen)(Object)this,"worldlogic_trauma"))cir.setReturnValue(false);
 }
}
