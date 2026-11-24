package com.kirill.simplesorter.sorting;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable evidence-based item identity. A profile is not a chest assignment.
 * The adaptive planner can choose any depth of the hierarchy per activation.
 * No translated names, mod-specific class references or ItemStack mutations.
 */
public record SemanticItemProfile(
        String itemId, String category, String legacyFamily, String group,
        String material, String form, String purpose, boolean moveAllowed,
        Confidence confidence, List<String> evidence) {
    public enum Confidence { UNKNOWN, CATEGORY, REGISTRY_PATTERN, TAG, EXPLICIT }
    public SemanticItemProfile {
        evidence = List.copyOf(evidence);
    }

    /** Unknown materials are empty, never an invented shared "unknown material". */
    public boolean hasMaterial() { return !material.isEmpty(); }

    public List<String> hierarchy() {
        List<String> keys = new ArrayList<>();
        keys.add(group);
        if ("unknown".equals(group)) {
            int at = itemId.indexOf(':');
            keys.add("unknown/" + (at < 0 ? "minecraft" : itemId.substring(0,at)));
        } else if (hasMaterial()) {
            keys.add(group + "/material/" + material);
            if (!form.isEmpty()) keys.add(group + "/material/" + material + "/" + form);
        } else {
            keys.add(group + "/family/" + legacyFamily);
            if (!form.isEmpty()) keys.add(group + "/family/" + legacyFamily + "/" + form);
        }
        keys.add("item/" + itemId);
        return List.copyOf(keys);
    }

    /**
     * A bounded semantic affinity, independent of slot counts and coordinates.
     * Strong material identity is meaningful only within the same purpose group.
     * Exact item identity is not stackability: NBT checks belong to the mover.
     */
    public int affinity(SemanticItemProfile other) {
        if(group.startsWith("mod/")&&other.group.startsWith("mod/"))
            return group.equals(other.group)?100:0;
        if (!moveAllowed || !other.moveAllowed) return 0;
        if (itemId.equals(other.itemId) && group.equals(other.group)
                && purpose.equals(other.purpose) && form.equals(other.form)) return 100;
        if (!group.equals(other.group)) return 0;
        if ("unknown".equals(group)) return 0;
        if (hasMaterial() && other.hasMaterial() && material.equals(other.material)) {
            if ("equipment".equals(group) && !purpose.equals(other.purpose)) return 45;
            return form.equals(other.form) ? 95 : 85;
        }
        if (legacyFamily.equals(other.legacyFamily)) return 65;
        return 40;
    }
}
