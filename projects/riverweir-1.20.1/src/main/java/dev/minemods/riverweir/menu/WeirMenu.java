package dev.minemods.riverweir.menu;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.blockentity.WeirPostBlockEntity;
import dev.minemods.riverweir.logic.WeirTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class WeirMenu extends AbstractContainerMenu {
    private final BlockPos masterPos;
    private final int tierId;
    private final ContainerData data;
    private final int netSlots;
    private final int rows;

    public WeirMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), buffer.readVarInt());
    }

    public WeirMenu(int id, Inventory inventory, BlockPos position, int tierId) {
        super(RiverWeirMod.WEIR_MENU.get(), id);
        masterPos = position;
        this.tierId = tierId;
        netSlots = WeirTier.byId(tierId).storage;
        rows = WeirLayout.rows(netSlots);

        WeirPostBlockEntity be = inventory.player.level().getBlockEntity(position) instanceof WeirPostBlockEntity found ? found : null;
        IItemHandler handler = be == null ? new ItemStackHandler(27) : be.items;
        data = be != null && !inventory.player.level().isClientSide ? new ContainerData() {
            @Override public int get(int index) {
                return switch(index) {
                    case 0 -> be.wear;
                    case 1 -> be.clog;
                    case 2 -> be.baitCharges;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                if(index == 0) be.wear = value;
                else if(index == 1) be.clog = value;
                else if(index == 2) be.baitCharges = value;
            }
            @Override public int getCount() { return 3; }
        } : new SimpleContainerData(3);
        addDataSlots(data);

        // Vanilla chest texture: 17px header, then 18px per row of net slots.
        // Two-slot Jute is centered; the other net tiers use full rows of nine.
        for(int index = 0; index < netSlots; index++) {
            int x = netSlots == 2 ? 70 + index * 18 : 8 + (index % 9) * 18;
            int y = 18 + (index / 9) * 18;
            addSlot(new SlotItemHandler(handler, index, x, y) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }

        // The additional 18px information strip is between net rows and the
        // vanilla player-inventory panel. Both SLOT and TEXTURE use this offset.
        int playerMainY = WeirLayout.playerInventoryY(netSlots);
        int hotbarY = WeirLayout.playerHotbarY(netSlots);
        for(int row = 0; row < 3; row++)
            for(int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, playerMainY + row * 18));
        for(int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, hotbarY));
    }

    public int getRows() { return rows; }
    public int getNetSlots() { return netSlots; }
    public int getCondition() {
        return Mth.clamp(100 - (100 * data.get(0)) / WeirTier.byId(tierId).endurance, 0, 100);
    }
    public int getClog() { return Mth.clamp(data.get(1), 0, 100); }
    public int getBaitCharges() { return data.get(2); }
    public WeirTier getTier() { return WeirTier.byId(tierId); }

    @Override public boolean stillValid(Player player) {
        if (!(player.level().getBlockEntity(masterPos) instanceof WeirPostBlockEntity be) ||
            !be.master || be.other == null) return false;
        double x = Mth.clamp(player.getX(), Math.min(masterPos.getX(), be.other.getX()), Math.max(masterPos.getX(), be.other.getX()) + 1);
        double z = Mth.clamp(player.getZ(), Math.min(masterPos.getZ(), be.other.getZ()), Math.max(masterPos.getZ(), be.other.getZ()) + 1);
        double y = Mth.clamp(player.getY(), be.netY - be.maxDepth(), masterPos.getY() + 2);
        double dx = player.getX() - x, dy = player.getY() - y, dz = player.getZ() - z;
        return dx * dx + dy * dy + dz * dz <= 64;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if(index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if(!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if(index >= netSlots || !moveItemStackTo(stack, netSlots, slots.size(), true)) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
