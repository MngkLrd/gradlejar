package com.kirill.simplesorter.integration;

import com.kirill.simplesorter.SimpleSorterMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JEI/EMI read vanilla crafting recipes from the server recipe manager.
 * A modpack can remove them via KubeJS/CraftTweaker/datapacks: log the
 * authoritative state rather than assuming JAR resources are always active.
 */
@Mod.EventBusSubscriber(modid = SimpleSorterMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RecipeDiagnostics {
    private static final Logger LOG = LoggerFactory.getLogger("SimpleSorter/Recipes");

    private RecipeDiagnostics() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        var recipes = event.getServer().getRecipeManager();
        boolean coal = recipes.byKey(new ResourceLocation(
                SimpleSorterMod.MOD_ID, "sorting_wand")).isPresent();
        boolean charcoal = recipes.byKey(new ResourceLocation(
                SimpleSorterMod.MOD_ID, "sorting_wand_charcoal")).isPresent();
        if (coal || charcoal) {
            LOG.info("Sorting Wand crafting recipes registered: coal={}, charcoal={}. JEI/EMI can display active vanilla recipes.",
                    coal, charcoal);
        } else {
            LOG.warn("Sorting Wand crafting recipes are missing from the SERVER RecipeManager. "
                    + "Check pack recipe removals / KubeJS / CraftTweaker / datapack overrides; "
                    + "JEI/EMI cannot display deleted recipes.");
        }
    }
}
