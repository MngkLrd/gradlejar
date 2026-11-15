package dev.minemods.lassotrident.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.minemods.lassotrident.LassoTrident;
import dev.minemods.lassotrident.TetherNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = LassoTrident.MOD_ID, value = Dist.CLIENT)
public final class ClientTetherEvents {
    private static int anchorEntityId = -1;
    private static boolean lastKeyDown = false;

    private ClientTetherEvents() {}

    public static void setAnchor(int entityId) {
        anchorEntityId = entityId;
        lastKeyDown = Minecraft.getInstance().options.keyUse.isDown();
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            lastKeyDown = false;
            return;
        }
        boolean down = mc.options.keyUse.isDown();
        if (anchorEntityId != -1 && down != lastKeyDown) {
            TetherNetwork.sendControl(down);
        }
        lastKeyDown = down;
    }

    @SubscribeEvent
    public static void renderRope(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || anchorEntityId < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Entity anchor = mc.level.getEntity(anchorEntityId);
        if (anchor == null) return;

        Vec3 camera = event.getCamera().getPosition();
        Vec3 from = mc.player.getEyePosition(event.getPartialTick()).add(0.0, -0.22, 0.0);
        Vec3 to = anchor.position();
        Matrix4f matrix = new Matrix4f().translation(
                (float) -camera.x, (float) -camera.y, (float) -camera.z);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        line(buffer, matrix, from, to, 148, 104, 57);
        line(buffer, matrix, from.add(0.025, 0, 0), to.add(0.025, 0, 0), 212, 177, 107);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static void line(BufferBuilder b, Matrix4f m, Vec3 start, Vec3 end, int r, int g, int blue) {
        b.addVertex(m, (float) start.x, (float) start.y, (float) start.z).setColor(r, g, blue, 255);
        b.addVertex(m, (float) end.x, (float) end.y, (float) end.z).setColor(r, g, blue, 255);
    }
}
