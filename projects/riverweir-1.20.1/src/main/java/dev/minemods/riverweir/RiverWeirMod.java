package dev.minemods.riverweir;

import dev.minemods.riverweir.block.WeirNetBlock;
import dev.minemods.riverweir.block.WeirPostBlock;
import dev.minemods.riverweir.blockentity.WeirPostBlockEntity;
import dev.minemods.riverweir.item.NetRollItem;
import dev.minemods.riverweir.menu.WeirMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(RiverWeirMod.ID)
public final class RiverWeirMod {
    public static final String ID = "riverweir";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);

    public static final RegistryObject<Block> POST = BLOCKS.register("weir_post", () -> new WeirPostBlock(
        BlockBehaviour.Properties.of().strength(1.7f).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> NET = BLOCKS.register("weir_net", () -> new WeirNetBlock(
        BlockBehaviour.Properties.of().strength(0.6f).sound(SoundType.WOOL).noOcclusion()));
    public static final RegistryObject<Item> POST_ITEM = ITEMS.register("weir_post", () -> new BlockItem(POST.get(), new Item.Properties()));
    public static final RegistryObject<Item> NET_ROLL = ITEMS.register("net_roll", () -> new NetRollItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> NET_TWINE = ITEMS.register("net_twine", () -> new Item(new Item.Properties()));

    public static final RegistryObject<BlockEntityType<WeirPostBlockEntity>> POST_BE = BLOCK_ENTITIES.register("weir_post",
        () -> BlockEntityType.Builder.of(WeirPostBlockEntity::new, POST.get()).build(null));
    public static final RegistryObject<MenuType<WeirMenu>> WEIR_MENU = MENUS.register("weir",
        () -> IForgeMenuType.create(WeirMenu::new));

    public RiverWeirMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
    }
}
