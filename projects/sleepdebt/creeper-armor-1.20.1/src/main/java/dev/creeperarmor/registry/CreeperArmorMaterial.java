package dev.creeperarmor.registry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
public enum CreeperArmorMaterial implements ArmorMaterial {
    INSTANCE;
    @Override public int getDurabilityForType(ArmorItem.Type type) { return switch(type) {
        case BOOTS -> 195; case LEGGINGS -> 225; case CHESTPLATE -> 240; case HELMET -> 165;
    }; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return switch(type) {
        case BOOTS -> 3; case LEGGINGS -> 6; case CHESTPLATE -> 8; case HELMET -> 3;
    }; }
    @Override public int getEnchantmentValue(){return 13;}
    @Override public SoundEvent getEquipSound(){return SoundEvents.ARMOR_EQUIP_CHAIN;}
    @Override public Ingredient getRepairIngredient(){return Ingredient.of(ModItems.CREEPER_PLATE.get());}
    @Override public String getName(){return "creeperarmor:creeper";}
    @Override public float getToughness(){return 2.5F;}
    @Override public float getKnockbackResistance(){return 0.05F;}
}
