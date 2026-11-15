package dev.minemods.traumacraft.menu;

import dev.minemods.traumacraft.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Two columns x two rows. One oversized medkit in row 0 takes the row 1 cell
 * immediately below it, without manufacturing a second phantom ItemStack.
 */
public final class MedicalPouchInventory extends SimpleContainer {
    private final ItemStack pouch;
    public MedicalPouchInventory(ItemStack pouch) {
        super(4);
        this.pouch=pouch;
        pouch.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
             .copyInto(getItems());
    }

    public static boolean isOversized(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.OVERSIZED_MEDKIT.get());
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (!stack.getItem().getClass().getName().endsWith("MedicalItem")) return false;
        if (stack.is(ModItems.MEDICAL_POUCH.get())) return false;
        if (slot >= 2 && isOversized(getItem(slot-2))) return false;
        if (isOversized(stack)) return slot < 2 && getItem(slot+2).isEmpty();
        return true;
    }

    @Override public void setChanged() {
        super.setChanged();
        if (!pouch.isEmpty()) {
            pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
        }
    }
}
