package dev.minemods.leashwrap.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.minemods.leashwrap.AnchorLogic;
import dev.minemods.leashwrap.client.RopeGeometry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LeashKnotRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeashKnotRenderer.class)
public abstract class LeashKnotRendererMixin {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("leashwrap","textures/entity/wrap.png");

    @Inject(method="render(Lnet/minecraft/world/entity/decoration/LeashFenceKnotEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("TAIL"))
    private void leashwrap$render(LeashFenceKnotEntity knot,float yaw,float partial,
                                 PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci) {
        BlockPos pos=BlockPos.containing(knot.getX(),knot.getY(),knot.getZ());
        if(knot.level().getBlockState(pos).is(BlockTags.FENCES)||!AnchorLogic.canAnchor(knot.level(),pos)) return;
        RopeGeometry.draw(pose.last(),buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light);
    }
}
