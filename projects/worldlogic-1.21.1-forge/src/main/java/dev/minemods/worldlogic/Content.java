package dev.minemods.worldlogic;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.*;
public final class Content {
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,"worldlogic");
 public static final RegistryObject<Item> SPOILED=ITEMS.register("spoiled_food",()->new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(.1f).build())));
}
