package dev.anvilrightclickrepair;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(AnvilRightClickRepair.MOD_ID)
public final class AnvilRightClickRepair {
    public static final String MOD_ID = "anvilrightclickrepair";
    public AnvilRightClickRepair() {
        NeoForge.EVENT_BUS.addListener(RepairInteraction::onRightClick);
    }
}
