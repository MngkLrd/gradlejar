package com.kirill.simplesorter.client;

import com.kirill.simplesorter.network.WandActionPayload;
import com.kirill.simplesorter.network.WandSettingsPayload;
import com.kirill.simplesorter.network.WandSuppliesPayload;
import com.kirill.simplesorter.settings.SortingMode;
import com.kirill.simplesorter.settings.SortingWandSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.kirill.simplesorter.network.ClientNetwork;

public class WandSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 410;
    private static final int PANEL_HEIGHT = 390;
    private static final int BUTTON_HEIGHT = 20;

    private static final int BACKGROUND = 0xFF070503;
    private static final int PANEL_DARK = 0xFF18140F;
    private static final int PANEL_MID = 0xFF221C14;
    private static final int CARD = 0xFF17130F;
    private static final int CARD_HOVER = 0xFF2B241A;
    private static final int OUTLINE = 0xFF7B6040;
    private static final int TEXT = 0xFFEDE2CC;
    private static final int MUTED = 0xFFA79D8F;
    private static final int DIM = 0xFF786F63;
    private static final int GOLD = 0xFFFFC45E;
    private static final int GREEN = 0xFF78E083;
    private static final int RED = 0xFFFF7777;
    private static final int BLUE = 0xFF79CCFF;
    private static final int PINK = 0xFFFF93DA;

    private final InteractionHand hand;
    private SortingWandSettings settings;

    public WandSettingsScreen(ItemStack stack, InteractionHand hand) {
        super(Component.translatable("screen.simplesorter.wand_settings.title"));
        this.hand = hand;
        this.settings = SortingWandSettings.readFrom(stack);
    }

    @Override
    protected void init() {
        rebuildButtons();
    }


    private void rebuildButtons() {
        clearWidgets();

        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        int left = panelX + 18;
        int right = panelX + PANEL_WIDTH - 18;

        int modeY = panelY + 70;
        int modeW = (right - left - 8) / 2;
        // Smart Categories includes stack merging and inventory compaction.
        // The old separate Compact Stacks option is kept only as a saved-mode
        // compatibility alias, not a redundant GUI action.
        addRenderableWidget(modeButton(SortingMode.SMART_CATEGORIES, left, modeY, modeW));
        addRenderableWidget(modeButton(SortingMode.MATCH_EXISTING, left + modeW + 8, modeY, modeW));
        addRenderableWidget(modeButton(SortingMode.PLAYER_DEPOSIT, left, modeY + 25, right - left));

        int radiusY = panelY + 156;
        addRenderableWidget(button(Component.literal("-"), left, radiusY, 34, BUTTON_HEIGHT, BLUE, false, ignored -> {
            settings = settings.previousRadius();
            rebuildButtons();
        }));
        addRenderableWidget(button(radiusLabel(), left + 42, radiusY, right - left - 84, BUTTON_HEIGHT, BLUE, true, ignored -> {
            settings = settings.nextRadius();
            rebuildButtons();
        }));
        addRenderableWidget(button(Component.literal("+"), right - 34, radiusY, 34, BUTTON_HEIGHT, BLUE, false, ignored -> {
            settings = settings.nextRadius();
            rebuildButtons();
        }));

        int toggleY = panelY + 215;
        int toggleW = (right - left - 12) / 3;
        addRenderableWidget(button(tinyToggle("screen.simplesorter.wand_settings.show_radius.short", settings.showRadius()), left, toggleY, toggleW, BUTTON_HEIGHT, settings.showRadius() ? GREEN : RED, settings.showRadius(), ignored -> {
            settings = settings.withShowRadius(!settings.showRadius());
            rebuildButtons();
        }));
        addRenderableWidget(button(tinyToggle("screen.simplesorter.wand_settings.highlight_chests.short", settings.showContainerHighlights()), left + toggleW + 6, toggleY, toggleW, BUTTON_HEIGHT, settings.showContainerHighlights() ? GREEN : RED, settings.showContainerHighlights(), ignored -> {
            settings = settings.withShowContainerHighlights(!settings.showContainerHighlights());
            rebuildButtons();
        }));
        addRenderableWidget(button(tinyToggle("screen.simplesorter.wand_settings.transfer_fx.short", settings.showTransferParticles()), left + (toggleW + 6) * 2, toggleY, toggleW, BUTTON_HEIGHT, settings.showTransferParticles() ? GREEN : RED, settings.showTransferParticles(), ignored -> {
            settings = settings.withShowTransferParticles(!settings.showTransferParticles());
            rebuildButtons();
        }));

        int labelY = panelY + 247;
        int extraW = (right - left - 12) / 3;
        addRenderableWidget(button(
                Component.literal("Auto-Organize: " + (settings.autoOrganize() ? "ON" : "OFF")),
                left, labelY, extraW, BUTTON_HEIGHT,
                settings.autoOrganize() ? GREEN : RED, settings.autoOrganize(), ignored -> {
                    settings = settings.withAutoOrganize(!settings.autoOrganize());
                    rebuildButtons();
                }));
        addRenderableWidget(button(
                Component.literal("Labels: " + switch (settings.labelMode()) {
                    case 1 -> "Text"; case 2 -> "Icons"; default -> "OFF";
                }),
                left + extraW + 6, labelY, extraW, BUTTON_HEIGHT,
                settings.labelMode() == 0 ? MUTED : GOLD, settings.labelMode() != 0, ignored -> {
                    settings = settings.nextLabelMode();
                    rebuildButtons();
                }));
        addRenderableWidget(button(Component.literal("Load Labels"),
                left + (extraW + 6) * 2, labelY, extraW, BUTTON_HEIGHT, BLUE, false, ignored -> {
                    save(false);
                    ClientNetwork.send(new WandSuppliesPayload(hand == InteractionHand.OFF_HAND ? 1 : 0));
                }));

        int actionY = panelY + 307;
        addRenderableWidget(button(Component.translatable("screen.simplesorter.wand_settings.run_selected"), left, actionY, 118, BUTTON_HEIGHT, modeAccent(settings.mode()), true, ignored -> run(settings.mode(), true)));
        addRenderableWidget(button(Component.translatable("screen.simplesorter.wand_settings.deposit_now"), left + 126, actionY, 126, BUTTON_HEIGHT, PINK, false, ignored -> run(SortingMode.PLAYER_DEPOSIT, true)));
        addRenderableWidget(button(Component.translatable("screen.simplesorter.wand_settings.save"), right - 86, actionY, 86, BUTTON_HEIGHT, GREEN, false, ignored -> {
            save(false);
            onClose();
        }));

        int bottomY = panelY + PANEL_HEIGHT - 28;
        addRenderableWidget(button(Component.translatable("screen.simplesorter.wand_settings.defaults"), left, bottomY, 86, BUTTON_HEIGHT, GOLD, false, ignored -> {
            settings = SortingWandSettings.defaults();
            rebuildButtons();
        }));
        addRenderableWidget(button(Component.translatable("screen.simplesorter.wand_settings.reload"), left + 94, bottomY, 104, BUTTON_HEIGHT, BLUE, false, ignored -> save(true)));
        addRenderableWidget(button(Component.translatable("gui.cancel"), right - 72, bottomY, 72, BUTTON_HEIGHT, RED, false, ignored -> onClose()));
    }

    private StyledButton modeButton(SortingMode mode, int x, int y, int w) {
        boolean selected = settings.mode() == mode;
        return button(Component.translatable(mode.translationKey()), x, y, w, BUTTON_HEIGHT, modeAccent(mode), selected, ignored -> {
            settings = settings.withMode(mode);
            rebuildButtons();
        });
    }

    private StyledButton button(Component label, int x, int y, int w, int h, int accent, boolean selected, Press press) {
        return new StyledButton(x, y, w, h, label, accent, selected, press);
    }

    private Component radiusLabel() {
        return Component.translatable("screen.simplesorter.wand_settings.radius_value", settings.radius());
    }

    private Component tinyToggle(String key, boolean enabled) {
        return Component.translatable(key)
                .append(Component.literal(" "))
                .append(Component.translatable(enabled ? "screen.simplesorter.wand_settings.on" : "screen.simplesorter.wand_settings.off")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private void save(boolean reloadRules) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            ItemStack stack = minecraft.player.getItemInHand(hand);
            settings.writeTo(stack);
        }

        ClientNetwork.send(WandSettingsPayload.save(hand, settings, reloadRules));
    }

    private void run(SortingMode actionMode, boolean closeAfter) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            ItemStack stack = minecraft.player.getItemInHand(hand);
            settings.writeTo(stack);
        }

        ClientNetwork.send(WandActionPayload.run(hand, settings, actionMode));
        if (closeAfter) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, BACKGROUND);

        int panelX = (width - PANEL_WIDTH) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        int left = panelX + 18;
        int right = panelX + PANEL_WIDTH - 18;

        drawPanel(graphics, panelX, panelY);

        graphics.drawString(font, title, panelX + 18, panelY + 13, TEXT, false);
        graphics.drawString(font, Component.translatable("screen.simplesorter.wand_settings.subtitle"), panelX + 18, panelY + 28, MUTED, false);
        drawModeBadge(graphics, right - 132, panelY + 13, 114, Component.translatable(settings.mode().translationKey()), modeAccent(settings.mode()));

        drawSectionLabel(graphics, Component.translatable("screen.simplesorter.wand_settings.section.behavior"), left, panelY + 51, modeAccent(settings.mode()));
        drawModeInfo(graphics, left, panelY + 121, right - left);

        drawSectionLabel(graphics, Component.translatable("screen.simplesorter.wand_settings.section.radius"), left, panelY + 140, BLUE);
        drawRadiusScale(graphics, left, panelY + 184, right - left);

        drawSectionLabel(graphics, Component.translatable("screen.simplesorter.wand_settings.section.visuals"), left, panelY + 199, GREEN);
        drawKeybindInfo(graphics, left, panelY + 283, right - left);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 4, y - 4, x + PANEL_WIDTH + 4, y + PANEL_HEIGHT + 4, 0xFF000000);
        graphics.fill(x - 1, y - 1, x + PANEL_WIDTH + 1, y + PANEL_HEIGHT + 1, OUTLINE);
        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, PANEL_DARK);
        graphics.fill(x + 1, y + 1, x + PANEL_WIDTH - 1, y + 47, 0xFF2D2216);
        graphics.fill(x + 1, y + 47, x + PANEL_WIDTH - 1, y + PANEL_HEIGHT - 1, PANEL_MID);
        graphics.fill(x + 7, y + 50, x + 10, y + PANEL_HEIGHT - 8, GOLD);
        graphics.fill(x + PANEL_WIDTH - 10, y + 50, x + PANEL_WIDTH - 7, y + PANEL_HEIGHT - 8, 0xFF463222);
        graphics.fill(x + 18, y + 47, x + PANEL_WIDTH - 18, y + 48, 0xFFFFC45E);
    }

    private void drawSectionLabel(GuiGraphics graphics, Component label, int x, int y, int accent) {
        graphics.drawString(font, label, x, y, accent, false);
        graphics.fill(x, y + 11, x + 118, y + 12, withAlpha(accent, 0x66));
    }

    private void drawModeInfo(GuiGraphics graphics, int x, int y, int w) {
        graphics.fill(x, y, x + w, y + 24, CARD);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 23, 0xFF261F17);
        graphics.fill(x + 2, y + 2, x + 5, y + 22, modeAccent(settings.mode()));
        graphics.drawString(font, Component.translatable(settings.mode().descriptionKey()), x + 12, y + 8, MUTED, false);
    }

    private void drawModeBadge(GuiGraphics graphics, int x, int y, int w, Component label, int accent) {
        graphics.fill(x, y, x + w, y + 18, 0xFF100D09);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 17, 0xFF2A2117);
        graphics.fill(x + 1, y + 1, x + 4, y + 17, accent);
        graphics.drawCenteredString(font, label, x + w / 2 + 2, y + 5, TEXT);
    }

    private void drawRadiusScale(GuiGraphics graphics, int x, int y, int w) {
        int barY = y;
        graphics.fill(x, barY, x + w, barY + 4, 0xFF15110D);
        graphics.fill(x + 1, barY + 1, x + w - 1, barY + 3, 0xFF413626);

        int[] steps = SortingWandSettings.RADIUS_STEPS;
        for (int i = 0; i < steps.length; i++) {
            int px = x + (int) ((w - 4) * (i / (double) (steps.length - 1)));
            boolean selected = steps[i] == settings.radius();
            int color = selected ? BLUE : DIM;
            graphics.fill(px, barY - 4, px + 4, barY + 8, color);
            graphics.drawCenteredString(font, Component.literal(String.valueOf(steps[i])), px + 2, barY + 12, color);
        }
    }

    private void drawKeybindInfo(GuiGraphics graphics, int x, int y, int w) {
        Component open = ClientKeyMappings.OPEN_SETTINGS.getTranslatedKeyMessage();
        Component sort = ClientKeyMappings.QUICK_SORT.getTranslatedKeyMessage();
        Component line = Component.translatable("screen.simplesorter.wand_settings.keys.line", open, sort);
        graphics.drawString(font, line, x, y, MUTED, false);
    }

    private int modeAccent(SortingMode mode) {
        return switch (mode) {
            case MATCH_EXISTING -> GREEN;
            case SMART_CATEGORIES -> GOLD;
            case COMPACT_STACKS -> BLUE;
            case PLAYER_DEPOSIT -> PINK;
        };
    }

    private static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @FunctionalInterface
    private interface Press {
        void onPress(StyledButton button);
    }

    private final class StyledButton extends AbstractButton {
        private final int accent;
        private final boolean selected;
        private final Press press;

        private StyledButton(int x, int y, int width, int height, Component message, int accent, boolean selected, Press press) {
            super(x, y, width, height, message);
            this.accent = accent;
            this.selected = selected;
            this.press = press;
        }

        @Override
        public void onPress() {
            press.onPress(this);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            int x = getX();
            int y = getY();
            int bg = hovered ? CARD_HOVER : CARD;
            int border = selected ? accent : OUTLINE;
            int textColor = active ? (selected ? 0xFFFFFFFF : TEXT) : DIM;

            graphics.fill(x, y, x + getWidth(), y + getHeight(), border);
            graphics.fill(x + 1, y + 1, x + getWidth() - 1, y + getHeight() - 1, bg);
            graphics.fill(x + 2, y + 2, x + 5, y + getHeight() - 2, accent);
            if (selected) {
                graphics.fill(x + getWidth() - 5, y + 2, x + getWidth() - 2, y + getHeight() - 2, accent);
            }

            graphics.drawCenteredString(font, getMessage(), x + getWidth() / 2, y + (getHeight() - 8) / 2, textColor);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
