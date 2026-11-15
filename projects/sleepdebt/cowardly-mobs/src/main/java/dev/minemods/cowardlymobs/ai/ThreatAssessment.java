package dev.minemods.cowardlymobs.ai;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class ThreatAssessment {
    private ThreatAssessment() {}

    public static boolean isDangerous(Player player) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
            return false;
        }
        // Uses final vanilla/modded attributes instead of hardcoding a list of weapon IDs.
        return player.getArmorValue() >= AiBalance.THREAT_ARMOR_POINTS
                || player.getAttributeValue(Attributes.ATTACK_DAMAGE) >= AiBalance.THREAT_ATTACK_DAMAGE;
    }
}
