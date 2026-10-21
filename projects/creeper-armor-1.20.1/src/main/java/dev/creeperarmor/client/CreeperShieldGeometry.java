package dev.creeperarmor.client;

import dev.creeperarmor.CreeperArmorMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Original Creeper Shield cuboids. The grip uses the exact vanilla ShieldModel origin. */
public final class CreeperShieldGeometry {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(CreeperArmorMod.MOD_ID, "creeper_shield"), "main");

    private CreeperShieldGeometry() {}

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder plate = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -8.0F, -2.0F, 12.0F, 16.0F, 2.0F)
                .texOffs(30, 0).addBox(-7.0F, -9.0F, -2.6F, 1.0F, 18.0F, 3.0F)
                .texOffs(42, 0).addBox(6.0F, -9.0F, -2.6F, 1.0F, 18.0F, 3.0F)
                .texOffs(27, 23).addBox(-6.0F, -9.0F, -2.6F, 12.0F, 1.0F, 3.0F)
                .texOffs(27, 29).addBox(-6.0F, 8.0F, -2.6F, 12.0F, 1.0F, 3.0F)
                .texOffs(27, 35).addBox(-4.5F, 9.0F, -2.2F, 9.0F, 1.0F, 2.0F)
                .texOffs(50, 44).addBox(-5.5F, -7.0F, -3.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(54, 44).addBox(4.5F, -7.0F, -3.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(50, 48).addBox(-5.5F, 6.0F, -3.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(54, 48).addBox(4.5F, 6.0F, -3.0F, 1.0F, 1.0F, 1.0F);
        root.addOrReplaceChild("plate", plate, PartPose.ZERO);
        root.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(28, 45)
                .addBox(-1.0F, -3.0F, 0.0F, 2.0F, 6.0F, 4.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
