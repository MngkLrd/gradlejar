package dev.minemods.redstonevision.menu;

import dev.minemods.redstonevision.ModItems;
import dev.minemods.redstonevision.ModMenus;
import dev.minemods.redstonevision.item.GogglesPower;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GogglesChargeMenu extends AbstractContainerMenu {
    private static final int PLAYER_START = 1;
    private static final int PLAYER_END = 37;
    private final SimpleContainer fuel = new SimpleContainer(1);
    private final InteractionHand hand;
    private final boolean equipped;
    private final int heldSlot;
    private final ItemStack original;
    private final DataSlot charge;
    private boolean loading;

    public GogglesChargeMenu(int id, Inventory inventory) {
        this(id, inventory, null, false);
    }
    public GogglesChargeMenu(int id, Inventory inventory, InteractionHand usedHand) {
        this(id, inventory, usedHand, false);
    }
    public GogglesChargeMenu(int id, Inventory inventory, boolean equipped) {
        this(id, inventory, null, equipped);
    }
    private GogglesChargeMenu(int id, Inventory inventory, InteractionHand usedHand, boolean isEquipped) {
        super(ModMenus.CHARGER.get(), id);
        hand = usedHand;
        equipped = isEquipped;
        heldSlot = inventory.selected;
        original = isEquipped ? inventory.player.getItemBySlot(EquipmentSlot.HEAD)
            : usedHand == null ? ItemStack.EMPTY : inventory.player.getItemInHand(usedHand);
        charge = usedHand == null && !isEquipped ? DataSlot.standalone() : new DataSlot() {
            @Override public int get() { return GogglesPower.ticks(original); }
            @Override public void set(int value) {}
        };
        addDataSlot(charge);
        addSlot(new Slot(fuel, 0, 80, 36) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.REDSTONE); }
            @Override public void setChanged() {
                super.setChanged();
                if (!inventory.player.level().isClientSide) autoLoad(inventory.player);
            }
        });
        for (int row=0; row<3; row++) for (int col=0; col<9; col++)
            addSlot(new Slot(inventory, 9 + col + row*9, 8 + col*18, 84 + row*18));
        for (int col=0; col<9; col++) {
            boolean locked = usedHand == InteractionHand.MAIN_HAND && col == heldSlot;
            addSlot(new Slot(inventory, col, 8 + col*18, 142) {
                @Override public boolean mayPickup(Player player) { return !locked && super.mayPickup(player); }
                @Override public boolean mayPlace(ItemStack stack) { return !locked && super.mayPlace(stack); }
            });
        }
    }
    private void autoLoad(Player player) {
        if (loading || !stillValid(player)) return;
        ItemStack dust = fuel.getItem(0);
        if (!dust.is(Items.REDSTONE)) return;
        int used = GogglesPower.addDust(original, dust.getCount());
        if (used == 0) return;
        loading = true;
        try {
            dust.shrink(used);
            if (dust.isEmpty()) fuel.setItem(0, ItemStack.EMPTY);
            else fuel.setChanged();
            player.getInventory().setChanged();
            broadcastChanges();
        } finally { loading = false; }
    }
    public int chargeTicks() { return charge.get(); }
    @Override public boolean stillValid(Player player) {
        if (equipped) return player.getItemBySlot(EquipmentSlot.HEAD) == original
            && original.is(ModItems.GOGGLES.get());
        return hand == null || (player.getInventory().selected == heldSlot
            && player.getItemInHand(hand) == original && original.is(ModItems.GOGGLES.get()));
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || player.level().isClientSide || !stillValid(player)) return false;
        autoLoad(player);
        return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack from = slot.getItem();
        ItemStack originalCopy = from.copy();
        if (index == 0) {
            if (!moveItemStackTo(from, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (from.is(Items.REDSTONE)) {
            if (!moveItemStackTo(from, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 28) {
            if (!moveItemStackTo(from, 28, 37, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(from, 1, 28, false)) return ItemStack.EMPTY;
        }
        if (from.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return originalCopy;
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            ItemStack leftover = fuel.removeItemNoUpdate(0);
            if (!leftover.isEmpty()) player.getInventory().placeItemBackInInventory(leftover);
        }
    }
}
