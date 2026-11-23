package com.kirill.simplesorter.settings;

public enum SortingMode {
    MATCH_EXISTING(0, "match_existing", "Match Existing", "screen.simplesorter.wand_settings.mode.match_existing", "screen.simplesorter.wand_settings.mode.match_existing.desc"),
    SMART_CATEGORIES(1, "smart_categories", "Smart Categories", "screen.simplesorter.wand_settings.mode.smart_categories", "screen.simplesorter.wand_settings.mode.smart_categories.desc"),
    COMPACT_STACKS(2, "compact_stacks", "Compact Stacks", "screen.simplesorter.wand_settings.mode.compact_stacks", "screen.simplesorter.wand_settings.mode.compact_stacks.desc"),
    PLAYER_DEPOSIT(3, "player_deposit", "Player Deposit", "screen.simplesorter.wand_settings.mode.player_deposit", "screen.simplesorter.wand_settings.mode.player_deposit.desc");

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
        SortingMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static SortingMode byIndex(int index) {
        for (SortingMode mode : values()) {
            if (mode.index == index) {
                return mode;
            }
        }
        return MATCH_EXISTING;
    }

    public static SortingMode byId(String id) {
        if (id == null || id.isBlank()) {
            return MATCH_EXISTING;
        }
        for (SortingMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return MATCH_EXISTING;
    }
}
