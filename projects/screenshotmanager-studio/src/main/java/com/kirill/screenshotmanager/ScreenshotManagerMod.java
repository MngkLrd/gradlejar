package com.kirill.screenshotmanager;
import com.kirill.screenshotmanager.client.StudioClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
@Mod(value="screenshotmanager", dist=Dist.CLIENT)
public final class ScreenshotManagerMod {
    public ScreenshotManagerMod(IEventBus bus) { StudioClient.init(bus); }
}
