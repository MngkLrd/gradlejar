package dev.norsegear.registry;

import dev.norsegear.NorseGear;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Real modded armor material, so equipped armor does not override vanilla iron armor textures. */
public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, NorseGear.MODID);

    public static final Holder<ArmorMaterial> IRONWEAVE = ARMOR_MATERIALS.register("ironweave", () -> {
        EnumMap<ArmorItem.Type, Integer> protection = new EnumMap<>(ArmorItem.Type.class);
        protection.put(ArmorItem.Type.HELMET, 2);
        protection.put(ArmorItem.Type.CHESTPLATE, 6);
        protection.put(ArmorItem.Type.LEGGINGS, 5);
        protection.put(ArmorItem.Type.BOOTS, 2);
        protection.put(ArmorItem.Type.BODY, 4);
        return new ArmorMaterial(protection, 9, SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ModItems.FORGED_IRON.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(NorseGear.MODID, "ironweave"))),
            0.0F, 0.0F);
    });

    private ModArmorMaterials() {}
}
