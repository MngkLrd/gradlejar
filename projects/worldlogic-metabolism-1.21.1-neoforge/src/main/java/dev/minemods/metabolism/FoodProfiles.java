package dev.minemods.metabolism;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
public final class FoodProfiles {
 public record Entry(MetabolicModel.Meal meal,boolean known){}
 /** Input carbohydrate includes fiber; stored carb is available carbohydrate, in grams per edible game portion. */
 private static Entry food(double p,double f,double totalCarb,double fiber,double a,double vitc,double b12,double iron,double ca,double mg,double toxin,double absorption){
  double c=Math.max(0,totalCarb-fiber);
  double kcal=4*p+9*f+4*c+2*fiber;
  return new Entry(new MetabolicModel.Meal(kcal,new double[]{p,f,c,fiber,a,vitc,b12,iron,ca,mg},toxin,absorption),true);
 }
 public static Optional<Entry> profile(ItemStack stack){return profile(stack,null);}
 public static Optional<Entry> profile(ItemStack stack,net.minecraft.world.entity.LivingEntity eater){
  // Ominous bottle is a non-nutritive effect carrier despite its vanilla FOOD component.
  if(stack.is(net.minecraft.world.item.Items.OMINOUS_BOTTLE))return Optional.empty();
  var component=stack.getFoodProperties(eater);
  if(stack.is(net.minecraft.world.item.Items.MILK_BUCKET))return Optional.of(food(8,8,12,0,60,0,1.1,.1,275,24,0,.93));
  if(component==null)return Optional.empty();
  String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
  // Aqua is authoritative for valid edible mass and preparation; never guess a whole catch or invalid portion.
  if(id.startsWith("aquaculture:raw_")||id.startsWith("aquaculture:cooked_"))return AquaFoodBridge.read(stack).flatMap(p->FishNutrition.portion(p.species(),p.grams(),p.cooked()));
  Entry e=switch(id){
   case "worldlogicterrain:strawberry" -> food(0.8,0.3,10,3,2,60,0,0.4,16,13,0,.92);
   case "worldlogicterrain:blueberry" -> food(0.7,0.3,14,2.4,3,10,0,0.3,6,6,0,.92);
   case "worldlogicterrain:raspberry" -> food(1.2,0.7,12,6.5,2,26,0,0.7,25,22,0,.92);
   case "worldlogicterrain:blackberry" -> food(1.4,0.5,10,5,11,21,0,0.6,29,20,0,.92);
   case "worldlogicterrain:grape" -> food(0.7,0.2,18,0.9,3,4,0,0.4,10,7,0,.92);
   case "worldlogicterrain:pineapple" -> food(0.8,0.2,21,2.3,5,76,0,0.5,21,19,0,.92);
   case "worldlogicterrain:tomato" -> food(1.1,0.2,5,1.5,50,17,0,0.3,12,13,0,.92);
   case "worldlogicterrain:cucumber" -> food(0.8,0.1,4,1,5,4,0,0.3,19,16,0,.92);
   case "worldlogicterrain:radish" -> food(0.8,0.1,4,2,0,18,0,0.4,30,12,0,.92);
   case "worldlogicterrain:onion" -> food(1.1,0.1,9,1.7,0,7,0,0.2,23,10,0,.92);
   case "worldlogicterrain:cabbage" -> food(1.3,0.1,6,2.5,5,37,0,0.5,40,12,0,.92);
   case "worldlogicterrain:bell_pepper" -> food(1,0.3,6,2,157,127,0,0.4,7,12,0,.92);
   case "worldlogicterrain:pear" -> food(0.6,0.2,27,5,2,7,0,0.3,16,12,0,.92);
   case "worldlogicterrain:plum" -> food(0.7,0.3,11,1.4,17,10,0,0.2,6,7,0,.92);
   case "worldlogicterrain:fig" -> food(0.8,0.3,19,3,7,2,0,0.4,35,17,0,.92);
   case "worldlogicterrain:mango" -> food(1.6,0.8,30,3.2,108,72,0,0.3,22,20,0,.92);
   case "worldlogicterrain:coconut" -> food(2,19,8,5,0,2,0,1.3,8,18,0,.92);
   case "worldlogicterrain:date" -> food(0.7,0,27,2.5,0,0,0,0.4,18,16,0,.92);
   case "worldlogicterrain:baobab_fruit" -> food(2,0.5,22,7,4,80,0,1,80,30,0,.92);
   case "minecraft:cooked_beef" -> food(26,15,0,0,5,0,2.5,2.6,18,23,0,.96);
   case "minecraft:cooked_mutton" -> food(25,20,0,0,0,0,2.6,2.1,17,24,0,.95);
   case "minecraft:cooked_porkchop" -> food(27,16,0,0,3,0,.7,1,18,25,0,.95);
   case "minecraft:cooked_chicken" -> food(30,7,0,0,12,0,.35,1.1,15,28,0,.97);
   case "minecraft:cooked_rabbit" -> food(29,8,0,0,0,0,6,2.3,19,29,0,.96);
   case "minecraft:beef" -> food(20,12,0,0,5,0,2,2.1,12,20,.04,.78);
   case "minecraft:mutton" -> food(18,18,0,0,0,0,2.2,1.6,16,20,.04,.76);
   case "minecraft:porkchop" -> food(18,15,0,0,8,0,.7,1.1,14,20,.08,.72);
   case "minecraft:chicken" -> food(18,8,0,0,15,0,.4,1,12,22,.15,.65);
   case "minecraft:rabbit" -> food(21,6,0,0,6,0,2.1,2.1,13,28,.06,.75);
   case "minecraft:cooked_cod" -> food(23,1.3,0,0,15,0,1.1,.55,15,30,0,.97);
   case "minecraft:cooked_salmon" -> food(23,12,0,0,15,0,2.8,.4,15,30,0,.95);
   case "minecraft:cod" -> food(18,1,0,0,10,0,1,.4,16,32,.04,.80);
   case "minecraft:salmon" -> food(20,13,0,0,12,0,3,.4,12,27,.04,.78);
   case "minecraft:tropical_fish" -> food(19,3,0,0,20,0,2,.7,20,26,.06,.75);
   case "minecraft:bread" -> food(7,2,40,4,0,0,0,2,45,38,0,.95);
   case "minecraft:baked_potato" -> food(4,0,35,3,1,20,0,1,18,38,0,.95);
   case "minecraft:potato" -> food(2,0,23,2,1,12,0,.7,12,20,.015,.65);
   case "minecraft:apple","minecraft:golden_apple","minecraft:enchanted_golden_apple" -> food(0.5,0.3,25,4,3,9,0,.2,11,9,0,.9);
   case "minecraft:carrot","minecraft:golden_carrot" -> food(1,0,12,4,1100,8,0,.4,42,16,0,.9);
   case "minecraft:beetroot" -> food(1.5,0,9,3,2,5,0,.8,16,23,0,.9);
   case "minecraft:melon_slice" -> food(.8,.2,11,.6,42,12,0,.3,10,15,0,.95);
   case "minecraft:sweet_berries" -> food(1,.4,12,4,8,24,0,.6,18,15,0,.92);
   case "minecraft:glow_berries" -> food(1,.4,14,3,12,18,0,.5,17,15,0,.9);
   case "minecraft:mushroom_stew" -> food(6,4,23,4,20,3,0,1.3,20,32,0,.95);
   case "minecraft:beetroot_soup" -> food(4,1,24,5,10,14,0,1.6,45,50,0,.95);
   case "minecraft:rabbit_stew" -> food(25,12,28,5,250,15,1.5,2.4,50,45,0,.96);
   case "minecraft:cookie" -> food(1.3,4,14,.7,0,0,0,.5,10,12,0,.97);
   case "minecraft:pumpkin_pie" -> food(5,11,38,3,190,2,.2,1,55,18,0,.95);
   case "minecraft:honey_bottle" -> food(0,0,45,0,0,0,0,.1,3,2,0,.99);
   case "minecraft:dried_kelp" -> food(2,.5,6,2,18,3,0,1,70,50,0,.85);
   case "minecraft:chorus_fruit" -> food(1,.3,22,4,25,12,0,.4,16,15,0,.88);
   case "minecraft:suspicious_stew" -> food(5,5,20,3,50,9,0,1,30,30,.02,.85);
   case "minecraft:rotten_flesh" -> food(12,6,0,0,0,0,0,1,10,10,.35,.4);
   case "minecraft:poisonous_potato" -> food(2,0,17,2,1,8,0,.5,10,16,.55,.4);
   case "minecraft:pufferfish" -> food(17,4,0,0,8,0,1,.4,15,15,.9,.3);
   case "minecraft:spider_eye" -> food(3,3,9,0,0,0,0,.4,5,5,.65,.3);
   default -> new Entry(new MetabolicModel.Meal(Math.max(0,component.nutrition()*38.0+component.saturation()*9.0),new double[MetabolicModel.N],0,.8),false);
  };return Optional.of(e);
 }
 static Entry cakeSlice(){return food(2,6,23,1,25,0,.1,.4,35,12,0,.95);}
 private FoodProfiles(){}
}
