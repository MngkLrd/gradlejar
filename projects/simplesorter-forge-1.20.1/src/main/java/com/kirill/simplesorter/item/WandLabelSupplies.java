package com.kirill.simplesorter.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SignItem;

import java.util.ArrayList;
import java.util.List;

/** Server-only inventory of consumed label materials stored inside the wand. */
public final class WandLabelSupplies {
    private static final String KEY = "simplesorter_label_supplies";
    private static final int MAX_PER_TYPE = 128;
    private static final int MAX_SLOTS = 20;

    private WandLabelSupplies() {}

    private static int type(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() == Items.ITEM_FRAME || stack.getItem() == Items.GLOW_ITEM_FRAME) return 2;
        if (stack.getItem() instanceof SignItem) return 1;
        return 0;
    }

    public static int stored(ItemStack wand, int materialType) {
        int count = 0;
        for (ItemStack stack : load(wand)) {
            if (type(stack) == materialType) count += stack.getCount();
        }
        return count;
    }

    public static int addFromInventory(ServerPlayer player, ItemStack wand) {
        List<ItemStack> items = load(wand);
        int added = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            int kind = type(stack);
            if (kind == 0) continue;
            int allowance = MAX_PER_TYPE - count(items, kind);
            if (allowance <= 0) continue;

            int take = Math.min(stack.getCount(), allowance);
            ItemStack fragment = stack.copyWithCount(take);
            ItemStack match = null;
            for (ItemStack entry : items) {
                if (ItemStack.isSameItemSameTags(entry, fragment)) {
                    match = entry;
                    break;
                }
            }
            if (match == null) {
                if (items.size() >= MAX_SLOTS) continue;
                items.add(fragment);
            } else {
                match.grow(take);
            }
            stack.shrink(take);
            added += take;
        }
        save(wand, items);
        player.getInventory().setChanged();
        return added;
    }

    private static int count(List<ItemStack> items, int kind) {
        int result = 0;
        for (ItemStack entry : items) if (type(entry) == kind) result += entry.getCount();
        return result;
    }

    public static ItemStack consume(ItemStack wand, int labelStyle) {
        if (labelStyle != 1 && labelStyle != 2) return ItemStack.EMPTY;
        List<ItemStack> items = load(wand);
        for (ItemStack entry : items) {
            if (type(entry) != labelStyle) continue;
            ItemStack used = entry.copyWithCount(1);
            entry.shrink(1);
            items.removeIf(ItemStack::isEmpty);
            save(wand, items);
            return used;
        }
        return ItemStack.EMPTY;
    }

    public static void refund(ItemStack wand, ItemStack material) {
        if (material.isEmpty() || type(material) == 0) return;
        List<ItemStack> items = load(wand);
        for (ItemStack existing : items) {
            if (ItemStack.isSameItemSameTags(existing, material)) {
                existing.grow(1);
                save(wand, items);
                return;
            }
        }
        if (items.size() < MAX_SLOTS) {
            items.add(material.copyWithCount(1));
            save(wand, items);
        }
    }

    public static void dropOnBreak(ItemStack wand, ServerPlayer player) {
        for (ItemStack entry : load(wand)) {
            if (entry.isEmpty()) continue;
            Containers.dropItemStack(player.serverLevel(), player.getX(),
                    player.getY() + 0.5D, player.getZ(), entry.copy());
        }
        wand.getOrCreateTag().remove(KEY);
    }

    private static List<ItemStack> load(ItemStack wand) {
        List<ItemStack> result = new ArrayList<>();
        if (!wand.hasTag()) return result;
        ListTag tags = wand.getTag().getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(MAX_SLOTS, tags.size()); i++) {
            CompoundTag stored = tags.getCompound(i);
            ItemStack stack = ItemStack.of(stored);
            // Minecraft 1.20.1 ItemStack normally saves its count as a byte.
            // Keep the wand's supply count in an explicit bounded int instead,
            // so storing more than 64 materials cannot corrupt or lose items.
            if (stored.contains("SSCount")) stack.setCount(Math.max(0,
                    Math.min(MAX_PER_TYPE, stored.getInt("SSCount"))));
            if (type(stack) != 0 && !stack.isEmpty()) result.add(stack);
        }
        return result;
    }

    private static void save(ItemStack wand, List<ItemStack> items) {
        ListTag tag = new ListTag();
        for (ItemStack entry : items) {
            if (entry.isEmpty()) continue;
            CompoundTag material = entry.save(new CompoundTag());
            material.putInt("SSCount", entry.getCount());
            tag.add(material);
        }
        wand.getOrCreateTag().put(KEY, tag);
    }
}
