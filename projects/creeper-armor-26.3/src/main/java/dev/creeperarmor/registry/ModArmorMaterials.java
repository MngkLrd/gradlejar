package dev.creeperarmor.registry;
import dev.creeperarmor.CreeperArmorMod;
import net.minecraft.Util;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import java.util.EnumMap;
public final class ModArmorMaterials {
    public static final ResourceKey<EquipmentAsset> ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(CreeperArmorMod.MOD_ID,"creeper"));
    public static final TagKey<Item> REPAIR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CreeperArmorMod.MOD_ID,"creeper_plate_repair"));
    public static final ArmorMaterial CREEPER = new ArmorMaterial(15,
        Util.make(new EnumMap<>(ArmorType.class), values -> {
            values.put(ArmorType.HELMET,3); values.put(ArmorType.CHESTPLATE,8);
            values.put(ArmorType.LEGGINGS,6); values.put(ArmorType.BOOTS,3);
            values.put(ArmorType.BODY,8);
        }),13,SoundEvents.ARMOR_EQUIP_CHAIN,2.5F,0.05F,REPAIR,ASSET);
    private ModArmorMaterials(){}
}
