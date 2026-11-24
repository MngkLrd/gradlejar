package com.kirill.simplesorter.settings;

public enum SortingMode {
    MATCH_EXISTING(0, "match_existing", "Match Existing", "screen.simplesorter.wand_settings.mode.match_existing", "screen.simplesorter.wand_settings.mode.match_existing.desc"),
    SMART_CATEGORIES(1, "smart_categories", "Smart Categories", "screen.simplesorter.wand_settings.mode.smart_categories", "screen.simplesorter.wand_settings.mode.smart_categories.desc"),
    COMPACT_STACKS(2, "compact_stacks", "Compact Stacks", "screen.simplesorter.wand_settings.mode.compact_stacks", "screen.simplesorter.wand_settings.mode.compact_stacks.desc"),
    PLAYER_DEPOSIT(3, "player_deposit", "Player Deposit", "screen.simplesorter.wand_settings.mode.player_deposit", "screen.simplesorter.wand_settings.mode.player_deposit.desc"),
    BY_MOD(4, "by_mod", "Sort by Mod", "screen.simplesorter.wand_settings.mode.by_mod", "screen.simplesorter.wand_settings.mode.by_mod.desc");

    private final int index;
    private final String id;
    private final String englishName;
    private final String translationKey;
    private final String descriptionKey;

    SortingMode(int index, String id, String englishName, String translationKey, String descriptionKey) {
        this.index = index;
        this.id = id;
        this.englishName = englishName;
        this.translationKey = translationKey;
        this.descriptionKey = descriptionKey;
    }

    public int index() {
        return index;
    }

    public String id() {
        return id;
    }

    public String englishName() {
        return englishName;
    }

    public String translationKey() {
        return translationKey;
    }

    public String descriptionKey() {
        return descriptionKey;
    }

    public SortingMode next() {
        return switch (this) {
            case MATCH_EXISTING -> SMART_CATEGORIES;
            case SMART_CATEGORIES, COMPACT_STACKS -> BY_MOD;
            case BY_MOD -> PLAYER_DEPOSIT;
            case PLAYER_DEPOSIT -> MATCH_EXISTING;
        };
    }

    public boolean usesGlobalWarehouse(){return this==SMART_CATEGORIES||this==BY_MOD;}
    public static SortingMode byIndex(int index) {
        for (SortingMode mode : values()) {
            if (mode.index == index) {
                return mode == COMPACT_STACKS ? SMART_CATEGORIES : mode;
            }
        }
        return SMART_CATEGORIES;
    }

    public static SortingMode byId(String id) {
        if (id == null || id.isBlank()) {
            return SMART_CATEGORIES;
        }
        for (SortingMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode == COMPACT_STACKS ? SMART_CATEGORIES : mode;
            }
        }
        return SMART_CATEGORIES;
    }
}
