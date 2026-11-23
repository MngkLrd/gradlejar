package com.kirill.simplesorter.sorting;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ChestProfile {
    private final Map<String, Integer> scores;
    private final int totalScore;
    private final int occupiedSlots;

    public ChestProfile(Map<String, Integer> scores, int totalScore, int occupiedSlots) {
        this.scores = Map.copyOf(scores);
        this.totalScore = totalScore;
        this.occupiedSlots = occupiedSlots;
    }

    public static ChestProfile empty() {
        return new ChestProfile(Map.of(), 0, 0);
    }

    public int scoreFor(String categoryId) {
        return scores.getOrDefault(SortCategory.normalizeId(categoryId), 0);
    }

    public int totalScore() {
        return totalScore;
    }

    public int occupiedSlots() {
        return occupiedSlots;
    }

    public boolean isEmpty() {
        return occupiedSlots == 0 || totalScore <= 0;
    }

    public Optional<String> primaryCategoryId() {
        if (isEmpty()) {
            return Optional.empty();
        }

        return scores.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey);
    }

    public double confidence() {
        if (totalScore <= 0) {
            return 0.0D;
        }
        return primaryCategoryId()
                .map(category -> (double) scoreFor(category) / (double) totalScore)
                .orElse(0.0D);
    }

    public Map<String, Integer> copyScores() {
        return new HashMap<>(scores);
    }
}
