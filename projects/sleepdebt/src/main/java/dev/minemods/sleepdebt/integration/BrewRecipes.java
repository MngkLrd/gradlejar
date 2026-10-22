package dev.minemods.sleepdebt.integration;
import dev.minemods.sleepdebt.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.List;
public final class BrewRecipes {
 public record Brew(Item ingredient,Item output,String name) {}
 public static List<Brew> all(){return List.of(
  new Brew(ModItems.GROUNDS.get(),ModItems.COFFEE.get(),"coffee"),
  new Brew(ModItems.DRIED_HERBS.get(),ModItems.HERBAL_TEA.get(),"herbal_tea")
 );}
 private BrewRecipes(){}
}

