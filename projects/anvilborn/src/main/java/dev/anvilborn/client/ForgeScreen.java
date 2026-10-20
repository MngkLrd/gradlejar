package dev.anvilborn.client;

import dev.anvilborn.Anvilborn;
import dev.anvilborn.forge.Aspect;
import dev.anvilborn.forge.Aspects;
import dev.anvilborn.forge.ForgeData;
import dev.anvilborn.forge.ForgeMenu;
import dev.anvilborn.forge.ForgeRules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Texture-backed forging workbench. All positional constants match ForgeMenu's Slot positions.
 * Data from the menu is read-only; every actual strike is evaluated by the server.
 */
public final class ForgeScreen extends AbstractContainerScreen<ForgeMenu> {
    private static final ResourceLocation BACKGROUND =
        ResourceLocation.fromNamespaceAndPath(Anvilborn.MODID, "textures/gui/forge_screen.png");
    private static final ResourceLocation WIDGETS =
        ResourceLocation.fromNamespaceAndPath(Anvilborn.MODID, "textures/gui/forge_widgets.png");

    private static final int FRAME_W = 230;
    private static final int FRAME_H = 256;
    private static final int BASE_TEXT = 0xE6DDC9;
    private static final int MUTED_TEXT = 0xAFAC9F;

    private ForgeStrikeButton strikeButton;
    private int lastSequence = -1;
    private int feedbackTicks;

    public ForgeScreen(ForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = FRAME_W;
        imageHeight = FRAME_H;
        titleLabelX = 27;
        titleLabelY = 12;
        inventoryLabelX = 34;
        inventoryLabelY = 164;
    }

    @Override
    protected void init() {
        super.init();
        strikeButton = addRenderableWidget(new ForgeStrikeButton(
            leftPos + 15, topPos + 139, 200, 22,
            Component.translatable("button.anvilborn.strike"), () -> {
                if (minecraft != null && minecraft.gameMode != null)
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        ));
        refreshButton();
    }

    private void refreshButton() {
        if (strikeButton == null || minecraft == null || minecraft.player == null) return;
        ItemStack stack = menu.workpiece();
        strikeButton.active = ForgeRules.eligible(stack)
            && ForgeRules.data(stack).level() < ForgeRules.MAX_LEVEL
            && (minecraft.player.getAbilities().instabuild || minecraft.player.experienceLevel > 0);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (lastSequence != menu.sequence()) {
            if (lastSequence >= 0) feedbackTicks = 28;
            lastSequence = menu.sequence();
        }
        if (feedbackTicks > 0) feedbackTicks--;
        refreshButton();
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.blit(BACKGROUND, leftPos, topPos, 0, 0, FRAME_W, FRAME_H, 256, 256);
        renderHammer(gui);
        renderQuality(gui);
        renderRisk(gui);
        renderAspects(gui);
        renderProgress(gui);
        renderStrikeFeedback(gui);
    }

    private void renderHammer(GuiGraphics gui) {
        boolean striking = feedbackTicks > 20;
        int u = striking ? 231 : 206;
        gui.blit(WIDGETS, leftPos + 72, topPos + 38, u, 48, 20, 30, 256, 128);
        if (feedbackTicks > 18) {
            gui.blit(WIDGETS, leftPos + 59, topPos + 51, 206, 88, 22, 20, 256, 128);
        }
    }

    private static String tierKey(int value) {
        if (value < 0) return "damaged";
        if (value == 0) return "common";
        if (value <= 2) return "uncommon";
        if (value <= 4) return "rare";
        if (value <= 7) return "epic";
        return "legendary";
    }

    private static int tierColor(int level) {
        if (level < 0) return 0xEF8680;
        if (level == 0) return 0xB8B8B0;
        if (level <= 2) return 0x8BCB87;
        if (level <= 4) return 0x81B7EE;
        if (level <= 7) return 0xD09EF4;
        return 0xFFD27A;
    }

    private void renderQuality(GuiGraphics gui) {
        ItemStack stack = menu.workpiece();
        if (stack.isEmpty()) {
            gui.drawString(font, Component.translatable("screen.anvilborn.empty"), leftPos + 116,
                topPos + 36, MUTED_TEXT, false);
            return;
        }
        ForgeData data = ForgeRules.data(stack);
        String item = font.plainSubstrByWidth(stack.getHoverName().getString(), 96);
        gui.drawString(font, item, leftPos + 116, topPos + 36, BASE_TEXT, false);
        int color = tierColor(data.level());
        gui.drawString(font, Component.translatable("tier.anvilborn." + tierKey(data.level())),
            leftPos + 116, topPos + 52, color, false);
        String numeral = data.level() > 0 ? "+" + data.level() : Integer.toString(data.level());
        gui.drawString(font, numeral,
            leftPos + 210 - font.width(numeral), topPos + 52, color, false);
        gui.fill(leftPos + 114, topPos + 61, leftPos + 210, topPos + 62, 0xFF000000 | color);
    }

    private void renderRisk(GuiGraphics gui) {
        ItemStack stack = menu.workpiece();
        if (stack.isEmpty()) return;
        ForgeData data = ForgeRules.data(stack);
        boolean frenzy = data.frenzy() > 0;
        int risk = frenzy ? 0 : ForgeRules.breakChance(data.level());
        Component message = frenzy
            ? Component.translatable("screen.anvilborn.frenzy", data.frenzy())
            : Component.translatable("screen.anvilborn.break_risk", risk);
        int color = frenzy ? 0xFFD47C : risk >= 20 ? 0xFF938C : risk >= 8 ? 0xE3B67C : 0xB8C0AE;
        gui.drawString(font, message, leftPos + 116, topPos + 72, color, false);
    }

    private void renderAspects(GuiGraphics gui) {
        ItemStack stack = menu.workpiece();
        if (stack.isEmpty()) return;
        List<ForgeData.Roll> aspects = ForgeRules.data(stack).aspects();
        if (feedbackTicks > 0) {
            int result = menu.resultCode();
            Component message = Component.translatable("screen.anvilborn.result." + result);
            int color = switch (result) {
                case 1 -> 0x99D798;
                case 2 -> 0xFFD884;
                case 3, 4 -> 0xF0948B;
                default -> 0xD9D0BF;
            };
            gui.drawString(font, font.plainSubstrByWidth(message.getString(), 96),
                leftPos + 116, topPos + 93, color, false);
            return;
        }
        if (aspects.isEmpty()) {
            gui.drawString(font, Component.translatable("screen.anvilborn.no_aspects"),
                leftPos + 116, topPos + 92, MUTED_TEXT, false);
            return;
        }
        for (int i = 0; i < Math.min(2, aspects.size()); i++) {
            ForgeData.Roll roll = aspects.get(i);
            Aspect aspect = Aspects.byId(roll.id());
            if (aspect == null) continue;
            String label = Component.translatable(aspect.translationKey()).getString()
                + " " + "I".repeat(roll.rank());
            gui.drawString(font, font.plainSubstrByWidth(label, 95),
                leftPos + 116, topPos + 87 + 10 * i,
                aspect.harmful() ? 0xE09A89 : 0xA5CBBB, false);
        }
        if (aspects.size() > 2) {
            gui.drawString(font, Component.translatable("screen.anvilborn.extra_aspects", aspects.size() - 2),
                leftPos + 174, topPos + 97, MUTED_TEXT, false);
        }
    }

    private void renderProgress(GuiGraphics gui) {
        ItemStack stack = menu.workpiece();
        ForgeData data = ForgeRules.data(stack);
        gui.drawString(font, Component.translatable("screen.anvilborn.heat"),
            leftPos + 18, topPos + 115, 0xC9BDA8, false);
        String hits = data.frenzy() > 0
            ? Component.translatable("screen.anvilborn.frenzy_short").getString()
            : data.hits() + "/" + ForgeRules.TARGET_HITS;
        gui.drawString(font, hits, leftPos + 209 - font.width(hits),
            topPos + 115, data.frenzy() > 0 ? 0xFFD47C : 0xD5B38A, false);
        for (int i = 0; i < ForgeRules.TARGET_HITS; i++) {
            int spriteY = data.frenzy() > 0 ? 24 : i < data.hits() ? 12 : 0;
            gui.blit(WIDGETS, leftPos + 19 + i * 32, topPos + 124, 205, spriteY, 30, 9, 256, 128);
        }
    }

    private void renderStrikeFeedback(GuiGraphics gui) {
        if (feedbackTicks < 17 || menu.resultCode() == 0) return;
        int color = switch (menu.resultCode()) {
            case 1, 2 -> 0x44FFBD68;
            case 3, 4 -> 0x55F07C71;
            default -> 0x33FFFFFF;
        };
        gui.fill(leftPos + 17, topPos + 33, leftPos + 100, topPos + 105, color);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, Component.literal("ANVILBORN"), titleLabelX, titleLabelY, 0xE9C592, false);
        gui.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xDDD6C6, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        refreshButton();
        renderBackground(gui, mouseX, mouseY, partialTick);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }
}
