package com.kirill.simplesorter.client;

import com.kirill.simplesorter.network.WarehouseLabelsPayload;
import com.kirill.simplesorter.network.LabelSyncRequestPayload;
import com.kirill.simplesorter.network.ModNetworking;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
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
                           boolean wide, double distanceSq, Direction face) {}

    // Receive one whole chunk at a time, so no O(all saved world labels)
    // scan occurs on every render frame in a TerraFirmaGreg warehouse.
    private static final Map<Long, Map<Long, RenderLabel>> CHUNKS = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel previousLevel;
    private static int initialSyncDelay = -1;

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
            // ChunkWatch packets may arrive before Minecraft.level is ready
            // and be discarded. Refresh once after entering the world.
            initialSyncDelay = mc.level == null ? -1 : 40;
        }
        if (initialSyncDelay >= 0 && mc.level != null && mc.player != null) {
            if (initialSyncDelay-- == 0) {
                ModNetworking.CHANNEL.sendToServer(new LabelSyncRequestPayload());
                initialSyncDelay = -1;
            }
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

                    if (!mc.level.hasChunkAt(pos) || mc.level.getBlockEntity(pos) == null) continue;

                    Vec3 center = Vec3.atCenterOf(pos);
                    boolean wide = false;
                    BlockState state = mc.level.getBlockState(pos);
                    boolean insetChest = state.getBlock() instanceof ChestBlock;
                    BlockPos otherHalf = null;
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
                            otherHalf = partner;
                        }
                    }

                    // The old direction was assigned from the player's
                    // position relative to EACH chest. On a 14-column wall,
                    // that puts many edge crates' labels on their EAST/WEST
                    // side rather than the actual NORTH/SOUTH front.
                    // Pick only among exposed, STATIC block faces. Labels
                    // never billboard, and turn only at 90-degree surfaces.
                    boolean north = exposed(mc.level, pos, Direction.NORTH);
                    boolean south = exposed(mc.level, pos, Direction.SOUTH);
                    boolean east = exposed(mc.level, pos, Direction.EAST);
                    boolean west = exposed(mc.level, pos, Direction.WEST);
                    if (otherHalf != null) {
                        north &= exposed(mc.level, otherHalf, Direction.NORTH);
                        south &= exposed(mc.level, otherHalf, Direction.SOUTH);
                        east &= exposed(mc.level, otherHalf, Direction.EAST);
                        west &= exposed(mc.level, otherHalf, Direction.WEST);
                        if (pos.getX() != otherHalf.getX()) {
                            east = false;
                            west = false;
                        } else {
                            north = false;
                            south = false;
                        }
                    }
                    int side = FixedLabelFaces.bestFace(
                            camera.x - center.x, camera.z - center.z,
                            north, south, east, west);
                    if (!FixedLabelFaces.isCardinal(side)) continue;
                    Direction face = faceOf(side);

                    double depth = FaceLabelDimensions.faceOffset(insetChest);
                    Vec3 surface = center.add(
                            face.getStepX() * depth, 0D,
                            face.getStepZ() * depth);
                    double distSq = surface.distanceToSqr(camera);
                    if (distSq <= LABEL_RADIUS_SQ) {
                        visible.add(new Visible(render, surface, wide, distSq, face));
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
            Direction face = v.face();

            boolean drawIcon = data.style() == 2 && !data.icon().isEmpty()
                    && v.distanceSq() <= ICON_RADIUS_SQ && icons < MAX_ICON_MODELS;
            int light = LevelRenderer.getLightColor(mc.level, pos);
            // The v0.9.7 renderer replaced FULL_BRIGHT with raw lighting.
            // At night this can make captions practically black. Keep an
            // ambient floor for the LETTERS only; 3D items still use natural
            // block light and thus remain part of the scene.
            int textLight = LightTexture.pack(
                    Math.max(9, (light >> 4) & 15),
                    Math.max(9, (light >> 20) & 15));
            poses.pushPose();
            poses.translate(v.surface().x - camera.x,
                    v.surface().y - camera.y, v.surface().z - camera.z);

            // Font glyphs using (-X,-Y,+Z) scale have a +Z front.
            // NORTH requires 180 degrees, SOUTH zero, EAST +90, WEST -90.
            // 0.9.7/0.9.8 used the opposite transformation, turning glyph
            // fronts INTO the chest and making the text disappear.
            poses.mulPose(Axis.YP.rotationDegrees(FixedLabelFaces.glyphRotationDegrees(faceCode(face))));

            if (drawIcon) {
                poses.pushPose();
                poses.translate(0D, FaceLabelDimensions.iconY(), FaceLabelDimensions.itemDepth());
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
            poses.translate(0D, FaceLabelDimensions.textY(drawIcon), FaceLabelDimensions.textDepth());
            float textScale = FaceLabelDimensions.textScale(v.label().textWidth(), v.wide());
            poses.scale(-textScale, -textScale, textScale);
            mc.font.drawInBatch(v.label().caption(),
                    -v.label().textWidth() * 0.5F, -4.5F,
                    TEXT_COLOR, false, poses.last().pose(),
                    buffer, Font.DisplayMode.NORMAL, TEXT_BACKGROUND, textLight);
            poses.popPose();
            poses.popPose();
            drawn++;
        }
        if (drawn > 0) buffer.endBatch();
    }

    private static int faceCode(Direction face) {
        return switch (face) {
            case NORTH -> FixedLabelFaces.NORTH;
            case SOUTH -> FixedLabelFaces.SOUTH;
            case EAST -> FixedLabelFaces.EAST;
            case WEST -> FixedLabelFaces.WEST;
            default -> FixedLabelFaces.SOUTH;
        };
    }

    private static Direction faceOf(int code) {
        return switch (code) {
            case FixedLabelFaces.NORTH -> Direction.NORTH;
            case FixedLabelFaces.SOUTH -> Direction.SOUTH;
            case FixedLabelFaces.EAST -> Direction.EAST;
            case FixedLabelFaces.WEST -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    /** Never force-load a chunk to draw a decorative decal. */
    private static boolean exposed(net.minecraft.client.multiplayer.ClientLevel level,
                                   BlockPos pos, Direction side) {
        BlockPos neighbor = pos.relative(side);
        if (!level.hasChunkAt(neighbor)) return false;
        if (level.getBlockEntity(neighbor) != null) return false;
        BlockState block = level.getBlockState(neighbor);
        return !block.isSolidRender(level, neighbor);
    }
}
