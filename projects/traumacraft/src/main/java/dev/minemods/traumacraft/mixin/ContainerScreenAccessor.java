package dev.minemods.traumacraft.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos")
    int traumacraft$getLeftPos();
    @Accessor("leftPos")
    void traumacraft$setLeftPos(int value);
}
