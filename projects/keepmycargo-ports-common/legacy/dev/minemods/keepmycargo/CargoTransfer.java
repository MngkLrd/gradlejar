package dev.minemods.keepmycargo;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraft.world.level.GameRules;

public final class CargoTransfer {
    private CargoTransfer() {}
    private static final ThreadLocal<Deque<Placement>> PLACEMENT = new ThreadLocal<>();

    private static final class Placement {
        final ItemStack stack;
        boolean restored;
        Placement(ItemStack stack) { this.stack = stack; }
    }
    public static boolean supported(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("minecraft") &&
            (id.getPath().endsWith("_chest_boat") || id.getPath().equals("bamboo_chest_raft")
             || stack.is(Items.CHEST_MINECART) || stack.is(Items.HOPPER_MINECART));
    }
    public static boolean isPacked(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("KeepMyCargo");
        return supported(stack) && tag != null && !tag.getList("Items", Tag.TAG_COMPOUND).isEmpty();
    }
    public static long packedCount(ItemStack stack) {
        CompoundTag tag = stack.getTagElement("KeepMyCargo");
        return tag == null ? 0 : tag.getList("Items", Tag.TAG_COMPOUND).size();
    }
    private static Item vehicleItem(Entity entity) {
        if (entity instanceof ChestBoat boat && entity.getType() == net.minecraft.world.entity.EntityType.CHEST_BOAT) return boat.getDropItem();
        if (entity instanceof MinecartChest && entity.getType() == net.minecraft.world.entity.EntityType.CHEST_MINECART) return Items.CHEST_MINECART;
        if (entity instanceof MinecartHopper && entity.getType() == net.minecraft.world.entity.EntityType.HOPPER_MINECART) return Items.HOPPER_MINECART;
        return null;
    }
    // Refuse recursive packing, including packed cargo hidden inside a shulker.
    public static boolean containsPackedCargo(ItemStack stack, int depth) {
        if (stack.isEmpty()) return false;
        if (isPacked(stack)) return true;
        if (depth >= 16) return true;
        CompoundTag block = stack.getTagElement("BlockEntityTag");
        if (block != null) {
            ListTag items = block.getList("Items", Tag.TAG_COMPOUND);
            for (int i=0; i<items.size(); i++) if (containsPackedCargo(ItemStack.of(items.getCompound(i)), depth+1)) return true;
        }
        return false;
    }
    public static boolean packOnDestroy(Entity entity, DamageSource source) {
        if (entity.level().isClientSide() || entity.isRemoved() ||
            !entity.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) return false;
        Item item = vehicleItem(entity);
        if (item == null || !(entity instanceof ContainerEntity cargo)) return false;
        cargo.unpackChestVehicleLootTable(null);
        NonNullList<ItemStack> snapshot = NonNullList.withSize(cargo.getContainerSize(), ItemStack.EMPTY);
        boolean nonempty = false;
        for (int i = 0; i < snapshot.size(); i++) {
            ItemStack stack = cargo.getItem(i);
            // Leave the original vanilla drop behavior intact for nested cargo.
            if (containsPackedCargo(stack, 0)) return false;
            snapshot.set(i, stack.copy());
            nonempty |= !stack.isEmpty();
        }
        if (!nonempty) return false;
        ItemStack drop = new ItemStack(item);
        CompoundTag packed = new CompoundTag();
        ContainerHelper.saveAllItems(packed, snapshot);
        drop.getOrCreateTag().put("KeepMyCargo", packed);
        if (entity.hasCustomName()) drop.setHoverName(entity.getCustomName());
        // Only empty the live inventory after the packed item was successfully spawned.
        if (entity.spawnAtLocation(drop) == null) return false;
        cargo.clearContent();
        cargo.chestVehicleDestroyed(source, entity.level(), entity);
        entity.discard();
        return true;
    }
    public static void beginPlacement(ItemStack stack) {
        Deque<Placement> contexts = PLACEMENT.get();
        if (contexts == null) { contexts = new ArrayDeque<>(); PLACEMENT.set(contexts); }
        contexts.push(new Placement(isPacked(stack) ? stack.copy() : ItemStack.EMPTY));
    }
    public static void endPlacement() {
        Deque<Placement> contexts = PLACEMENT.get();
        if (contexts != null) {
            contexts.pop();
            if (contexts.isEmpty()) PLACEMENT.remove();
        }
    }
    public static void restoreOnSpawn(Entity entity) {
        Deque<Placement> contexts = PLACEMENT.get();
        if (contexts == null || contexts.isEmpty()) return;
        Placement current = contexts.peek();
        if (current.restored || current.stack.isEmpty() || !(entity instanceof ContainerEntity cargo)
            || vehicleItem(entity) != current.stack.getItem()) return;
        CompoundTag contents = current.stack.getTagElement("KeepMyCargo");
        if (contents == null) return;
        NonNullList<ItemStack> slots = NonNullList.withSize(cargo.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(contents, slots);
        cargo.setLootTable(null);
        for (int i = 0; i < slots.size(); i++) cargo.setItem(i, slots.get(i));
        cargo.setChanged();
        if (current.stack.hasCustomHoverName()) entity.setCustomName(current.stack.getHoverName());
        current.restored = true;
    }
}
