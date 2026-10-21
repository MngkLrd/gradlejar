package dev.bearback;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Inventory remains attached to the individual bear's saved data, not to the rider
 * or a global world inventory. The cache values hold only weak owner references,
 * so unloaded bears cannot become immortal through this map.
 */
public final class BearCargo {
    public static final int SLOTS = 18;
    private static final String ITEMS_TAG = "BearbackCargo";
    private static final Map<PolarBear, CargoContainer> CACHE = new WeakHashMap<>();

    private BearCargo() {}

    public static CargoContainer inventory(PolarBear bear) {
        return CACHE.computeIfAbsent(bear, CargoContainer::new);
    }

    public static void open(ServerPlayer player, PolarBear bear) {
        if (!bear.isAlive() || bear.isBaby()
                || !BearEvents.ownedBy(bear, player)
                || !bear.getData(Bearback.BAGS.get())
                || (player.getVehicle() != bear && player.distanceToSqr(bear) > 64)) return;

        CargoContainer cargo = inventory(bear);
        player.openMenu(new SimpleMenuProvider(
                (windowId, inventory, user) -> new ChestMenu(MenuType.GENERIC_9x2, windowId, inventory, cargo, 2),
                Component.translatable("container.bearback.saddlebags")));
    }

    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof PolarBear bear) || bear.level().isClientSide()) return;

        // Dropped as ordinary ground items, not tied to vanilla mob-loot tables.
        if (bear.getData(Bearback.BAGS.get())) {
            CargoContainer cargo = inventory(bear);
            for (int slot = 0; slot < SLOTS; slot++) {
                ItemStack stack = cargo.removeItemNoUpdate(slot);
                if (!stack.isEmpty()) {
                    event.getDrops().add(new ItemEntity(bear.level(),
                            bear.getX(), bear.getY() + 0.35D, bear.getZ(), stack));
                }
            }
            cargo.setChanged(); // Prevent duplicate drops if death processing is retried.
            event.getDrops().add(new ItemEntity(bear.level(),
                    bear.getX(), bear.getY() + 0.35D, bear.getZ(),
                    new ItemStack(Bearback.SADDLEBAGS.get())));
            bear.setData(Bearback.BAGS.get(), false);
        }
        if (bear.getData(Bearback.SADDLED.get())) {
            event.getDrops().add(new ItemEntity(bear.level(),
                    bear.getX(), bear.getY() + 0.35D, bear.getZ(), new ItemStack(Items.SADDLE)));
            bear.setData(Bearback.SADDLED.get(), false);
        }
    }

    public static final class CargoContainer extends SimpleContainer {
        private final WeakReference<PolarBear> bearer;
        private boolean loading = true;

        private CargoContainer(PolarBear bear) {
            super(SLOTS);
            this.bearer = new WeakReference<>(bear);
            CompoundTag persistent = bear.getPersistentData();
            if (persistent.contains(ITEMS_TAG, Tag.TAG_LIST)) {
                fromTag(persistent.getList(ITEMS_TAG, Tag.TAG_COMPOUND),
                        bear.level().registryAccess());
            }
            loading = false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            PolarBear bear = bearer.get();
            if (!loading && bear != null && !bear.level().isClientSide()) {
                bear.getPersistentData().put(ITEMS_TAG, createTag(bear.level().registryAccess()));
            }
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            PolarBear bear = bearer.get();
            return bear != null && bear.isAlive()
                    && bear.getData(Bearback.BAGS.get())
                    && (player.getVehicle() == bear || player.distanceToSqr(bear) <= 64);
        }
    }
}
