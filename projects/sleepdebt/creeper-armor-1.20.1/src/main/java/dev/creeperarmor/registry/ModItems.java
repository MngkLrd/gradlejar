package dev.creeperarmor.registry;
import dev.creeperarmor.CreeperArmorMod;
import dev.creeperarmor.item.CreeperShieldItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,CreeperArmorMod.MOD_ID);
    public static final RegistryObject<Item> CREEPER_PLATE=ITEMS.register("creeper_plate",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> CREEPER_POWDER=ITEMS.register("creeper_powder",()->new Item(new Item.Properties()));
    public static final RegistryObject<ArmorItem> CREEPER_HELMET=armor("creeper_helmet",ArmorItem.Type.HELMET);
    public static final RegistryObject<ArmorItem> CREEPER_CHESTPLATE=armor("creeper_chestplate",ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<ArmorItem> CREEPER_LEGGINGS=armor("creeper_leggings",ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<ArmorItem> CREEPER_BOOTS=armor("creeper_boots",ArmorItem.Type.BOOTS);
    public static final RegistryObject<CreeperShieldItem> CREEPER_SHIELD=ITEMS.register("creeper_shield",()->new CreeperShieldItem(new Item.Properties().durability(500)));
    private static RegistryObject<ArmorItem> armor(String id,ArmorItem.Type type){return ITEMS.register(id,()->new ArmorItem(CreeperArmorMaterial.INSTANCE,type,new Item.Properties()));}
    private ModItems(){}
}
