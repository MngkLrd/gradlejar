package dev.christmasui;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import dev.christmasui.client.TitleSnow;

@Mod(value = "christmasui", dist = Dist.CLIENT)
public final class ChristmasUI {
    public ChristmasUI() {
        NeoForge.EVENT_BUS.addListener(TitleSnow::render);
    }
}
