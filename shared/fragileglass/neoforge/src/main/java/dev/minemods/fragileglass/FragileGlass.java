package dev.minemods.fragileglass;

import dev.minemods.fragileglass.gameplay.GlassEvents;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(FragileGlass.ID)
public final class FragileGlass {
    public static final String ID = "fragileglass";
    public FragileGlass() {
        NeoForge.EVENT_BUS.addListener(GlassEvents::mining);
        NeoForge.EVENT_BUS.addListener(GlassEvents::impact);
        NeoForge.EVENT_BUS.addListener(GlassEvents::landing);
    }
}
