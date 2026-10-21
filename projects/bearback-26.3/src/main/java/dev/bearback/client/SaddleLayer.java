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

/** Exact 1.21.1 saddle dimensions in a 26.3 animated polar-bear-body model. */
public final class SaddleLayer extends RenderLayer<PolarBearRenderState,PolarBearModel> {
    private static final Identifier TEXTURE=Identifier.fromNamespaceAndPath("bearback","textures/entity/bear_saddle.png");
    private final PolarBearModel saddleModel=new PolarBearModel(mesh().bakeRoot());

    public SaddleLayer(RenderLayerParent<PolarBearRenderState,PolarBearModel> parent){super(parent);}

    @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,
                                 PolarBearRenderState state,float yaw,float pitch){
        if(!(state instanceof BearRenderState custom)||!custom.bearbackSaddled||state.isBaby||state.isInvisible)return;
        collector.submitModel(saddleModel,state,pose,RenderTypes.entityCutout(TEXTURE),
                light,LivingEntityRenderer.getOverlayCoords(state,0f),-1,null,state.outlineColor,null);
    }
    private static LayerDefinition mesh(){
        MeshDefinition mesh=new MeshDefinition();
        var root=mesh.getRoot();
        root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.offset(0,10,-16));
        root.addOrReplaceChild("right_hind_leg",CubeListBuilder.create(),PartPose.offset(-4.5F,14,6));
        root.addOrReplaceChild("left_hind_leg",CubeListBuilder.create(),PartPose.offset(4.5F,14,6));
        root.addOrReplaceChild("right_front_leg",CubeListBuilder.create(),PartPose.offset(-3.5F,14,-8));
        root.addOrReplaceChild("left_front_leg",CubeListBuilder.create(),PartPose.offset(3.5F,14,-8));
        root.addOrReplaceChild("body",CubeListBuilder.create()
                .texOffs(0,0).addBox(-5.5F,-12.9F,3.94F,15F,10.8F,.45F)
                .texOffs(34,0).addBox(-5.25F,-12.4F,4.20F,14.5F,9.6F,.92F)
                .texOffs(68,0).addBox(-4.5F,-14F,3.08F,13F,1.5F,2.17F)
                .texOffs(68,8).addBox(-5.15F,-3.2F,4F,14.3F,1.3F,1.55F)
                .texOffs(0,18).addBox(-5.90F,-11.6F,-1.45F,1.22F,8.2F,5.75F)
                .texOffs(22,18).addBox(8.68F,-11.6F,-1.45F,1.22F,8.2F,5.75F)
                .texOffs(44,18).addBox(-6.05F,-10.65F,-6.95F,.9F,1.5F,11.12F)
                .texOffs(76,18).addBox(9.15F,-10.65F,-6.95F,.9F,1.5F,11.12F)
                .texOffs(0,39).addBox(-5.6F,-10.65F,-7.21F,15.2F,1.5F,.6F)
                .texOffs(39,39).addBox(-6.29F,-10.92F,-1F,.55F,2.05F,2.2F)
                .texOffs(63,39).addBox(9.75F,-10.92F,-1F,.55F,2.05F,2.2F),
                PartPose.offsetAndRotation(-2F,9F,12F,(float)Math.PI/2,0,0));
        return LayerDefinition.create(mesh,128,64);
    }
}
