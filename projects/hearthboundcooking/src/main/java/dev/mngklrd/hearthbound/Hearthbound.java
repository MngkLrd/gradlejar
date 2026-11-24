package dev.mngklrd.hearthbound;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

@Mod(Hearthbound.ID)
public final class Hearthbound {
    public static final String ID="hearthboundcooking";
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ID);
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ID);
    public static final RegistryObject<Block> CLAY=BLOCKS.register("clay_stockpot",()->new VesselBlock(3,BlockBehaviour.Properties.of().strength(1.4f).sound(SoundType.DECORATED_POT).noOcclusion()));
    public static final RegistryObject<Block> BRONZE=BLOCKS.register("bronze_cauldron",()->new VesselBlock(6,BlockBehaviour.Properties.of().strength(2.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Item> CLAY_ITEM=ITEMS.register("clay_stockpot",()->new BlockItem(CLAY.get(),new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BRONZE_ITEM=ITEMS.register("bronze_cauldron",()->new BlockItem(BRONZE.get(),new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> UNFIRED=ITEMS.register("unfired_stockpot",()->new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockEntityType<VesselEntity>> VESSEL=ENTITIES.register("vessel",()->BlockEntityType.Builder.of(VesselEntity::new,CLAY.get(),BRONZE.get()).build(null));
    public Hearthbound(){
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        bus.addListener(this::tab);
    }
    private void tab(BuildCreativeModeTabContentsEvent event){
        if(event.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS){event.accept(UNFIRED.get());event.accept(CLAY_ITEM.get());event.accept(BRONZE_ITEM.get());}
    }
}
