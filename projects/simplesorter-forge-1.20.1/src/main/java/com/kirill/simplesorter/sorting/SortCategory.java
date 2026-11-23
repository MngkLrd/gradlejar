package com.kirill.simplesorter.sorting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public final class SortCategory {
    private final String id;
    private final String displayName;
    private final String group;
    private final int priority;
    private final int weightMultiplier;
    private final boolean moveAllowed;
    private final List<ResourceLocation> itemIds;
    private final List<TagKey<Item>> tags;
    private final List<String> idContains;
    private final List<String> namespaces;
    private final EnumSet<ClassHint> classHints;

    private SortCategory(Builder builder) {
        this.id = normalizeId(builder.id);
        this.displayName = builder.displayName == null || builder.displayName.isBlank() ? this.id : builder.displayName;
        this.group = builder.group == null || builder.group.isBlank() ? this.id : normalizeId(builder.group);
        this.priority = builder.priority;
        this.weightMultiplier = Math.max(1, builder.weightMultiplier);
        this.moveAllowed = builder.moveAllowed;
        this.itemIds = List.copyOf(builder.itemIds);
        this.tags = List.copyOf(builder.tags);
        this.idContains = builder.idContains.stream().map(SortCategory::normalizeToken).filter(s -> !s.isBlank()).toList();
        this.namespaces = builder.namespaces.stream().map(SortCategory::normalizeToken).filter(s -> !s.isBlank()).toList();
        this.classHints = builder.classHints.isEmpty() ? EnumSet.noneOf(ClassHint.class) : EnumSet.copyOf(builder.classHints);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String group() {
        return group;
    }

    public int priority() {
        return priority;
    }

    public int weight(int stackCount) {
        return Math.max(1, stackCount) * weightMultiplier;
    }

    public boolean moveAllowed() {
        return moveAllowed;
    }

    public List<ResourceLocation> itemIds() {
        return itemIds;
    }

    public List<TagKey<Item>> tags() {
        return tags;
    }

    public List<String> idContains() {
        return idContains;
    }

    public List<String> namespaces() {
        return namespaces;
    }

    public EnumSet<ClassHint> classHints() {
        return classHints.isEmpty() ? EnumSet.noneOf(ClassHint.class) : EnumSet.copyOf(classHints);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static String normalizeId(String raw) {
        return normalizeToken(raw).replace(' ', '_');
    }

    public static String normalizeToken(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    public static final class Builder {
        private final String id;
        private String displayName;
        private String group;
        private int priority;
        private int weightMultiplier = 1;
        private boolean moveAllowed = true;
        private final List<ResourceLocation> itemIds = new ArrayList<>();
        private final List<TagKey<Item>> tags = new ArrayList<>();
        private final List<String> idContains = new ArrayList<>();
        private final List<String> namespaces = new ArrayList<>();
        private final EnumSet<ClassHint> classHints = EnumSet.noneOf(ClassHint.class);

        private Builder(String id) {
            this.id = id;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder weightMultiplier(int weightMultiplier) {
            this.weightMultiplier = weightMultiplier;
            return this;
        }

        public Builder moveAllowed(boolean moveAllowed) {
            this.moveAllowed = moveAllowed;
            return this;
        }

        public Builder item(String itemId) {
            this.itemIds.add(SortCategoryRegistry.parseLocation(itemId));
            return this;
        }

        public Builder items(String... itemIds) {
            for (String itemId : itemIds) {
                item(itemId);
            }
            return this;
        }

        public Builder tag(String tagId) {
            this.tags.add(SortCategoryRegistry.itemTag(tagId));
            return this;
        }

        public Builder tags(String... tagIds) {
            for (String tagId : tagIds) {
                tag(tagId);
            }
            return this;
        }

        public Builder idContains(String... tokens) {
            this.idContains.addAll(List.of(tokens));
            return this;
        }

        public Builder namespaces(String... namespaces) {
            this.namespaces.addAll(List.of(namespaces));
            return this;
        }

        public Builder classHints(ClassHint... hints) {
            this.classHints.addAll(List.of(hints));
            return this;
        }

        public SortCategory build() {
            return new SortCategory(this);
        }
    }
}
