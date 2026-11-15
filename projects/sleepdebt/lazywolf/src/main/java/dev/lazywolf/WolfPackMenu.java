package dev.lazywolf;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class WolfPackMenu extends AbstractContainerMenu {
    private final Wolf wolf;
    private final ItemStackHandler bag;

    public WolfPackMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, resolveWolf(playerInv, buf.readVarInt()));
    }

    private static Wolf resolveWolf(Inventory inv, int id) {
        Entity e = inv.player.level().getEntity(id);
        return e instanceof Wolf wolf ? wolf : null;
    }

    public WolfPackMenu(int containerId, Inventory playerInv, Wolf wolf) {
        super(LazyWolf.WOLF_MENU.get(), containerId);
        this.wolf = wolf;
        this.bag = wolf == null ? new ItemStackHandler(9) : wolf.getData(LazyWolf.PACK.get());
        for (int col = 0; col < 9; col++)
            addSlot(new SlotItemHandler(bag, col, 8 + col * 18, 18));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, 9 + row * 9 + col, 8 + col * 18, 50 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, 8 + col * 18, 108));
    }

    @Override
    public boolean stillValid(Player player) {
        return wolf != null && wolf.isAlive() && wolf.distanceToSqr(player) <= 64.0D
                && wolf.isTame() && wolf.isOwnedBy(player) && wolf.getData(LazyWolf.HAS_HARNESS.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack result = original.copy();
        if (index < 9) {
            if (!moveItemStackTo(original, 9, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(original, 0, 9, false)) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
