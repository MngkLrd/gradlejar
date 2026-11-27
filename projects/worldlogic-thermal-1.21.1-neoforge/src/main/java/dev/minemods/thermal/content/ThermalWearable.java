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

/** Keep leather's existing gameplay properties and geometry; only replace its two rendered layers. */
public class ThermalWearable extends ArmorItem {
    public ThermalWearable(Type type, Properties properties) {
        super(ArmorMaterials.LEATHER, type, properties);
    }
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                            ArmorMaterial.Layer layer, boolean innerModel) {
        String name = layer.dyeable() ? "blank" : BuiltInRegistries.ITEM.getKey(this).getPath();
        return ResourceLocation.fromNamespaceAndPath(ThermalMod.ID, "textures/models/armor/" + name + ".png");
    }
}
