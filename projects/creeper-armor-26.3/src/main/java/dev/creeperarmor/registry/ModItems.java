package dev.creeperarmor.registry;
import dev.creeperarmor.CreeperArmorMod;
import dev.creeperarmor.item.CreeperShieldItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class ModItems {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(CreeperArmorMod.MOD_ID);
    public static final DeferredItem<Item> CREEPER_PLATE=ITEMS.registerSimpleItem("creeper_plate");
    public static final DeferredItem<Item> CREEPER_POWDER=ITEMS.registerSimpleItem("creeper_powder");
    public static final DeferredItem<Item> CREEPER_HELMET=armor("creeper_helmet",ArmorType.HELMET);
    public static final DeferredItem<Item> CREEPER_CHESTPLATE=armor("creeper_chestplate",ArmorType.CHESTPLATE);
    public static final DeferredItem<Item> CREEPER_LEGGINGS=armor("creeper_leggings",ArmorType.LEGGINGS);
    public static final DeferredItem<Item> CREEPER_BOOTS=armor("creeper_boots",ArmorType.BOOTS);
    public static final DeferredItem<CreeperShieldItem> CREEPER_SHIELD=ITEMS.registerItem("creeper_shield",CreeperShieldItem::new, props->props.durability(500).repairable(ModArmorMaterials.REPAIR));
    private static DeferredItem<Item> armor(String id,ArmorType type) {
        return ITEMS.registerItem(id,props->new Item(props.humanoidArmor(ModArmorMaterials.CREEPER,type)));
    }
    private ModItems(){}
}
