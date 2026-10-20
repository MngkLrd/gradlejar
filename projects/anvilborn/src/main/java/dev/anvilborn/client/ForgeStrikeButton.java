package dev.anvilborn.client;

import dev.anvilborn.Anvilborn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** One focusable, keyboard-accessible button; all states live in the pixel-art widget atlas. */
public final class ForgeStrikeButton extends AbstractButton {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Anvilborn.MODID, "textures/gui/forge_widgets.png");
    private final Runnable action;
    private int pressedFrames;

    public ForgeStrikeButton(int x, int y, int width, int height, Component label, Runnable action) {
        super(x, y, width, height, label);
        this.action = action;
    }

    @Override
    public void onPress() {
        if (!active) return;
        pressedFrames = 5;
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int sourceY = !active ? 72 : pressedFrames > 0 ? 48 : isHoveredOrFocused() ? 24 : 0;
        graphics.blit(TEXTURE, getX(), getY(), 0, sourceY, getWidth(), getHeight(), 256, 128);
        int color = !active ? 0xA2A29B : isHoveredOrFocused() ? 0xFFF0C9 : 0xEBD8BA;
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, getMessage(),
            getX() + (getWidth() - font.width(getMessage())) / 2,
            getY() + (getHeight() - 8) / 2, color, false);
        if (pressedFrames > 0) pressedFrames--;
    }
}
