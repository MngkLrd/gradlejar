package dev.fieldkit.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compact coordinate HUD. No second equipment panel or prominent colored bar. */
public final class HudOverlay {
    private static final float SCALE = 0.5F;
    private static final int TEXT = 0xFFE8EEF8;
    private static final int MUTED = 0xFFB8C4D8;
    private static final int ACCENT = 0xFF8DE6D9;
    private static final int WARNING = 0xFFFFD27A;
    private static final int PANEL = 0x55101824;
    private static final int SHADOW = 0x33000000;
    private static final int LINE_HEIGHT = 7;
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 3;

    private HudOverlay() {}

    public static void render(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientState.isHudVisible() || mc.player == null || mc.level == null
            || mc.getDebugOverlay().showDebugScreen()) return;

        List<Line> lines = new ArrayList<>();
        BlockPos playerPos = mc.player.blockPosition();
        lines.add(new Line(Component.translatable("hud.fieldkit.coords",
            playerPos.getX(), playerPos.getY(), playerPos.getZ()), TEXT));

        if (!ClientState.isCompact()) {
            lines.add(new Line(Component.translatable("hud.fieldkit.facing",
                Component.translatable("direction.fieldkit." + mc.player.getDirection().getSerializedName())), MUTED));
            long day = Math.floorMod(mc.level.getOverworldClockTime(), 24000L);
            int minutes = (int) (((day + 6000) % 24000) * 60 / 1000);
            lines.add(new Line(Component.translatable("hud.fieldkit.time",
                String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)), MUTED));
            Component target = getTarget(mc);
            if (target != null) {
                lines.add(new Line(Component.translatable("hud.fieldkit.target", target), MUTED));
            }
        }

        BlockPos waypoint = ClientState.getWaypoint();
        if (waypoint != null) {
            if (ClientState.isWaypointIn(mc.level.dimension())) {
                int dx = waypoint.getX() - playerPos.getX();
                int dz = waypoint.getZ() - playerPos.getZ();
                int distance = (int) Math.round(Math.hypot(dx, dz));
                String direction = compass(dx, dz);
                lines.add(new Line(Component.translatable("hud.fieldkit.waypoint", distance, direction), ACCENT));
            } else {
                lines.add(new Line(Component.translatable("hud.fieldkit.other_dimension"), WARNING));
            }
        }

        drawCompactPanel(gfx, mc.font, 6, 6, lines);
    }

    private static Component getTarget(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null) return null;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockState block = mc.level.getBlockState(blockHit.getBlockPos());
            return block.getBlock().getName();
        }
        if (hit instanceof EntityHitResult entityHit) return entityHit.getEntity().getDisplayName();
        return null;
    }

    private static String compass(int dx, int dz) {
        if (dx == 0 && dz == 0) return "HERE";
        double angle = Math.toDegrees(Math.atan2(-dz, dx));
        String[] directions = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};
        int index = Math.floorMod((int) Math.round(angle / 45), 8);
        return directions[index];
    }

    private static int measure(Font font, List<Line> lines) {
        int width = 0;
        for (Line line : lines) width = Math.max(width, font.width(line.text()));
        return width;
    }

    private static void drawCompactPanel(GuiGraphicsExtractor gfx, Font font, int x, int y, List<Line> lines) {
        if (lines.isEmpty()) return;
        int unscaledWidth = measure(font, lines);
        int scaledTextWidth = Math.max(1, Math.round(unscaledWidth * SCALE));
        int width = scaledTextWidth + PADDING_X * 2;
        int height = lines.size() * LINE_HEIGHT + PADDING_Y * 2;

        gfx.fill(x, y, x + width, y + height, PANEL);
        gfx.fill(x, y + height, x + width, y + height + 1, SHADOW);

        gfx.pose().pushMatrix();
        gfx.pose().translate(x + PADDING_X, y + PADDING_Y);
        gfx.pose().scale(SCALE, SCALE);
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            int drawY = Math.round(i * LINE_HEIGHT / SCALE);
            gfx.text(font, line.text(), 0, drawY, line.color(), false);
        }
        gfx.pose().popMatrix();
    }

    private record Line(Component text, int color) {}
}
