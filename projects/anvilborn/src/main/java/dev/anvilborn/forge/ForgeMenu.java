package dev.anvilborn.forge;

import dev.anvilborn.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ForgeMenu extends AbstractContainerMenu {
    private final Container input = new SimpleContainer(1);
    private final ContainerData feedback = new SimpleContainerData(2);
    private final Level level;
    private final BlockPos pos;
    private final boolean serverMenu;

    public ForgeMenu(int id, Inventory inventory) { this(id, inventory, false, BlockPos.ZERO); }

    public ForgeMenu(int id, Inventory inventory, boolean serverMenu, BlockPos pos) {
        super(ModMenus.FORGE.get(), id);
        this.level = inventory.player.level();
        this.pos = pos;
        this.serverMenu = serverMenu;
        addSlot(new Slot(input, 0, 80, 38) {
            @Override public boolean mayPlace(ItemStack stack) { return ForgeRules.eligible(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(inventory, x + y * 9 + 9, 8 + x * 18, 103 + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) addSlot(new Slot(inventory, x, 8 + x * 18, 161));
        addDataSlots(feedback);
    }

    public ItemStack workpiece() { return input.getItem(0); }
    public int resultCode() { return feedback.get(0); }
    public int sequence() { return feedback.get(1); }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || player.level().isClientSide || !stillValid(player)) return false;
        ItemStack stack = workpiece();
        if (!ForgeRules.eligible(stack) || ForgeRules.data(stack).level() >= ForgeRules.MAX_LEVEL) return false;
        if (!player.getAbilities().instabuild && player.experienceLevel < 1) return false;
        // Charge before roll, every hit has a material XP cost.
        if (!player.getAbilities().instabuild) player.giveExperienceLevels(-1);
        int result = ForgeRules.strike(stack, player.getRandom());
        if (result == 4) input.setItem(0, ItemStack.EMPTY);
        else input.setChanged();
        feedback.set(0, result);
        feedback.set(1, (sequence() + 1) % 30000);
        level.playSound(null, pos, result == 4 ? SoundEvents.ANVIL_BREAK : SoundEvents.ANVIL_USE,
            SoundSource.BLOCKS, result == 4 ? 0.9f : 0.55f, 0.9f + player.getRandom().nextFloat() * 0.24f);
        broadcastChanges();
        return true;
    }

    @Override public boolean stillValid(Player player) {
        if (!serverMenu) return true;
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64
            && level.getBlockState(pos).is(BlockTags.ANVIL);
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (serverMenu && !player.level().isClientSide) {
            clearContainer(player, input);
        }
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else {
            if (!ForgeRules.eligible(stack) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
