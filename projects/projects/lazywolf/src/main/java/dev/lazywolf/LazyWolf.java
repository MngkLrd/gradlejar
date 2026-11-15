package dev.lazywolf;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

@Mod(LazyWolf.ID)
public final class LazyWolf {
    public static final String ID = "lazywolf";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredItem<Item> HARNESS = ITEMS.registerSimpleItem(
            "wolf_harness", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> BELT = ITEMS.registerSimpleItem(
            "leash_belt", new Item.Properties().stacksTo(1));

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ID);
    public static final Supplier<AttachmentType<Boolean>> HAS_HARNESS = ATTACHMENTS.register(
            "harnessed", () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL).sync(ByteBufCodecs.BOOL).build());
    public static final Supplier<AttachmentType<ItemStackHandler>> PACK = ATTACHMENTS.register(
            "pack", () -> AttachmentType.serializable(() -> new ItemStackHandler(9)).build());

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    public static final DeferredHolder<MenuType<?>, MenuType<WolfPackMenu>> WOLF_MENU =
            MENUS.register("wolf_pack", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(WolfPackMenu::new));

    public LazyWolf(IEventBus modBus) {
        ITEMS.register(modBus);
        ATTACHMENTS.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(this::creative);
        NeoForge.EVENT_BUS.addListener(this::interactWolf);
        NeoForge.EVENT_BUS.addListener(this::wolfDrops);
    }

    private void creative(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(HARNESS.get());
            e.accept(BELT.get());
        }
    }

    private static boolean owner(Wolf wolf, Player player) {
        return wolf.isTame() && wolf.isOwnedBy(player);
    }

    private static void handled(PlayerInteractEvent.EntityInteract e) {
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }

    private void interactWolf(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getTarget() instanceof Wolf wolf)) return;
        Player player = e.getEntity();
        if (!owner(wolf, player)) return;
        ItemStack hand = player.getItemInHand(e.getHand());

        // A belt remains effective after being moved back into the inventory:
        // Minecraft stores a leash's holder as the player entity, not the held item.
        if (hand.is(BELT.get())) {
            handled(e);
            if (!wolf.level().isClientSide()) toggleLeash(wolf, player);
            return;
        }

        if (player.isShiftKeyDown()) {
            if (!wolf.getData(HAS_HARNESS.get()) && hand.is(HARNESS.get())) {
                handled(e);
                if (!wolf.level().isClientSide()) {
                    wolf.setData(HAS_HARNESS.get(), true);
                    if (!player.getAbilities().instabuild) hand.shrink(1);
                }
                return;
            }
            if (wolf.getData(HAS_HARNESS.get()) && hand.isEmpty()) {
                handled(e);
                if (!wolf.level().isClientSide()) {
                    ItemStackHandler inv = wolf.getData(PACK.get());
                    for (int i = 0; i < inv.getSlots(); i++) {
                        ItemStack drop = inv.extractItem(i, inv.getSlotLimit(i), false);
                        if (!drop.isEmpty()) wolf.spawnAtLocation(drop);
                    }
                    wolf.setData(HAS_HARNESS.get(), false);
                    give(player, new ItemStack(HARNESS.get()));
                }
                return;
            }
        }

        // An ordinary right click opens the pack, without forcing the wolf to sit.
        if (wolf.getData(HAS_HARNESS.get()) && !player.isShiftKeyDown()
                && !hand.is(Items.LEAD)) {
            handled(e);
            if (player instanceof ServerPlayer sp) {
                sp.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, p) -> new WolfPackMenu(containerId, inventory, wolf),
                        Component.translatable("menu.lazywolf.wolf_pack")
                ), buf -> buf.writeVarInt(wolf.getId()));
            }
        }
    }

    private void toggleLeash(Wolf wolf, Player player) {
        if (wolf.isLeashed()) {
            if (wolf.getLeashHolder() != player) return;
            wolf.dropLeash(true, false);
            give(player, new ItemStack(Items.LEAD));
            return;
        }
        int connected = player.level().getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(48.0D),
                mob -> mob.isLeashed() && mob.getLeashHolder() == player).size();
        if (connected >= 10) {
            player.displayClientMessage(Component.translatable("message.lazywolf.limit"), true);
            return;
        }
        if (!player.getAbilities().instabuild && !consumeLead(player)) {
            player.displayClientMessage(Component.translatable("message.lazywolf.need_lead"), true);
            return;
        }
        wolf.setOrderedToSit(false);
        wolf.setLeashedTo(player, true);
    }

    private static boolean consumeLead(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.LEAD)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private void wolfDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof Wolf wolf) || !wolf.getData(HAS_HARNESS.get())) return;
        e.getDrops().add(new ItemEntity(wolf.level(), wolf.getX(), wolf.getY(), wolf.getZ(), new ItemStack(HARNESS.get())));
        ItemStackHandler inv = wolf.getData(PACK.get());
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.extractItem(i, inv.getSlotLimit(i), false);
            if (!stack.isEmpty()) e.getDrops().add(new ItemEntity(wolf.level(), wolf.getX(), wolf.getY(), wolf.getZ(), stack));
        }
    }
}
