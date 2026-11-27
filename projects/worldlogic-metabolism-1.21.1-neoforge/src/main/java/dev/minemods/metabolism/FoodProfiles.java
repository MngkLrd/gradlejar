package dev.minemods.metabolism;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
public final class FoodProfiles {
 public record Entry(MetabolicModel.Meal meal,boolean known){}
 private static Entry food(double kcal,double p,double f,double c,double fiber,double a,double vitc,double b12,double iron,double ca,double mg,double toxin,double absorption){
  return new Entry(new MetabolicModel.Meal(kcal,new double[]{p,f,c,fiber,a,vitc,b12,iron,ca,mg},toxin,absorption),true);
 }
 public static Optional<Entry> profile(ItemStack stack){
  var component=stack.get(DataComponents.FOOD);if(component==null)return Optional.empty();
  String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
  Entry e=switch(id){
   case "minecraft:cooked_beef","minecraft:cooked_mutton" -> food(260,25,17,0,0,8,0,2.5,2.6,20,25,0,.98);
   case "minecraft:cooked_porkchop","minecraft:cooked_chicken","minecraft:cooked_rabbit" -> food(230,25,14,0,0,12,0,.6,1.3,15,28,0,.98);
   case "minecraft:beef","minecraft:porkchop","minecraft:mutton","minecraft:chicken","minecraft:rabbit" -> food(180,18,12,0,0,10,0,1,1.5,15,20,.05,.72);
   case "minecraft:cooked_cod","minecraft:cooked_salmon" -> food(190,24,10,0,0,30,0,3,1,25,30,0,.95);
   case "minecraft:cod","minecraft:salmon","minecraft:tropical_fish" -> food(130,20,5,0,0,25,0,2.5,1,20,24,.04,.78);
   case "minecraft:bread" -> food(210,7,2,40,4,0,0,0,2,45,38,0,.95);
   case "minecraft:baked_potato" -> food(155,4,0,35,3,1,20,0,1,18,38,0,.95);
   case "minecraft:potato" -> food(100,2,0,23,2,1,12,0,.7,12,20,.015,.65);
   case "minecraft:apple","minecraft:golden_apple","minecraft:enchanted_golden_apple" -> food(95,0.5,0.3,25,4,3,9,0,.2,11,9,0,.9);
   case "minecraft:carrot","minecraft:golden_carrot" -> food(55,1,0,12,4,1100,8,0,.4,42,16,0,.9);
   case "minecraft:beetroot" -> food(42,1.5,0,9,3,2,5,0,.8,16,23,0,.9);
   case "minecraft:melon_slice","minecraft:sweet_berries","minecraft:glow_berries" -> food(55,1,0,14,2,24,18,0,.5,16,15,0,.9);
   case "minecraft:mushroom_stew","minecraft:beetroot_soup","minecraft:rabbit_stew" -> food(180,9,6,25,4,75,12,.3,1.4,35,35,0,.95);
   case "minecraft:cookie","minecraft:pumpkin_pie","minecraft:honey_bottle" -> food(220,3,7,42,1,18,1,0,.8,25,14,0,.97);
   case "minecraft:rotten_flesh" -> food(120,12,6,0,0,0,0,0,1,10,10,.35,.4);
   case "minecraft:poisonous_potato","minecraft:pufferfish","minecraft:spider_eye" -> food(75,2,1,12,1,0,0,0,.2,5,5,.65,.3);
   default -> new Entry(new MetabolicModel.Meal(component.nutrition()*35.0,new double[MetabolicModel.N],0,.8),false);
  };return Optional.of(e);
 }
 private FoodProfiles(){}
}
