package dev.bearback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.polarbear.PolarBearModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class SaddlebagsLayer extends RenderLayer<PolarBearRenderState,PolarBearModel> {
    private static final Identifier TEXTURE=Identifier.fromNamespaceAndPath("bearback","textures/entity/bear_saddlebags.png");
    private final PolarBearModel model=new PolarBearModel(mesh().bakeRoot());
    public SaddlebagsLayer(RenderLayerParent<PolarBearRenderState,PolarBearModel> parent){super(parent);}
    @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,
                                 PolarBearRenderState state,float yaw,float pitch){
        if(!(state instanceof BearRenderState custom)||!custom.bearbackBags||state.isBaby||state.isInvisible)return;
        collector.submitModel(model,state,pose,RenderTypes.entityCutout(TEXTURE),
                light,LivingEntityRenderer.getOverlayCoords(state,0f),-1);
    }
    private static LayerDefinition mesh(){
        MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.offset(0,10,-16));
        root.addOrReplaceChild("right_hind_leg",CubeListBuilder.create(),PartPose.offset(-4.5F,14,6));
        root.addOrReplaceChild("left_hind_leg",CubeListBuilder.create(),PartPose.offset(4.5F,14,6));
        root.addOrReplaceChild("right_front_leg",CubeListBuilder.create(),PartPose.offset(-3.5F,14,-8));
        root.addOrReplaceChild("left_front_leg",CubeListBuilder.create(),PartPose.offset(3.5F,14,-8));
        root.addOrReplaceChild("body",CubeListBuilder.create()
                .texOffs(0,0).addBox(-9.8F,-11.7F,-1.75F,4.1F,8.4F,6.3F)
                .texOffs(34,0).addBox(9.7F,-11.7F,-1.75F,4.1F,8.4F,6.3F)
                .texOffs(70,0).addBox(-10.1F,-12F,2.45F,4.7F,9F,2F)
                .texOffs(96,0).addBox(9.4F,-12F,2.45F,4.7F,9F,2F)
                .texOffs(0,22).addBox(-6.1F,-10.6F,4.14F,1F,6.3F,.48F)
                .texOffs(48,22).addBox(9.1F,-10.6F,4.14F,1F,6.3F,.48F)
                .texOffs(72,40).addBox(-10.3F,-9F,.55F,.6F,2F,2.5F)
                .texOffs(94,40).addBox(13.7F,-9F,.55F,.6F,2F,2.5F),
                PartPose.offsetAndRotation(-2F,9F,12F,(float)Math.PI/2,0,0));
        return LayerDefinition.create(mesh,128,64);
    }
}
