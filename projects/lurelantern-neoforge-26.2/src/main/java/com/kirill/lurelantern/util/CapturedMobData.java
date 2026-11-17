package com.kirill.lurelantern.util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import java.util.Optional;
public final class CapturedMobData {
 private static final String ROOT="LureLantern",MOB="CapturedMob",ACTIVE="LureActive";
 public static CompoundTag root(ItemStack stack){
  CustomData data=stack.get(DataComponents.CUSTOM_DATA);
  return data==null?new CompoundTag():data.copyTag().getCompoundOrEmpty(ROOT).copy();
 }
 private static void save(ItemStack stack,CompoundTag data){
  CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.put(ROOT,data.copy()));
 }
 public static Optional<CompoundTag> mob(ItemStack stack){return root(stack).getCompound(MOB).map(CompoundTag::copy);}
 public static boolean hasMob(ItemStack stack){return mob(stack).isPresent();}
 public static void setMob(ItemStack stack,CompoundTag mob){CompoundTag r=root(stack);r.put(MOB,mob.copy());save(stack,r);}
 public static void clearMob(ItemStack stack){CompoundTag r=root(stack);r.remove(MOB);save(stack,r);}
 public static boolean isActive(ItemStack stack){return root(stack).getBooleanOr(ACTIVE,false);}
 public static void setActive(ItemStack stack,boolean active){CompoundTag r=root(stack);r.putBoolean(ACTIVE,active);save(stack,r);}
}
