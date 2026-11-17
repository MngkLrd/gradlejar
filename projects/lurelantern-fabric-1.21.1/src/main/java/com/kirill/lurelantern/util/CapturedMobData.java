package com.kirill.lurelantern.util;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.Optional;
public final class CapturedMobData {
 public static final String ROOT_TAG="LureLantern",CAPTURED_MOB_TAG="CapturedMob",LURE_ACTIVE_TAG="LureActive";
 private static CompoundTag getRoot(ItemStack stack){
   CustomData data=stack.get(DataComponents.CUSTOM_DATA);
   if(data==null)return new CompoundTag();
   CompoundTag tag=data.copyTag();
   return tag.contains(ROOT_TAG,10)?tag.getCompound(ROOT_TAG):new CompoundTag();
 }
 private static void saveRoot(ItemStack stack,CompoundTag root){
   CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->tag.put(ROOT_TAG,root.copy()));
 }
 public static Optional<CompoundTag> getCapturedMobTag(ItemStack stack){
   CompoundTag root=getRoot(stack);
   return root.contains(CAPTURED_MOB_TAG,10)?Optional.of(root.getCompound(CAPTURED_MOB_TAG).copy()):Optional.empty();
 }
 public static boolean hasCapturedMob(ItemStack stack){return getCapturedMobTag(stack).isPresent();}
 public static void setCapturedMobTag(ItemStack stack,CompoundTag tag){CompoundTag root=getRoot(stack);root.put(CAPTURED_MOB_TAG,tag.copy());saveRoot(stack,root);}
 public static void clearCapturedMobTag(ItemStack stack){CompoundTag root=getRoot(stack);root.remove(CAPTURED_MOB_TAG);saveRoot(stack,root);}
 public static void migrateLegacyBlockEntityData(ItemStack stack){}
 public static boolean isLureActive(ItemStack stack){return getRoot(stack).getBoolean(LURE_ACTIVE_TAG);}
 public static void setLureActive(ItemStack stack,boolean active){CompoundTag root=getRoot(stack);root.putBoolean(LURE_ACTIVE_TAG,active);saveRoot(stack,root);}
 public static Component getCapturedMobName(CompoundTag tag){return Component.literal(tag.getString("id"));}
}
