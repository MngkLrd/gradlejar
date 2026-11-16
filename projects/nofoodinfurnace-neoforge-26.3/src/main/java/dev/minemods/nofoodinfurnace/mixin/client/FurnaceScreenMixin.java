package dev.minemods.nofoodinfurnace.mixin.client;
import dev.minemods.nofoodinfurnace.FoodRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractFurnaceScreen.class)
public abstract class FurnaceScreenMixin {
 private static final Identifier nofoodinfurnace$CROSS=Identifier.withDefaultNamespace("container/anvil/error");
 @Inject(method="extractBackground",at=@At("TAIL"))
 private void nofoodinfurnace$render(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float a,CallbackInfo ci) {
  if(!((Object)this instanceof FurnaceScreen screen))return;
  var level=Minecraft.getInstance().level;
  if(level==null||!FoodRules.forbidden(level,screen.getMenu().getSlot(0).getItem()))return;
  graphics.blitSprite(RenderPipelines.GUI_TEXTURED,nofoodinfurnace$CROSS,screen.getGuiLeft()+77,screen.getGuiTop()+33,28,21);
 }
}
