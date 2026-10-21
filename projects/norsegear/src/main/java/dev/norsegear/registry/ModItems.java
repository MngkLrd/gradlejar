package dev.norsegear.registry;

import dev.norsegear.NorseGear;
import dev.norsegear.item.FieldWhetstoneItem;
import dev.norsegear.item.SlingItem;
import dev.norsegear.item.BogIronItem;
import dev.norsegear.item.BossedShieldItem;
import dev.norsegear.item.WarHornItem;
import dev.norsegear.item.ThrownWeaponItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NorseGear.MODID);

    // Four crafting resources, with a small ironworking loop.
    public static final DeferredItem<BogIronItem> BOG_IRON = ITEMS.registerItem("bog_iron", BogIronItem::new);
    public static final DeferredItem<Item> FORGED_IRON = ITEMS.registerSimpleItem("forged_iron");
    public static final DeferredItem<Item> LEATHER_BINDING = ITEMS.registerSimpleItem("leather_binding");
    public static final DeferredItem<Item> CARVED_HORN = ITEMS.registerSimpleItem("carved_horn");

    // Five melee weapons. Iron-tier performance rather than power-creeping diamond.
    public static final DeferredItem<SwordItem> SEAX = ITEMS.register("seax", () ->
        new SwordItem(Tiers.IRON, new Item.Properties()
            .attributes(SwordItem.createAttributes(Tiers.IRON, 2.5F, -2.2F)).durability(245)));
    public static final DeferredItem<AxeItem> BEARDED_AXE = ITEMS.register("bearded_axe", () ->
        new AxeItem(Tiers.IRON, new Item.Properties()
            .attributes(AxeItem.createAttributes(Tiers.IRON, 5.0F, -3.0F)).durability(285)));
    public static final DeferredItem<AxeItem> DANE_AXE = ITEMS.register("dane_axe", () ->
        new AxeItem(Tiers.IRON, new Item.Properties()
            .attributes(AxeItem.createAttributes(Tiers.IRON, 7.0F, -3.4F)).durability(330)));
    public static final DeferredItem<SwordItem> VIKING_SWORD = ITEMS.register("viking_sword", () ->
        new SwordItem(Tiers.IRON, new Item.Properties()
            .attributes(SwordItem.createAttributes(Tiers.IRON, 3.0F, -2.4F)).durability(270)));
    public static final DeferredItem<SwordItem> SPEAR = ITEMS.register("spear", () ->
        new SwordItem(Tiers.IRON, new Item.Properties()
            .attributes(SwordItem.createAttributes(Tiers.IRON, 3.5F, -2.9F)).durability(240)));

    // Two different recoverable thrown projectiles.
    public static final DeferredItem<ThrownWeaponItem> JAVELIN = ITEMS.registerItem(
        "javelin", p -> new ThrownWeaponItem(p.stacksTo(16), true));
    public static final DeferredItem<ThrownWeaponItem> THROWING_AXE = ITEMS.registerItem(
        "throwing_axe", p -> new ThrownWeaponItem(p.stacksTo(16), false));

    public static final DeferredItem<ShieldItem> ROUND_SHIELD = ITEMS.registerItem(
        "round_shield", p -> new ShieldItem(p.durability(290)));

    // Sling uses crafted stone ammunition; shots can partially survive impact.
    public static final DeferredItem<SlingItem> SLING = ITEMS.registerItem(
        "sling", p -> new SlingItem(p.durability(220)));
    public static final DeferredItem<Item> STONE_SHOT = ITEMS.registerSimpleItem(
        "stone_shot", new Item.Properties().stacksTo(64));

    // Celtic decoration is cosmetic for now; both shields still use vanilla block behavior.
    public static final DeferredItem<ShieldItem> CELTIC_SHIELD = ITEMS.registerItem(
        "celtic_shield", p -> new ShieldItem(p.durability(340)));
    // Only when carried in offhand: +2 armor points, not a wearable chestpiece.
    public static final DeferredItem<BossedShieldItem> BOSSED_SHIELD = ITEMS.registerItem(
        "bossed_shield", p -> new BossedShieldItem(p.durability(410)
            .attributes(BossedShieldItem.offhandArmor())));

    // Four armor slots with custom placeholder layers and iron-grade defense.
    public static final DeferredItem<ArmorItem> NORSE_HELMET = armor("norse_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> NORSE_CHESTPLATE = armor("norse_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> NORSE_LEGGINGS = armor("norse_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> NORSE_BOOTS = armor("norse_boots", ArmorItem.Type.BOOTS);

    // Four utility items. Pitch flask is a crafting resource, not a magic potion.
    public static final DeferredItem<FieldWhetstoneItem> FIELD_WHETSTONE = ITEMS.registerItem(
        "field_whetstone", p -> new FieldWhetstoneItem(p.stacksTo(16)));
    public static final DeferredItem<WarHornItem> WAR_HORN = ITEMS.registerItem(
        "war_horn", p -> new WarHornItem(p.durability(48)));
    public static final DeferredItem<Item> TRAVEL_RATION = ITEMS.registerSimpleItem(
        "travel_ration", new Item.Properties().stacksTo(32).food(new FoodProperties.Builder()
            .nutrition(6).saturationModifier(0.45F).build()));
    public static final DeferredItem<Item> PITCH_FLASK = ITEMS.registerSimpleItem(
        "pitch_flask", new Item.Properties().stacksTo(16));

    private static DeferredItem<ArmorItem> armor(String id, ArmorItem.Type slot) {
        return ITEMS.register(id, () -> new ArmorItem(ModArmorMaterials.IRONWEAVE, slot,
            new Item.Properties().durability(slot.getDurability(15))));
    }

    private ModItems() {}
}
