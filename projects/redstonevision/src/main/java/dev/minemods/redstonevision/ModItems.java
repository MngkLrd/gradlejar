package dev.minemods.redstonevision;

import dev.minemods.redstonevision.item.EngineerGogglesItem;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, RedstoneVision.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RedstoneVision.MOD_ID);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ENGINEER_MATERIAL =
        ARMOR_MATERIALS.register("engineer", () -> {
            EnumMap<ArmorItem.Type,Integer> defense = new EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type type : ArmorItem.Type.values()) defense.put(type, 0);
            return new ArmorMaterial(defense, 0, SoundEvents.ARMOR_EQUIP_GENERIC,
                () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(RedstoneVision.MOD_ID, "engineer"))),
                0.0F, 0.0F);
        });
    public static final DeferredHolder<Item, ArmorItem> GOGGLES =
        ITEMS.register("engineer_goggles", () ->
            new EngineerGogglesItem(ENGINEER_MATERIAL, new Item.Properties().stacksTo(1)));
}
