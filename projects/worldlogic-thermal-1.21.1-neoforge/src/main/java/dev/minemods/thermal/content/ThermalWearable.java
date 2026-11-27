package dev.minemods.thermal.content;

import dev.minemods.thermal.ThermalMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

/** Climate garments use armor slots and leather geometry, but provide no combat armor. */
public class ThermalWearable extends ArmorItem {
    public ThermalWearable(Type type, Properties properties) {
        super(ArmorMaterials.LEATHER, type, properties);
    }
    @Override public int getDefense(){return 0;}
    @Override public float getToughness(){return 0;}
    @Override public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(){return net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY;}
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                            ArmorMaterial.Layer layer, boolean innerModel) {
        String name = layer.dyeable() ? "blank" : BuiltInRegistries.ITEM.getKey(this).getPath();
        return ResourceLocation.fromNamespaceAndPath(ThermalMod.ID, "textures/models/armor/" + name + ".png");
    }
}
