package dev.minemods.redstonevision.client;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public final class VisionRenderTypes extends RenderStateShard {
    private VisionRenderTypes() { super("redstonevision",()->{},()->{}); }
    public static final RenderType CIRCUITS=RenderType.create("redstonevision_circuits",
        DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,true,false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setDepthTestState(NO_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(false));
    public static final RenderType OUTLINES=RenderType.create("redstonevision_outlines",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,1536,false,false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new LineStateShard(OptionalDouble.of(2.0)))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(NO_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false));
}
