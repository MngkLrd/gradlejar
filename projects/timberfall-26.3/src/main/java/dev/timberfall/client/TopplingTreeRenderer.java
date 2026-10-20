package dev.timberfall.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.timberfall.entity.TopplingTreeEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
public final class TopplingTreeRenderer extends EntityRenderer<TopplingTreeEntity,TopplingTreeRenderState> {
    private static final BlockDisplayContext CONTEXT=BlockDisplayContext.create();
    private final BlockModelResolver resolver;
    public TopplingTreeRenderer(EntityRendererProvider.Context context){
        super(context);
        this.resolver=context.getBlockModelResolver();
    }
    @Override protected AABB getBoundingBoxForCulling(TopplingTreeEntity entity,float partialTicks){
        return entity.getBoundingBox().inflate(25);
    }
    @Override public TopplingTreeRenderState createRenderState(){return new TopplingTreeRenderState();}
    @Override public void extractRenderState(TopplingTreeEntity entity,TopplingTreeRenderState state,float partialTick){
        super.extractRenderState(entity,state,partialTick);
        state.age=entity.tickCount+partialTick;
        state.direction=entity.fallDirection();
        var models=new java.util.ArrayList<TopplingTreeRenderState.RenderedPiece>();
        for(var piece:entity.pieces()){
            var model=new BlockModelRenderState();
            resolver.update(model,piece.state(),CONTEXT);
            models.add(new TopplingTreeRenderState.RenderedPiece(piece.x(),piece.y(),piece.z(),model));
        }
        state.blocks=java.util.List.copyOf(models);
    }
    @Override public void submit(TopplingTreeRenderState state,PoseStack pose,
        SubmitNodeCollector collector,CameraRenderState camera){
        pose.pushPose();
        float t=Mth.clamp(state.age/(TopplingTreeEntity.LIFE_TICKS-3f),0,1);
        float angle=88f*t*t*(3-2*t);
        Direction dir=state.direction;
        switch(dir){
            case NORTH -> pose.rotateDegrees(Axis.XP,-angle);
            case SOUTH -> pose.rotateDegrees(Axis.XP,angle);
            case EAST -> pose.rotateDegrees(Axis.ZP,-angle);
            case WEST -> pose.rotateDegrees(Axis.ZP,angle);
            default -> {}
        }
        for(var piece:state.blocks){
            pose.pushPose();
            pose.translate(piece.x()-0.5f,piece.y(),piece.z()-0.5f);
            piece.model().submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,state.outlineColor);
            pose.popPose();
        }
        pose.popPose();
        super.submit(state,pose,collector,camera);
    }
}