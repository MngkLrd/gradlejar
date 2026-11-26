package dev.minemods.worldlogic;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.registries.BuiltInRegistries;
public final class Material {
 public static boolean block(BlockState s,String tag){return s.is(TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("worldlogic",tag)));}
 public static boolean item(ItemStack s,String tag){return s.is(TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("worldlogic",tag)));}
 public static double weight(ItemStack s){
  if(s.isEmpty()) return 0;
  if(item(s,"heavy"))return 2;
  if(item(s,"light"))return .2;
  String id=BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
  if(id.contains("netherite")||id.contains("anvil")||id.contains("gold"))return 1.5;
  if(id.contains("iron")||id.contains("stone")||id.contains("copper"))return 1;
  return .5;
 }
 public static double mass(Entity e){
  if(e instanceof ItemEntity i)return Math.max(.15,Math.min(4,weight(i.getItem())*(1+Math.log(i.getItem().getCount())*.15)));
  double m=Math.max(.25,Math.min(4,e.getBbWidth()*e.getBbWidth()*e.getBbHeight()/0.65));
  if(e instanceof LivingEntity l)for(ItemStack s:l.getArmorSlots())m+=Math.min(.14,weight(s)*.07);
  return m;
 }
 public static String family(BlockState s){
  for(String k:new String[]{"ricochet_metal","ricochet_stone","wood","soft_landing","glass","unstable"})if(block(s,k))return k;
  return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
 }
 public static double surfaceBounce(BlockState s){return block(s,"ricochet_metal")?.70:block(s,"ricochet_stone")?.50:block(s,"wood")?.18:0;}
 public static double armorNoise(LivingEntity e){
  double n=1;
  for(ItemStack s:e.getArmorSlots()){if(s.isEmpty())continue;String p=BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
  n+=p.contains("chainmail")?.12:p.contains("iron")?.08:p.contains("leather")?-.06:.025;}
  return n;
 }
}
