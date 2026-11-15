package dev.creeperarmor.registry;
import dev.creeperarmor.CreeperArmorMod;
import dev.creeperarmor.item.CreeperShieldItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.component.BlocksAttacks;
import java.util.List;
import java.util.Optional;
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
    public static final DeferredItem<CreeperShieldItem> CREEPER_SHIELD=ITEMS.registerItem("creeper_shield",CreeperShieldItem::new, props->props.durability(500)
        .repairable(ModArmorMaterials.REPAIR)
        .equippableUnswappable(EquipmentSlot.OFFHAND)
        .delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
            0.0F,1.0F,
            List.of(new BlocksAttacks.DamageReduction(90.0F,Optional.empty(),0.0F,1.0F)),
            new BlocksAttacks.ItemDamageFunction(3.0F,1.0F,1.0F),
            Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
            Optional.of(SoundEvents.SHIELD_BLOCK),
            Optional.of(SoundEvents.SHIELD_BREAK)
        )).repairable(ModArmorMaterials.REPAIR));
    private static DeferredItem<Item> armor(String id,ArmorType type) {
        return ITEMS.registerItem(id,props->new Item(props.humanoidArmor(ModArmorMaterials.CREEPER,type)));
    }
    private ModItems(){}
}
