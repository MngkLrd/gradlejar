package dev.minemods.thermal.client;

import dev.minemods.thermal.ThermalMod;
import dev.minemods.thermal.content.ThermalContent;
import dev.minemods.thermal.content.ThermalWearable;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Model variants are client-only. The ordinary armor layer copies body and limb poses. */
@EventBusSubscriber(modid=ThermalMod.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class GarmentModels {
    private static final Set<String> EXTENDED=Set.of("sun_hat","grass_hood","desert_headwear","fur_hat","leather_face_mask","fur_coat","grass_coat","straw_cape","waxed_cloak");
    private static final Map<String,HumanoidModel<LivingEntity>> MODELS=new HashMap<>();
    private GarmentModels(){}
    @SubscribeEvent public static void register(RegisterClientExtensionsEvent event){
        IClientItemExtensions extensions=new IClientItemExtensions(){
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){
                String name=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                return MODELS.computeIfAbsent(name,GarmentModels::bake);
            }
        };
        for(Item item:ThermalContent.creativeItems())if(item instanceof ThermalWearable)event.registerItem(extensions,item);
    }
    private static void box(PartDefinition part,String name,int u,int v,float x,float y,float z,float w,float h,float d){
        part.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d,new CubeDeformation(name.startsWith("cuff_")?.6f:0f)),PartPose.ZERO);
    }
    private static HumanoidModel<LivingEntity> bake(String id){
        float inflate=switch(id){
            case "fur_coat"->.85f;case "fur_boots"->.8f;case "fur_hat"->.65f;
            case "grass_hood","grass_coat"->.45f;
            case "grass_leggings"->.4f;case "grass_boots"->.35f;
            case "wool_underwear"->.35f;case "linen_clothes"->.35f;case "light_tunic"->.35f;
            case "desert_headwear"->.35f;case "leather_face_mask"->.55f;
            case "waxed_cloak"->.36f;case "straw_cape"->.5f;default->.4f;
        };
        MeshDefinition mesh=HumanoidModel.createMesh(new CubeDeformation(inflate),0);
        PartDefinition root=mesh.getRoot();
        PartDefinition head=root.getChild("head"),body=root.getChild("body");
        switch(id){
            case "sun_hat" -> {
                // Replace helmet shell with a brim above the eyes and a separate crown.
                root.addOrReplaceChild("head",CubeListBuilder.create()
                    .texOffs(0,32).addBox(-7,-6,-7,14,1,14)
                    .texOffs(0,48).addBox(-4.5f,-10,-4.5f,9,4,9,new CubeDeformation(.15f)),PartPose.ZERO);
            }
            case "grass_hood" -> {
                box(head,"woven_right",0,32,-5,-3,-3.5f,2,5,7);
                box(head,"woven_left",20,32,3,-3,-3.5f,2,5,7);
            }
            case "desert_headwear" -> box(head,"neck_wrap",0,32,-4.5f,-1,-4.5f,9,3,9);
            case "fur_hat" -> {
                box(head,"ear_right",0,32,-5.5f,-2,-3,2,4,6);
                box(head,"ear_left",20,32,3.5f,-2,-3,2,4,6);
            }
            case "leather_face_mask" -> box(head,"nose_panel",0,32,-3,-3,-5,6,3,1);
            case "fur_coat" -> {
                box(body,"collar",0,32,-5,-1,-3,10,2,6);
                box(root.getChild("right_arm"),"cuff_right",0,43,-3.5f,7,-2.5f,5,2,5);
                box(root.getChild("left_arm"),"cuff_left",20,43,-1.5f,7,-2.5f,5,2,5);
            }
            case "grass_coat" -> {
                box(body,"fringe_one",0,32,-4,11,-2.7f,2,4,1);
                box(body,"fringe_two",8,32,-1,11,-2.7f,2,3,1);
                box(body,"fringe_three",16,32,2,11,-2.7f,2,4,1);
                box(body,"fringe_back",24,32,-3,11,2,2,3,1);
            }
            case "straw_cape" -> {
                box(body,"shoulder_yoke",0,32,-6,0,-3,12,2,6);
                box(body,"rear_mat",0,43,-5,2,2.5f,10,10,1);
                box(body,"left_drape",24,43,-7,2,2.5f,2,9,1);
                box(body,"right_drape",32,43,5,2,2.5f,2,9,1);
            }
            case "waxed_cloak" -> {
                box(body,"long_back",0,32,-5,0,3,10,16,1);
                box(body,"lapel_left",24,32,-4,0,-3,4,8,1);
                box(body,"lapel_right",36,32,0,0,-3,4,8,1);
            }
            default -> { }
        }
        return new HumanoidModel<>(LayerDefinition.create(mesh,64,EXTENDED.contains(id)?64:32).bakeRoot());
    }
}
