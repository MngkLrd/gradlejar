package dev.minemods.blowpipes;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class Blowpipes implements ModInitializer {
    public static final String ID = "blowpipes";
    private static EntityType<DartProjectile> dartType;
    private static Item defaultDart;

    private static ResourceLocation id(String name) {
        return /*VERSION_LOCATION*/;
    }

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, id(name), item);
    }

    public static EntityType<DartProjectile> DART_TYPE() { return dartType; }
    public static Item DEFAULT_DART() { return defaultDart; }

    @Override
    public void onInitialize() {
        register("bamboo_blowpipe", new BlowpipeItem(new Item.Properties().stacksTo(1), 0));
        register("hunting_blowpipe", new BlowpipeItem(new Item.Properties().stacksTo(1), 1));
        register("reinforced_blowpipe", new BlowpipeItem(new Item.Properties().stacksTo(1), 2));
        defaultDart = register("plain_dart", new Item(new Item.Properties()));
        for (String name : new String[]{"iron_dart", "poison_dart",
                "slowness_dart", "fire_dart", "sleep_dart"}) {
            register(name, new Item(new Item.Properties()));
        }
        dartType = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                id("dart"),
                EntityType.Builder.<DartProjectile>of(DartProjectile::new, MobCategory.MISC)
                        .sized(0.16F, 0.16F).clientTrackingRange(6).updateInterval(2).build("dart")
        );
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(contents -> {
            for (String name : new String[]{
                    "bamboo_blowpipe", "hunting_blowpipe", "reinforced_blowpipe",
                    "plain_dart", "iron_dart", "poison_dart", "slowness_dart",
                    "fire_dart", "sleep_dart"
            }) contents.accept(BuiltInRegistries.ITEM.get(id(name)));
        });
        BlowpipeEvents.register();
    }
}
