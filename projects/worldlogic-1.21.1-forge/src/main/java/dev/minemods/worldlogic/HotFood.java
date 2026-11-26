package dev.minemods.worldlogic;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
public final class HotFood {
 public static final String KEY="worldlogic_hot_until";
 public static long now(ServerLevel l){return l.getServer().overworld().getGameTime();}
 public static void stamp(ItemStack stack,long until){if(Rules.on("hot_meal")&&!stack.isEmpty()&&stack.has(DataComponents.FOOD)&&until>0)CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putLong(KEY,until));}
 public static long deadline(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getLong(KEY);}
}
