package dev.minemods.cowardlymobs;

import dev.minemods.cowardlymobs.ai.MobAiRegistration;
import dev.minemods.cowardlymobs.hunger.config.ZombieHungerConfig;
import dev.minemods.cowardlymobs.hunger.event.ZombieHungerEvents;
import dev.minemods.cowardlymobs.jockey.SpiderJockeyConfig;
import dev.minemods.cowardlymobs.spider.SpiderInstinctConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CowardlyMobs.MOD_ID)
public final class CowardlyMobs {
    public static final String MOD_ID = "cowardlymobs";

    public CowardlyMobs(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ZombieHungerConfig.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, SpiderJockeyConfig.SPEC,
                "cowardlymobs-jockey-common.toml");
        container.registerConfig(ModConfig.Type.COMMON, SpiderInstinctConfig.SPEC,
                "cowardlymobs-spiders-common.toml");
        NeoForge.EVENT_BUS.addListener(MobAiRegistration::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(ZombieHungerEvents::onHurt);
    }
}
