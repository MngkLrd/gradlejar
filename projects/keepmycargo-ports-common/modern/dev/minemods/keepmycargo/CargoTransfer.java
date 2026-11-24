package dev.minemods.keepmycargo;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.gamerules.GameRules;

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
        var cargo = stack.get(DataComponents.CONTAINER);
        return supported(stack) && cargo != null && cargo.nonEmptyItemCopyStream().findAny().isPresent();
    }
    public static long packedCount(ItemStack stack) {
        var cargo = stack.get(DataComponents.CONTAINER);
        return cargo == null ? 0 : cargo.nonEmptyItemCopyStream().count();
    }
    private static Item vehicleItem(Entity entity) {
        if (entity instanceof AbstractChestBoat boat) { ItemStack picked = boat.getPickResult(); return picked != null && supported(picked) ? picked.getItem() : null; }
        if (entity instanceof MinecartChest && entity.getType() == net.minecraft.world.entity.EntityType.CHEST_MINECART) return Items.CHEST_MINECART;
        if (entity instanceof MinecartHopper && entity.getType() == net.minecraft.world.entity.EntityType.HOPPER_MINECART) return Items.HOPPER_MINECART;
        return null;
    }
    // Refuse recursive packing, including packed cargo hidden inside a shulker.
    public static boolean containsPackedCargo(ItemStack stack, int depth) {
        if (stack.isEmpty()) return false;
        if (isPacked(stack)) return true;
        if (depth >= 16) return true;
        var contents = stack.get(DataComponents.CONTAINER);
        return contents != null && contents.nonEmptyItemCopyStream().anyMatch(child -> containsPackedCargo(child, depth + 1));
    }
    public static boolean packOnDestroy(Entity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level) || entity.isRemoved() ||
            !level.getGameRules().get(GameRules.ENTITY_DROPS)) return false;
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
        drop.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(snapshot));
        drop.set(DataComponents.MAX_STACK_SIZE, 1);
        if (entity.hasCustomName()) drop.set(DataComponents.CUSTOM_NAME, entity.getCustomName());
        // Only empty the live inventory after the packed item was successfully spawned.
        if (entity.spawnAtLocation(level, drop) == null) return false;
        cargo.clearContent();
        cargo.chestVehicleDestroyed(source, level, entity);
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
        ItemContainerContents contents = current.stack.get(DataComponents.CONTAINER);
        if (contents == null) return;
        NonNullList<ItemStack> slots = NonNullList.withSize(cargo.getContainerSize(), ItemStack.EMPTY);
        contents.copyInto(slots);
        cargo.setContainerLootTable(null);
        for (int i = 0; i < slots.size(); i++) cargo.setItem(i, slots.get(i));
        cargo.setChanged();
        if (current.stack.has(DataComponents.CUSTOM_NAME)) entity.setCustomName(current.stack.get(DataComponents.CUSTOM_NAME));
        current.restored = true;
    }
}
