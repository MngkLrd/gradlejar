package com.kirill.simplesorter.client;

import com.kirill.simplesorter.network.LabelSyncRequestPayload;
import com.kirill.simplesorter.network.ModNetworking;
import com.kirill.simplesorter.network.WarehouseLabelsPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
 * Small original-style labels drawn in world coordinates over real chest
 * faces, not floating billboards. The labels never follow camera rotation.
 *
 * Text visible only within 10 blocks and only with line of sight.
 * The old 0.9.5 billboard text was rendered much farther outward than the
 * physical front of TFC crates. The 0.9.7+ "nearly flush" decals sat BEHIND
 * the protruding crate models, which explains why the item icons (drawn
 * farther out) showed up but the text did not.
 *
 * All exposed faces are static cardinal planes. The back/side is only
 * rendered when it is itself an exposed chest face, never through walls.
 * Pure client-side visuals: no entities, item copies in inventories, or
 * player/world mutations.
 */
@Mod.EventBusSubscriber(modid = "simplesorter", value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientWarehouseLabels {
    private static final int MAX_VISIBLE_LABELS = 36;
    private static final int MAX_ICON_MODELS = 12;
    private static final int CHUNK_SEARCH_RADIUS = 1;
    private static final int TEXT_COLOR = 0xFFF4E9D2;
    private static final int TEXT_BACKGROUND = 0x70241C15;
    private static final Direction[] FACES = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private record RenderLabel(WarehouseLabelsPayload.Entry data,
                               Component caption, int textWidth) {}
    private record Visible(RenderLabel label, Vec3 surface, BlockPos partner,
                           boolean wide, double distanceSq, Direction face) {}

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
            String key = "label.simplesorter." + entry.family();
            Component text = Component.translatableWithFallback(key,entry.family().replace('_',' '));
            incoming.put(entry.pos().asLong(),
                    new RenderLabel(entry, text, mc.font.width(text)));
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
            initialSyncDelay = mc.level == null ? -1 : 40;
        }
        if (initialSyncDelay >= 0 && mc.level != null && mc.player != null
                && initialSyncDelay-- == 0) {
            ModNetworking.CHANNEL.sendToServer(new LabelSyncRequestPayload());
            initialSyncDelay = -1;
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || CHUNKS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != previousLevel || mc.player == null) return;

        Vec3 camera = event.getCamera().getPosition();
        int chunkX = ((int) Math.floor(camera.x)) >> 4;
        int chunkZ = ((int) Math.floor(camera.z)) >> 4;
        List<Visible> candidates = new ArrayList<>(64);

        // 10-block distance only needs a 3x3 chunk neighborhood, with no
        // background tasks or complete warehouse searches on every frame.
        for (int cx = chunkX - CHUNK_SEARCH_RADIUS; cx <= chunkX + CHUNK_SEARCH_RADIUS; cx++) {
            for (int cz = chunkZ - CHUNK_SEARCH_RADIUS; cz <= chunkZ + CHUNK_SEARCH_RADIUS; cz++) {
                Map<Long, RenderLabel> chunk = CHUNKS.get(ChunkPos.asLong(cx, cz));
                if (chunk == null) continue;
                for (RenderLabel label : chunk.values()) {
                    BlockPos pos = label.data().pos();
                    Vec3 blockCenter = Vec3.atCenterOf(pos);
                    // Cheap candidate rejection, allowing the front face to be
                    // closer than the block center by up to ~0.7 blocks.
                    if (blockCenter.distanceToSqr(camera) > 11D * 11D) {
                        continue;
                    }
                    if (!mc.level.hasChunkAt(pos) || mc.level.getBlockEntity(pos) == null) {
                        continue;
                    }

                    BlockState state = mc.level.getBlockState(pos);
                    boolean vanillaChest = state.getBlock() instanceof ChestBlock;
                    BlockPos partner = null;
                    Vec3 center = blockCenter;
                    boolean wide = false;
                    if (vanillaChest && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                        BlockPos candidate = pos.relative(ChestBlock.getConnectedDirection(state));
                        if (mc.level.hasChunkAt(candidate)
                                && mc.level.getBlockState(candidate).getBlock() == state.getBlock()) {
                            partner = candidate;
                            wide = true;
                            center = center.add(
                                    (partner.getX() - pos.getX()) * 0.5D,
                                    0D,
                                    (partner.getZ() - pos.getZ()) * 0.5D);
                        }
                    }

                    for (Direction face : FACES) {
                        if (!exposed(mc.level, pos, face)) continue;
                        if (partner != null && !exposed(mc.level, partner, face)) continue;
                        if (partner != null) {
                            // The connecting faces / short ends cannot carry
                            // a centered sign across both chest halves.
                            if (pos.getX() != partner.getX()
                                    && face.getAxis() == Direction.Axis.X) continue;
                            if (pos.getZ() != partner.getZ()
                                    && face.getAxis() == Direction.Axis.Z) continue;
                        }

                        Vec3 cameraRelative = camera.subtract(center);
                        if (cameraRelative.x * face.getStepX()
                                + cameraRelative.z * face.getStepZ() < 0.18D) continue;

                        Vec3 surface = center.add(
                                face.getStepX() * FaceLabelDimensions.labelDistanceFromCenter(vanillaChest),
                                0D,
                                face.getStepZ() * FaceLabelDimensions.labelDistanceFromCenter(vanillaChest));
                        double distanceSquared = surface.distanceToSqr(camera);
                        if (!FaceLabelDimensions.withinRange(distanceSquared)) continue;
                        candidates.add(new Visible(label, surface, partner,
                                wide, distanceSquared, face));
                    }
                }
            }
        }

        if (candidates.isEmpty()) return;
        candidates.sort(Comparator.comparingDouble(Visible::distanceSq));

        PoseStack poses = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        int labels = 0;
        int icons = 0;

        for (Visible visible : candidates) {
            if (labels >= MAX_VISIBLE_LABELS) break;
            BlockPos pos = visible.label().data().pos();
            // A manual camera-to-label ray is essential for shader packs that
            // render translucent Minecraft text without reliable depth tests.
            // Reject walls and any other crate between viewer and this one.
            if (!unobstructed(mc, camera, visible.surface(), pos, visible.partner())) continue;

            WarehouseLabelsPayload.Entry data = visible.label().data();
            Direction face = visible.face();
            boolean drawIcon = data.style() == 2 && !data.icon().isEmpty()
                    && icons < MAX_ICON_MODELS;

            int blockLight = LevelRenderer.getLightColor(mc.level, pos);
            // Keep the old readable text illumination but not fullbright
            // models. Occlusion is enforced by ray tracing and normal depth.
            int captionLight = LightTexture.FULL_BRIGHT;

            poses.pushPose();
            poses.translate(visible.surface().x - camera.x,
                    visible.surface().y - camera.y,
                    visible.surface().z - camera.z);

            // Static facing based on the physical NORTH/SOUTH/EAST/WEST wall,
            // not Minecraft.cameraOrientation. No moving billboard.
            int side = faceCode(face);
            poses.mulPose(Axis.YP.rotationDegrees(
                    FixedLabelFaces.glyphRotationDegrees(side)));

            if (drawIcon) {
                poses.pushPose();
                poses.translate(0D, FaceLabelDimensions.iconY(),
                        FaceLabelDimensions.itemDepth());
                float modelSize = FaceLabelDimensions.iconScale();
                poses.scale(modelSize, modelSize, modelSize);
                mc.getItemRenderer().renderStatic(
                        data.icon(), ItemDisplayContext.FIXED,
                        blockLight, OverlayTexture.NO_OVERLAY,
                        poses, buffer, mc.level,
                        (int) (pos.asLong() & 0xFFFF));
                poses.popPose();
                icons++;
            }

            poses.pushPose();
            poses.translate(0D, FaceLabelDimensions.textY(drawIcon),
                    FaceLabelDimensions.textDepth());
            float scale = FaceLabelDimensions.textScale(
                    visible.label().textWidth(), visible.wide());
            // Match vanilla sign text transforms: positive X, negative
            // Y, positive Z. Earlier versions flipped BOTH X and Y;
            // together with the world-fixed yaw this made the glyph plane
            // face inward even when its nominal facing looked correct.
            // The original billboard hid this through camera rotation.
            poses.scale(scale, -scale, scale);
            mc.font.drawInBatch(
                    visible.label().caption(),
                    -visible.label().textWidth() * 0.5F, -4.5F,
                    TEXT_COLOR, false, poses.last().pose(),
                    buffer, Font.DisplayMode.NORMAL,
                    TEXT_BACKGROUND, captionLight);
            poses.popPose();
            poses.popPose();
            labels++;
        }
        if (labels > 0) buffer.endBatch();
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

    /** Never load new chunks just to draw a label. */
    private static boolean exposed(net.minecraft.client.multiplayer.ClientLevel level,
                                   BlockPos pos, Direction side) {
        BlockPos adjacent = pos.relative(side);
        if (!level.hasChunkAt(adjacent)) return false;
        if (level.getBlockEntity(adjacent) != null) return false;
        BlockState state = level.getBlockState(adjacent);
        return !state.isSolidRender(level, adjacent);
    }

    private static boolean unobstructed(Minecraft mc, Vec3 from, Vec3 target,
                                        BlockPos owner, BlockPos doubleHalf) {
        BlockHitResult hit = mc.level.clip(new ClipContext(from, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() != HitResult.Type.BLOCK) return true;
        BlockPos first = hit.getBlockPos();
        return first.equals(owner) || (doubleHalf != null && first.equals(doubleHalf));
    }
}
