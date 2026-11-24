package dev.ataflore.catchtheblast;
import dev.ataflore.catchtheblast.item.BlastShieldItem;
import dev.ataflore.catchtheblast.combat.*;
import dev.ataflore.catchtheblast.config.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerPlayer;
public final class CatchTheBlast implements ModInitializer {
 public static final String ID="catchtheblast";
 private static final ResourceKey<Item> KEY=ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(ID,"blast_shield"));
 public static final Item BLAST_SHIELD=Registry.register(BuiltInRegistries.ITEM,KEY,new BlastShieldItem(BlastShieldItem.configure(new Item.Properties().setId(KEY))));
 public void onInitialize(){
  CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(e->e.accept(BLAST_SHIELD));
  ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,original,actual,blocked)->{if(blocked&&entity instanceof ServerPlayer player)BlastCombat.onBlock(player,source,original);});
  CommandRegistrationCallback.EVENT.register((dispatcher,access,env)->BlastCommands.register(dispatcher));
  ServerLifecycleEvents.SERVER_STARTED.register(BlastConfig::load);
  ServerLifecycleEvents.SERVER_STOPPED.register(TravellingWave::stop);
  ServerTickEvents.END_SERVER_TICK.register(TravellingWave::tick);
 }
}