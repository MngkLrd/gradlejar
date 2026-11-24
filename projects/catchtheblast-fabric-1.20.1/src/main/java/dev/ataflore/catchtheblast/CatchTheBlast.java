package dev.ataflore.catchtheblast;
import dev.ataflore.catchtheblast.item.BlastShieldItem;
import dev.ataflore.catchtheblast.combat.TravellingWave;
import dev.ataflore.catchtheblast.config.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
public final class CatchTheBlast implements ModInitializer {
 public static final String ID="catchtheblast";
 public static final Item BLAST_SHIELD=Registry.register(BuiltInRegistries.ITEM,new ResourceLocation(ID,"blast_shield"),new BlastShieldItem(new Item.Properties().durability(512)));
 public void onInitialize(){
  ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries->entries.accept(BLAST_SHIELD));
  CommandRegistrationCallback.EVENT.register((dispatcher,access,env)->BlastCommands.register(dispatcher));
  ServerLifecycleEvents.SERVER_STARTED.register(BlastConfig::load);
  ServerLifecycleEvents.SERVER_STOPPED.register(TravellingWave::stop);
  ServerTickEvents.END_SERVER_TICK.register(TravellingWave::tick);
 }
}