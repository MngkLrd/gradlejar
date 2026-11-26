package dev.minemods.sleepdebt.item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
/** Vanilla equipment slot and normal right-click swap, no accessory dependency. */
public final class SleepMaskItem extends Item implements Equipable {
 public SleepMaskItem(Properties properties){super(properties);}
 @Override public EquipmentSlot getEquipmentSlot(){return EquipmentSlot.HEAD;}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  return swapWithEquipmentSlot(this,level,player,hand);
 }
}
