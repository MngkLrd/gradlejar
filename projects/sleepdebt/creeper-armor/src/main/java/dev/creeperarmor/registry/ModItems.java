package dev.creeperarmor.registry;

import dev.creeperarmor.CreeperArmorMod;
import dev.creeperarmor.item.CreeperShieldItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreeperArmorMod.MOD_ID);

    public static final DeferredItem<Item> CREEPER_PLATE = ITEMS.registerSimpleItem("creeper_plate");
    public static final DeferredItem<Item> CREEPER_POWDER = ITEMS.registerSimpleItem("creeper_powder");
    public static final DeferredItem<ArmorItem> CREEPER_HELMET = armor("creeper_helmet", ArmorItem.Type.HELMET, 165);
    public static final DeferredItem<ArmorItem> CREEPER_CHESTPLATE = armor("creeper_chestplate", ArmorItem.Type.CHESTPLATE, 240);
    public static final DeferredItem<ArmorItem> CREEPER_LEGGINGS = armor("creeper_leggings", ArmorItem.Type.LEGGINGS, 225);
    public static final DeferredItem<ArmorItem> CREEPER_BOOTS = armor("creeper_boots", ArmorItem.Type.BOOTS, 195);
    public static final DeferredItem<CreeperShieldItem> CREEPER_SHIELD = ITEMS.registerItem(
            "creeper_shield", CreeperShieldItem::new, new Item.Properties().durability(500));

    private static DeferredItem<ArmorItem> armor(String id, ArmorItem.Type type, int durability) {
        return ITEMS.registerItem(id,
                props -> new ArmorItem(ModArmorMaterials.CREEPER, type, props),
                new Item.Properties().durability(durability));
    }

    private ModItems() {}
}
