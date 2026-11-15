package dev.bearback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.bearback.Bearback;
import net.minecraft.client.model.PolarBearModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.PolarBear;

public final class SaddleLayer extends RenderLayer<PolarBear,PolarBearModel<PolarBear>> {
    private static final ResourceLocation TEXTURE=new ResourceLocation(Bearback.MOD_ID,"textures/entity/bear_saddle.png");
    private final PoseModel animated=new PoseModel();
    private final ModelPart saddle=createMesh().bakeRoot().getChild("saddle");

    public SaddleLayer(RenderLayerParent<PolarBear,PolarBearModel<PolarBear>> parent){super(parent);}

    @Override
    public void render(PoseStack pose,MultiBufferSource buffer,int light,PolarBear bear,
                       float limbSwing,float limbSwingAmount,float partialTick,
                       float age,float yaw,float pitch){
        if(!ClientHooks.isSaddled(bear)||bear.isBaby())return;
        animated.prepareMobModel(bear,limbSwing,limbSwingAmount,partialTick);
        animated.setupAnim(bear,limbSwing,limbSwingAmount,age,yaw,pitch);
        pose.pushPose();
        animated.torso().translateAndRotate(pose);
        VertexConsumer consumer=buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        saddle.render(pose,consumer,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static LayerDefinition createMesh(){
        MeshDefinition mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("saddle",CubeListBuilder.create()
                .texOffs(0,0).addBox(-5.5F,-12.9F,3.94F,15F,10.8F,.45F)
                .texOffs(34,0).addBox(-5.25F,-12.4F,4.20F,14.5F,9.6F,.92F)
                .texOffs(68,0).addBox(-4.5F,-14.0F,3.08F,13F,1.5F,2.17F)
                .texOffs(68,8).addBox(-5.15F,-3.2F,4F,14.3F,1.3F,1.55F)
                .texOffs(0,18).addBox(-5.90F,-11.6F,-1.45F,1.22F,8.2F,5.75F)
                .texOffs(22,18).addBox(8.68F,-11.6F,-1.45F,1.22F,8.2F,5.75F)
                .texOffs(44,18).addBox(-6.05F,-10.65F,-6.95F,.9F,1.5F,11.12F)
                .texOffs(76,18).addBox(9.15F,-10.65F,-6.95F,.9F,1.5F,11.12F)
                .texOffs(0,39).addBox(-5.6F,-10.65F,-7.21F,15.2F,1.5F,.6F)
                .texOffs(39,39).addBox(-6.29F,-10.92F,-1F,.55F,2.05F,2.2F)
                .texOffs(63,39).addBox(9.75F,-10.92F,-1F,.55F,2.05F,2.2F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh,128,64);
    }
    private static final class PoseModel extends PolarBearModel<PolarBear>{
        private PoseModel(){super(PolarBearModel.createBodyLayer().bakeRoot());}
        private ModelPart torso(){return this.body;}
    }
}
