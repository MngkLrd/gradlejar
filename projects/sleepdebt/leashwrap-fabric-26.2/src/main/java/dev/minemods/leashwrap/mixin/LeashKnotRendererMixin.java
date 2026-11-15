package dev.minemods.leashwrap.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.minemods.leashwrap.AnchorLogic;
import dev.minemods.leashwrap.client.RopeGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LeashKnotRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeashKnotRenderer.class)
public abstract class LeashKnotRendererMixin {
    private static final Identifier TEXTURE=
        Identifier.fromNamespaceAndPath("leashwrap","textures/entity/wrap.png");

    @Inject(method="submit",at=@At("TAIL"))
    private void leashwrap$submit(EntityRenderState state,PoseStack pose,
                                  SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci) {
        var level=Minecraft.getInstance().level;
        if(level==null) return;
        BlockPos pos=BlockPos.containing(state.x,state.y,state.z);
        if(level.getBlockState(pos).is(BlockTags.FENCES)||!AnchorLogic.canAnchor(level,pos)) return;
        final int light=state.lightCoords;
        collector.submitCustomGeometry(pose,RenderTypes.entityCutout(TEXTURE),
            (current,buffer)->RopeGeometry.draw(current,buffer,light));
    }
}
