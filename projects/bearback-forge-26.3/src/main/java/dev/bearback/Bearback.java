package dev.bearback;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.TickEvent;

@Mod(Bearback.ID)
public final class Bearback {
  public static final String ID="bearback";
  public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ID);
  public static final RegistryObject<EntityType<BlackBear>> BLACK_BEAR=ENTITIES.register("black_bear",()->{
      var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(ID,"black_bear"));
      return EntityType.Builder.<BlackBear>of(BlackBear::new,MobCategory.CREATURE)
              .sized(1.4F,1.4F).clientTrackingRange(8).build(key);
  });
  public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
  public static final RegistryObject<Item> SADDLEBAGS=ITEMS.register("saddlebags",()->{
      var key=ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(ID,"saddlebags"));
      return new Item(new Item.Properties().setId(key).stacksTo(16));
  });
  public static final RegistryObject<Item> EGG=ITEMS.register("black_bear_spawn_egg",()->{
      var key=ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(ID,"black_bear_spawn_egg"));
      return new SpawnEggItem(new Item.Properties().setId(key).spawnEgg(BLACK_BEAR.get()));
  });

  public Bearback(FMLJavaModLoadingContext context){
      ENTITIES.register(context.getModBusGroup());
      ITEMS.register(context.getModBusGroup());
      EntityAttributeCreationEvent.BUS.addListener(e->e.put(BLACK_BEAR.get(),PolarBear.createAttributes().build()));
      BuildCreativeModeTabContentsEvent.BUS.addListener(e->{
          if(e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS))e.accept(EGG.get());
          if(e.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES))e.accept(SADDLEBAGS.get());
      });
      PlayerInteractEvent.EntityInteractSpecific.BUS.addListener(BearEvents::interact);
      EntityJoinLevelEvent.BUS.addListener(BearEvents::onJoin);
      TickEvent.ServerTickEvent.Post.BUS.addListener(e->BearEvents.tickLoaded(e.server()));
      LivingDropsEvent.BUS.addListener(BearCargo::onDrops);
      PlayerEvent.StartTracking.BUS.addListener(e->{
          if(e.getTarget() instanceof PolarBear b && e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)
              BearNetwork.syncTo(b,p);
      });
      BearNetwork.register();
      if(FMLEnvironment.dist==Dist.CLIENT)dev.bearback.client.ClientHooks.init();
  }
}
