package dev.norsegear.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;

/** An uneven three-piece pebble with clipped corners rather than a perfect cube. */
public final class SlingStoneModel {
    private SlingStoneModel() {}

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0)
            .addBox(-2.0F, -1.5F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.ZERO);
        root.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 8)
            .addBox(-1.0F, -2.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(8, 8)
            .addBox(-1.0F, 1.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }
}
