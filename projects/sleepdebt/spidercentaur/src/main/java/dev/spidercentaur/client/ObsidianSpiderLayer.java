package dev.spidercentaur.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.spidercentaur.entity.SpiderCentaurEntity;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Sparse obsidian growths over the vanilla spider abdomen and thorax. */
public final class ObsidianSpiderLayer
        extends RenderLayer<SpiderCentaurEntity, SpiderModel<SpiderCentaurEntity>> {

    private static final ResourceLocation OBSIDIAN =
            ResourceLocation.fromNamespaceAndPath("spidercentaur", "textures/entity/obsidian_growth.png");
    private static final ResourceLocation VEINS =
            ResourceLocation.fromNamespaceAndPath("spidercentaur", "textures/entity/obsidian_veins.png");

    private final ModelPart growths;
    private final ModelPart cracks;

    public ObsidianSpiderLayer(RenderLayerParent<SpiderCentaurEntity, SpiderModel<SpiderCentaurEntity>> parent) {
        super(parent);
        // Vanilla spider's rear abdomen sits at (0, 15, 9); its top is around y=11.
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("growths", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 10.3F, 7.1F, 4.8F, 1.15F, 6.4F)
                .texOffs(0, 19).addBox(1.6F, 10.25F, 10.0F, 2.0F, 1.35F, 3.4F)
                .texOffs(0, 30).addBox(-5.35F, 12.1F, 9.1F, 1.2F, 3.3F, 3.6F)
                .texOffs(25, 0).addBox(4.2F, 12.3F, 6.8F, 1.25F, 2.6F, 4.0F)
                .texOffs(28, 20).addBox(-2.2F, 11.5F, -1.0F, 3.1F, 1.25F, 4.2F),
                PartPose.ZERO);
        this.growths = LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("growths");

        MeshDefinition veinsMesh = new MeshDefinition();
        veinsMesh.getRoot().addOrReplaceChild("cracks", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.9F, 10.21F, 9.1F, 0.48F, 0.15F, 3.3F)
                .texOffs(6, 0).addBox(-2.6F, 10.20F, 10.3F, 1.55F, 0.15F, 0.45F)
                .texOffs(0, 7).addBox(1.95F, 10.17F, 11.05F, 0.46F, 0.14F, 1.7F),
                PartPose.ZERO);
        this.cracks = LayerDefinition.create(veinsMesh, 16, 16).bakeRoot().getChild("cracks");
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light,
                       SpiderCentaurEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
        growths.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(OBSIDIAN)), light, overlay);
        cracks.render(pose, buffers.getBuffer(RenderType.eyes(VEINS)), 0xF000F0, overlay);
    }
}
