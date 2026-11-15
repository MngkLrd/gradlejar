package dev.spidercentaur.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Render-only vanilla-geometry decorations, attached to the animated skeleton
 * head and chest. No Blockbench model and no client classes in common code.
 */
final class SkeletonDecorations {
    private static final ResourceLocation HAT =
            ResourceLocation.fromNamespaceAndPath("spidercentaur", "textures/entity/cowboy_hat.png");
    private static final ResourceLocation OBSIDIAN =
            ResourceLocation.fromNamespaceAndPath("spidercentaur", "textures/entity/obsidian_growth.png");
    private static final ResourceLocation VEINS =
            ResourceLocation.fromNamespaceAndPath("spidercentaur", "textures/entity/obsidian_veins.png");
    private static final ResourceLocation RED_EYE =
            ResourceLocation.fromNamespaceAndPath("spidercentaur", "textures/entity/red_eye.png");

    private final ModelPart hat;
    private final ModelPart hatBand;
    private final ModelPart skullGrowth;
    private final ModelPart skullVeins;
    private final ModelPart eyeSocket;
    private final ModelPart eyeGlow;
    private final ModelPart chestGrowth;
    private final ModelPart chestVeins;

    SkeletonDecorations() {
        this.hat = bake(64, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.7F, -9.2F, -6.3F, 13.4F, 0.8F, 12.6F)
                .texOffs(0, 28).addBox(-3.7F, -13.0F, -3.8F, 7.4F, 4.1F, 7.6F)
                // Turned-up outer edges create a readable cowboy-hat silhouette.
                .texOffs(0, 46).addBox(-7.8F, -10.5F, -5.5F, 1.6F, 1.6F, 11.0F)
                .texOffs(28, 46).addBox(6.2F, -10.5F, -5.5F, 1.6F, 1.6F, 11.0F));
        this.hatBand = bake(64, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.85F, -10.1F, -4.05F, 7.7F, 1.15F, 0.45F)
                .texOffs(0, 8).addBox(-3.85F, -10.1F, 3.6F, 7.7F, 1.15F, 0.45F)
                .texOffs(0, 16).addBox(-4.05F, -10.1F, -3.8F, 0.45F, 1.15F, 7.6F)
                .texOffs(20, 16).addBox(3.6F, -10.1F, -3.8F, 0.45F, 1.15F, 7.6F));

        // The left side of the skull is overtaken by a few chipped obsidian plates.
        // The undamaged right eye stays the vanilla black socket.
        this.skullGrowth = bake(64, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.25F, -7.55F, -4.35F, 3.9F, 2.7F, 0.7F)
                .texOffs(0, 12).addBox(-4.25F, -4.9F, -4.33F, 2.15F, 3.15F, 0.65F)
                .texOffs(0, 22).addBox(-4.55F, -6.6F, -3.8F, 0.7F, 4.9F, 4.8F)
                .texOffs(25, 12).addBox(-2.3F, -7.9F, -3.95F, 1.6F, 1.35F, 1.0F)
                .texOffs(0, 35).addBox(-3.55F, -1.9F, -4.3F, 1.55F, 1.4F, 0.6F));
        this.skullVeins = bake(16, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.9F, -6.6F, -4.44F, 0.45F, 1.95F, 0.12F)
                .texOffs(4, 0).addBox(-3.55F, -4.8F, -4.46F, 1.4F, 0.4F, 0.14F)
                .texOffs(9, 0).addBox(-4.4F, -3.75F, -4.44F, 0.45F, 1.6F, 0.12F));
        this.eyeSocket = bake(64, CubeListBuilder.create()
                .texOffs(0, 42).addBox(-3.35F, -5.0F, -4.65F, 2.3F, 2.2F, 0.4F));
        this.eyeGlow = bake(16, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.9F, -4.7F, -4.81F, 1.4F, 1.5F, 0.22F));

        this.chestGrowth = bake(64, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.35F, 2.3F, -2.45F, 2.5F, 4.55F, 0.75F)
                .texOffs(0, 15).addBox(-4.5F, 0.9F, -2.2F, 3.0F, 2.2F, 3.7F)
                .texOffs(0, 30).addBox(-4.45F, 5.0F, -1.9F, 0.85F, 3.0F, 3.9F));
        this.chestVeins = bake(16, CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.85F, 3.7F, -2.57F, 0.48F, 2.4F, 0.15F)
                .texOffs(4, 0).addBox(-3.4F, 4.85F, -2.58F, 1.15F, 0.42F, 0.15F));
    }

    void render(SkeletonModel<?> skeleton, PoseStack pose, MultiBufferSource buffers,
                int light, int overlay) {
        pose.pushPose();
        // Match the skeleton's head yaw and pitch during bow aiming.
        skeleton.head.translateAndRotate(pose);
        hat.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(HAT)), light, overlay);
        hatBand.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(OBSIDIAN)), light, overlay);
        skullGrowth.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(OBSIDIAN)), light, overlay);
        skullVeins.render(pose, buffers.getBuffer(RenderType.eyes(VEINS)), 0xF000F0, overlay);
        eyeSocket.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(OBSIDIAN)), light, overlay);
        // Only one eye. Emissive rendering remains visible even in complete darkness.
        eyeGlow.render(pose, buffers.getBuffer(RenderType.eyes(RED_EYE)), 0xF000F0, overlay);
        pose.popPose();

        pose.pushPose();
        skeleton.body.translateAndRotate(pose);
        chestGrowth.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(OBSIDIAN)), light, overlay);
        chestVeins.render(pose, buffers.getBuffer(RenderType.eyes(VEINS)), 0xF000F0, overlay);
        pose.popPose();
    }

    private static ModelPart bake(int atlasSize, CubeListBuilder boxes) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("detail", boxes, PartPose.ZERO);
        return LayerDefinition.create(mesh, atlasSize, atlasSize).bakeRoot().getChild("detail");
    }
}
