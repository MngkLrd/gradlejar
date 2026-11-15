package dev.creeperarmor.registry;
import dev.creeperarmor.CreeperArmorMod;
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
import java.util.Map;
public final class ModArmorMaterials {
    public static final ResourceKey<EquipmentAsset> ASSET =
        ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(CreeperArmorMod.MOD_ID,"creeper"));
    public static final TagKey<Item> REPAIR =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CreeperArmorMod.MOD_ID,"creeper_plate_repair"));
    private static Map<ArmorType,Integer> defense() {
        EnumMap<ArmorType,Integer> map=new EnumMap<>(ArmorType.class);
        map.put(ArmorType.HELMET,3);map.put(ArmorType.CHESTPLATE,8);
        map.put(ArmorType.LEGGINGS,6);map.put(ArmorType.BOOTS,3);
        map.put(ArmorType.BODY,8);
        return map;
    }
    public static final ArmorMaterial CREEPER = new ArmorMaterial(
        15, defense(), 13, SoundEvents.ARMOR_EQUIP_CHAIN, 2.5F, 0.05F, REPAIR, ASSET);
    private ModArmorMaterials(){}
}
