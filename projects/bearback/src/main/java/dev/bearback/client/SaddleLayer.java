package dev.bearback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.bearback.Bearback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PolarBearModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.AxisAngle4f;
import com.mojang.math.Axis;

public final class SaddleLayer extends RenderLayer<PolarBear, PolarBearModel<PolarBear>> {
    private static final ItemStack SADDLE = new ItemStack(Items.SADDLE);

    public SaddleLayer(RenderLayerParent<PolarBear, PolarBearModel<PolarBear>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, PolarBear bear,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!bear.getData(Bearback.SADDLED.get())) return;
        pose.pushPose();
        this.getParentModel().body.translateAndRotate(pose);
        pose.translate(0, -0.39, 0.06);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(1.3F, 1.3F, 1.3F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                SADDLE, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, bear.level(), bear.getId());
        pose.popPose();
    }
}
