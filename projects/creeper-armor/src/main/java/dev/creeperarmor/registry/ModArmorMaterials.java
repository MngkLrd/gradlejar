package dev.creeperarmor.registry;

import dev.creeperarmor.CreeperArmorMod;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, CreeperArmorMod.MOD_ID);

    public static final Holder<ArmorMaterial> CREEPER = MATERIALS.register("creeper", () ->
            new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.HELMET, 2);
                        defense.put(ArmorItem.Type.CHESTPLATE, 6);
                        defense.put(ArmorItem.Type.LEGGINGS, 5);
                        defense.put(ArmorItem.Type.BOOTS, 2);
                        defense.put(ArmorItem.Type.BODY, 5);
                    }),
                    13,
                    SoundEvents.ARMOR_EQUIP_CHAIN,
                    () -> Ingredient.of(ModItems.CREEPER_PLATE.get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(CreeperArmorMod.MOD_ID, "creeper"))),
                    0.0F,
                    0.0F
            ));

    private ModArmorMaterials() {}
}
