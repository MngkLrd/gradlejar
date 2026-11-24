package dev.minemods.potiontimerbar;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(PotionTimerBar.MOD_ID)
public final class PotionTimerBar {
    public static final String MOD_ID = "potiontimerbar";
    public PotionTimerBar() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC);
        }
    }
}
