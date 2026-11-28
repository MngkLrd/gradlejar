package dev.minemods.worldlogic.client.compat;
import dev.minemods.worldlogic.client.SeasonDisplay;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import journeymap.api.v2.common.event.MinimapEventRegistry;
import journeymap.api.v2.client.event.InfoSlotDisplayEvent;
import net.minecraft.network.chat.Component;

/** JourneyMap 6 / API 2: optional loader-discovered plugin, not referenced by Core. */
@JourneyMapPlugin(apiVersion="2.0.0")
public final class JourneyMapSeasons implements IClientPlugin{
 private static final String MOD="worldlogic", KEY="worldlogic.journeymap.season";
 @Override public String getModId(){return MOD;}
 @Override public void initialize(IClientAPI api){
  ClientEventRegistry.INFO_SLOT_REGISTRY_EVENT.subscribe(MOD,e->e.register(MOD,Component.translatable(KEY),1000,SeasonDisplay::label));
  MinimapEventRegistry.INFO_SLOT_DISPLAY_EVENT.subscribe(MOD,e->{
   if(!SeasonDisplay.label().getString().isBlank())e.addLast(KEY,InfoSlotDisplayEvent.Position.Bottom);
  });
 }
}
