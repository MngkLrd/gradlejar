package dev.minemods.sleepdebt.integration;
import dev.minemods.sleepdebt.registry.ModItems;
import net.minecraft.world.item.*;
import java.util.List;
public final class BrewRecipes {
 public record Brew(Item base,Item ingredient,Item output,String name){}
 public static List<Brew> all(){return List.of(
  new Brew(Items.WATER_BUCKET,ModItems.GROUNDS.get(),ModItems.COFFEE.get(),"coffee"),
  new Brew(Items.WATER_BUCKET,ModItems.DRIED_HERBS.get(),ModItems.HERBAL_TEA.get(),"herbal_tea"),
  new Brew(ModItems.COFFEE.get(),Items.REDSTONE,ModItems.LONG.get(),"long_coffee"),
  new Brew(ModItems.COFFEE.get(),Items.GLOWSTONE_DUST,ModItems.STRONG.get(),"strong_coffee"),
  new Brew(ModItems.COFFEE.get(),Items.HONEY_BOTTLE,ModItems.CARAMEL_COFFEE.get(),"caramel_coffee"),
  new Brew(ModItems.STRONG.get(),Items.SUGAR,ModItems.ENERGY.get(),"energy_drink"),
  new Brew(ModItems.ENERGY.get(),Items.REDSTONE,ModItems.ENERGY_LONG.get(),"long_energy_drink"),
  new Brew(ModItems.ENERGY.get(),Items.GLOWSTONE_DUST,ModItems.ENERGY_STRONG.get(),"strong_energy_drink"),
  new Brew(Items.MILK_BUCKET,Items.SUGAR,ModItems.WARM_MILK.get(),"warm_milk"),
  new Brew(ModItems.HERBAL_TEA.get(),Items.HONEY_BOTTLE,ModItems.DREAM_TEA.get(),"dream_tea"),
  new Brew(ModItems.HERBAL_TEA.get(),ModItems.GROUNDS.get(),ModItems.BITTER_TONIC.get(),"bitter_tonic")
 );}
 public static boolean acceptsBase(ItemStack s){return all().stream().anyMatch(r->s.is(r.base()));}
 public static boolean acceptsIngredient(ItemStack s){return all().stream().anyMatch(r->s.is(r.ingredient()));}
 public static ItemStack result(ItemStack a,ItemStack b){for(Brew r:all())if(a.is(r.base())&&b.is(r.ingredient()))return new ItemStack(r.output());return ItemStack.EMPTY;}
 private BrewRecipes(){}
}
