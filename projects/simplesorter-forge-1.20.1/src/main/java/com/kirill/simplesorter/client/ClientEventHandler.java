package com.kirill.simplesorter.client;

import com.kirill.simplesorter.registry.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import com.kirill.simplesorter.settings.SortingWandSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mod.EventBusSubscriber(modid="simplesorter",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEventHandler {
 @SubscribeEvent public static void forgeTick(TickEvent.ClientTickEvent e){
  if(e.phase==TickEvent.Phase.END)onClientTick(Minecraft.getInstance());
 }
 @SubscribeEvent public static void forgeGui(RenderGuiEvent.Post e){onRenderGui(e.getGuiGraphics());}

    private static final int HUD_TEXT = 0xFFEDE2CC;
    private static final int HUD_MUTED = 0xFFB5AA9B;
    private static final int HUD_BG = 0x99000000;

    private ClientEventHandler() {}

    public static void onClientTick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        Player player = minecraft.player;
        HeldWand heldWand = getHeldWandWithHand(player);
        if (heldWand.stack().isEmpty()) {
            return;
        }

        handleKeybinds(minecraft, player, heldWand);

        SortingWandSettings settings = SortingWandSettings.readFrom(heldWand.stack());
        if (!settings.showRadius()) {
            return;
        }

        if (player.tickCount % 10 != 0) {
            return;
        }

        showSortingRadius(minecraft, player, settings.radius());
    }

    public static void onRenderGui(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null) {
            return;
        }

        HeldWand heldWand = getHeldWandWithHand(minecraft.player);
        if (heldWand.stack().isEmpty()) {
            return;
        }

        drawWandHints(minecraft, graphics, heldWand.stack());
    }

    private static void handleKeybinds(Minecraft minecraft, Player player, HeldWand heldWand) {
        while (ClientKeyMappings.OPEN_SETTINGS.consumeClick()) {
            ClientScreenOpener.openWandSettings(heldWand.stack(), heldWand.hand());
        }

        while (ClientKeyMappings.QUICK_SORT.consumeClick()) {
            if (minecraft.gameMode != null && minecraft.screen == null) {
                minecraft.gameMode.useItem(player, heldWand.hand());
                player.swing(heldWand.hand());
            }
        }
    }

    private static void drawWandHints(Minecraft minecraft, GuiGraphics graphics, ItemStack stack) {
        SortingWandSettings settings = SortingWandSettings.readFrom(stack);
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int y = screenHeight - 106;

        Component useKey = minecraft.options.keyUse.getTranslatedKeyMessage();
        Component menuKey = ClientKeyMappings.OPEN_SETTINGS.getTranslatedKeyMessage();
        Component quickKey = ClientKeyMappings.QUICK_SORT.getTranslatedKeyMessage();

        Component line1 = Component.translatable(
                "hud.simplesorter.wand_hint.line1",
                useKey,
                useKey,
                menuKey,
                quickKey
        );
        Component line2 = Component.translatable(
                "hud.simplesorter.wand_hint.line2",
                Component.translatable(settings.mode().translationKey()),
                settings.radius()
        );

        int width1 = minecraft.font.width(line1);
        int width2 = minecraft.font.width(line2);
        int boxWidth = Math.max(width1, width2) + 18;
        int x = (screenWidth - boxWidth) / 2;

        graphics.fill(x, y - 4, x + boxWidth, y + 20, HUD_BG);
        graphics.drawCenteredString(minecraft.font, line1, screenWidth / 2, y, HUD_TEXT);
        graphics.drawCenteredString(minecraft.font, line2, screenWidth / 2, y + 10, HUD_MUTED);
    }

    private static HeldWand getHeldWandWithHand(Player player) {
        if (isWand(player.getMainHandItem())) {
            return new HeldWand(player.getMainHandItem(), InteractionHand.MAIN_HAND);
        }
        if (isWand(player.getOffhandItem())) {
            return new HeldWand(player.getOffhandItem(), InteractionHand.OFF_HAND);
        }
        return HeldWand.EMPTY;
    }

    private static boolean isWand(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ModItems.SORTING_WAND.get();
    }

    private static void showSortingRadius(Minecraft minecraft, Player player, int configuredRadius) {
        double centerX = player.getX();
        double centerY = player.getY() + 0.08D;
        double centerZ = player.getZ();
        double radius = configuredRadius;
        int stepsPerEdge = Math.max(6, Math.min(14, configuredRadius));

        drawLine(minecraft, centerX - radius, centerY, centerZ - radius, centerX + radius, centerY, centerZ - radius, stepsPerEdge);
        drawLine(minecraft, centerX - radius, centerY, centerZ + radius, centerX + radius, centerY, centerZ + radius, stepsPerEdge);
        drawLine(minecraft, centerX - radius, centerY, centerZ - radius, centerX - radius, centerY, centerZ + radius, stepsPerEdge);
        drawLine(minecraft, centerX + radius, centerY, centerZ - radius, centerX + radius, centerY, centerZ + radius, stepsPerEdge);

        drawCornerColumn(minecraft, centerX - radius, centerY, centerZ - radius);
        drawCornerColumn(minecraft, centerX + radius, centerY, centerZ - radius);
        drawCornerColumn(minecraft, centerX - radius, centerY, centerZ + radius);
        drawCornerColumn(minecraft, centerX + radius, centerY, centerZ + radius);
    }

    private static void drawLine(Minecraft minecraft, double x1, double y1, double z1, double x2, double y2, double z2, int steps) {
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            double x = x1 + (x2 - x1) * t;
            double y = y1 + (y2 - y1) * t;
            double z = z1 + (z2 - z1) * t;
            addRadiusParticle(minecraft, x, y, z);
        }
    }

    private static void drawCornerColumn(Minecraft minecraft, double x, double y, double z) {
        addRadiusParticle(minecraft, x, y + 0.25D, z);
        addRadiusParticle(minecraft, x, y + 0.7D, z);
        addRadiusParticle(minecraft, x, y + 1.15D, z);
    }

    private static void addRadiusParticle(Minecraft minecraft, double x, double y, double z) {
        minecraft.level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    private record HeldWand(ItemStack stack, InteractionHand hand) {
        private static final HeldWand EMPTY = new HeldWand(ItemStack.EMPTY, InteractionHand.MAIN_HAND);
    }
}
