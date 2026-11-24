package dev.ataflore.catchtheblast.item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
public final class Charge {
 private Charge(){}
 public static final int MAX=150;
 public static int get(ItemStack stack){CompoundTag t=stack.getTag();return t==null?0:Math.max(0,Math.min(MAX,t.getInt("blastCharge")));}
 public static long deadline(ItemStack stack){CompoundTag t=stack.getTag();return t==null?0:t.getLong("blastDeadline");}
 public static void set(ItemStack stack,int value,long deadline){CompoundTag t=stack.getOrCreateTag();if(value<=0){t.remove("blastCharge");t.remove("blastDeadline");}else{t.putInt("blastCharge",Math.min(MAX,value));if(deadline>0)t.putLong("blastDeadline",deadline);else t.remove("blastDeadline");}}
 public static int stage(ItemStack stack){int v=get(stack);return v>100?3:v>=50?2:v>0?1:0;}
}