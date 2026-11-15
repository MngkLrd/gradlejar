package dev.kirill.embershelter.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Main inventory only: server-validated, all-or-nothing reservation. */
public final class InventoryUtil {
    private InventoryUtil() {}

    public static Item shortage(ServerPlayer player, Map<Item, Integer> cost) {
        for (var needed : cost.entrySet()) {
            int have = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(needed.getKey())) have += stack.getCount();
            }
            if (have < needed.getValue()) return needed.getKey();
        }
        return null;
    }

    public static void consume(ServerPlayer player, Map<Item, Integer> cost) {
        for (var needed : cost.entrySet()) {
            int remaining = needed.getValue();
            for (ItemStack stack : player.getInventory().items) {
                if (remaining == 0) break;
                if (stack.is(needed.getKey())) {
                    int taken = Math.min(stack.getCount(), remaining);
                    stack.shrink(taken);
                    remaining -= taken;
                }
            }
            if (remaining != 0) throw new IllegalStateException("Inventory changed during camp checkout");
        }
        player.getInventory().setChanged();
    }

    public static Map<Item, Integer> cost(List<dev.kirill.embershelter.camp.CampBuildStep> steps) {
        Map<Item, Integer> result = new LinkedHashMap<>();
        for (var step : steps) {
            Item item = step.state().getBlock().asItem();
            result.merge(item, 1, Integer::sum);
        }
        return result;
    }
}
