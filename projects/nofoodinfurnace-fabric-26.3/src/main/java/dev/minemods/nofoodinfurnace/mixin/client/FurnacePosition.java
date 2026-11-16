package dev.minemods.nofoodinfurnace.mixin.client;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractContainerScreen.class)
public interface FurnacePosition {
  @Accessor("leftPos") int nofoodinfurnace$getLeft();
  @Accessor("topPos") int nofoodinfurnace$getTop();
}
