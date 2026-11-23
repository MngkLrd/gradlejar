package com.kirill.simplesorter.client;

import com.kirill.simplesorter.network.WarehouseLabelsPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed chest-face decal, NOT a camera-facing nametag/billboard.
 *
 * A label faces the block's stored horizontal FACING direction and occupies
 * no entity or block space. The text and representative ItemStack are
 * render-only; an item can never be picked up or duplicated.
 *
 * Performance: label data is indexed by chunk; inspect only nearby 5x5
 * chunks and render at most 48 labels / 18 item models per frame. Labels on
 * the rear of a chest are skipped. Light follows the local block lighting.
 */
@Mod.EventBusSubscriber(modid = "simplesorter", value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientWarehouseLabels {
    private static final int MAX_VISIBLE_LABELS = 48;
    private static final int MAX_ICON_MODELS = 18;
    private static final int CHUNK_SEARCH_RADIUS = 2;
    private static final double LABEL_RADIUS_SQ = 23D * 23D;
    private static final double ICON_RADIUS_SQ = 14D * 14D;
    private static final int TEXT_COLOR = 0xFFF4E8C9;
    private static final int TEXT_BACKGROUND = 0x6A201910;

    private record RenderLabel(WarehouseLabelsPayload.Entry data,
                               Component caption, int textWidth) {}
    private record Visible(RenderLabel label, Vec3 surface,
                           boolean wide, double distanceSq) {}

    // Receive one whole chunk at a time, so no O(all saved world labels)
    // scan occurs on every render frame in a TerraFirmaGreg warehouse.
    private static final Map<Long, Map<Long, RenderLabel>> CHUNKS = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel previousLevel;

    private ClientWarehouseLabels() {}

    public static void accept(WarehouseLabelsPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (previousLevel != mc.level) {
            CHUNKS.clear();
            previousLevel = mc.level;
        }
        long chunkKey = ChunkPos.asLong(payload.chunkX(), payload.chunkZ());
        if (payload.labels().isEmpty()) {
            CHUNKS.remove(chunkKey);
            return;
        }
        Map<Long, RenderLabel> incoming = new HashMap<>();
        for (WarehouseLabelsPayload.Entry entry : payload.labels()) {
            Component caption = Component.translatable("label.simplesorter." + entry.family());
            incoming.put(entry.pos().asLong(),
                    new RenderLabel(entry, caption, mc.font.width(caption)));
        }
        CHUNKS.put(chunkKey, incoming);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != previousLevel) {
            CHUNKS.clear();
            previousLevel = mc.level;
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || CHUNKS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != previousLevel) return;

        Vec3 camera = event.getCamera().getPosition();
        int baseX = ((int) Math.floor(camera.x)) >> 4;
        int baseZ = ((int) Math.floor(camera.z)) >> 4;
        List<Visible> visible = new ArrayList<>(MAX_VISIBLE_LABELS * 2);

        // Only the 5x5 loaded chunk neighbourhood can be relevant. Even if
        // the world has thousands of persistent labels, we never scan them.
        for (int cx = baseX - CHUNK_SEARCH_RADIUS; cx <= baseX + CHUNK_SEARCH_RADIUS; cx++) {
            for (int cz = baseZ - CHUNK_SEARCH_RADIUS; cz <= baseZ + CHUNK_SEARCH_RADIUS; cz++) {
                Map<Long, RenderLabel> nearby = CHUNKS.get(ChunkPos.asLong(cx, cz));
                if (nearby == null) continue;
                for (RenderLabel render : nearby.values()) {
                    BlockPos pos = render.data().pos();
                    double roughX = pos.getX() + 0.5D - camera.x;
                    double roughY = pos.getY() + 0.5D - camera.y;
                    double roughZ = pos.getZ() + 0.5D - camera.z;
                    if (roughX * roughX + roughY * roughY + roughZ * roughZ
                            > LABEL_RADIUS_SQ + 2D) continue;

                    Direction face = render.data().facing();
                    if (!face.getAxis().isHorizontal()) continue;
                    if (roughX * face.getStepX() + roughZ * face.getStepZ() <= 0.09D) {
                        // Never display the front label through the rear of
                        // the chest when the user circles the storage wall.
                        continue;
                    }
                    if (!mc.level.hasChunkAt(pos) || mc.level.getBlockEntity(pos) == null) continue;

                    Vec3 center = Vec3.atCenterOf(pos);
                    boolean wide = false;
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.getBlock() instanceof ChestBlock chest
                            && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                        BlockPos partner = pos.relative(ChestBlock.getConnectedDirection(state));
                        if (mc.level.hasChunkAt(partner)
                                && mc.level.getBlockState(partner).getBlock() == chest) {
                            // A vanilla 54-slot chest has one label precisely
                            // centered across its two physical block halves.
                            center = center.add(
                                    (partner.getX() - pos.getX()) * 0.5D,
                                    0D,
                                    (partner.getZ() - pos.getZ()) * 0.5D);
                            wide = true;
                        }
                    }

                    // Fixed 0.53-block normal offset: sits just outside the
                    // full-block face (or the inset vanilla chest model).
                    Vec3 surface = center.add(
                            face.getStepX() * 0.535D, 0D,
                            face.getStepZ() * 0.535D);
                    double distSq = surface.distanceToSqr(camera);
                    if (distSq <= LABEL_RADIUS_SQ) {
                        visible.add(new Visible(render, surface, wide, distSq));
                    }
                }
            }
        }

        if (visible.isEmpty()) return;
        visible.sort(Comparator.comparingDouble(Visible::distanceSq));
        PoseStack poses = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        int drawn = 0;
        int icons = 0;

        for (Visible v : visible) {
            if (drawn >= MAX_VISIBLE_LABELS) break;
            WarehouseLabelsPayload.Entry data = v.label().data();
            BlockPos pos = data.pos();
            Direction face = data.facing();

            boolean drawIcon = data.style() == 2 && !data.icon().isEmpty()
                    && v.distanceSq() <= ICON_RADIUS_SQ && icons < MAX_ICON_MODELS;
            int light = LevelRenderer.getLightColor(mc.level, pos);
            poses.pushPose();
            poses.translate(v.surface().x - camera.x,
                    v.surface().y - camera.y, v.surface().z - camera.z);

            // A fixed block face, NEVER cameraOrientation().
            // Minecraft's font plane initially looks toward -Z and uses a
            // negative X/Y glyph scale. Rotate that plane outward:
            // SOUTH=180, NORTH=0, EAST=-90, WEST=90.
            // This preserves legibility and stops labels rotating with us.
            poses.mulPose(Axis.YP.rotationDegrees(180F - face.toYRot()));

            if (drawIcon) {
                poses.pushPose();
                poses.translate(0D, FaceLabelDimensions.iconY(), 0.105D);
                float iconScale = FaceLabelDimensions.iconScale();
                poses.scale(iconScale, iconScale, iconScale);
                mc.getItemRenderer().renderStatic(data.icon(), ItemDisplayContext.FIXED,
                        light, OverlayTexture.NO_OVERLAY,
                        poses, buffer, mc.level, (int) (pos.asLong() & 0xFFFF));
                poses.popPose();
                icons++;
            }

            // All widths are clamped to the physical chest front. Even a long
            // Russian translation cannot extend outside an adjacent chest.
            poses.pushPose();
            poses.translate(0D, FaceLabelDimensions.textY(drawIcon), 0.032D);
            float textScale = FaceLabelDimensions.textScale(v.label().textWidth(), v.wide());
            poses.scale(-textScale, -textScale, textScale);
            mc.font.drawInBatch(v.label().caption(),
                    -v.label().textWidth() * 0.5F, -4.5F,
                    TEXT_COLOR, false, poses.last().pose(),
                    buffer, Font.DisplayMode.NORMAL, TEXT_BACKGROUND, light);
            poses.popPose();
            poses.popPose();
            drawn++;
        }
        if (drawn > 0) buffer.endBatch();
    }
}
