package dev.minemods.fragileglass;

import dev.minemods.fragileglass.gameplay.GlassEvents;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;

@Mod(FragileGlass.ID)
public final class FragileGlass {
    public static final String ID = "fragileglass";
    public FragileGlass() {
        MinecraftForge.EVENT_BUS.addListener(GlassEvents::mining);
        MinecraftForge.EVENT_BUS.addListener(GlassEvents::impact);
        MinecraftForge.EVENT_BUS.addListener(GlassEvents::landing);
    }
}
